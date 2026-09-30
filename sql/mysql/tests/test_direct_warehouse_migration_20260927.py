"""Run only against the disposable local MySQL instance on 127.0.0.1:33307.

No project database credentials are read. Each case creates a uniquely named test
schema; it never drops a schema or connects to the production server.
"""
from pathlib import Path
from decimal import Decimal
import json
import unittest
import uuid

import pymysql


SQL_DIR = Path(__file__).resolve().parents[1]
IDS = [7, 21, 22, 23, 25, 27, 28]
REMARK = "系统自动创建：销售手推车跨部门调拨专用直发仓"


class DirectWarehouseMigrationTest(unittest.TestCase):
    def setUp(self):
        self.conn = pymysql.connect(host="127.0.0.1", port=33307, user="root",
                                    charset="utf8mb4", autocommit=True)
        self.addCleanup(self.conn.close)
        self.q = self.conn.cursor()
        self.q.execute("SELECT @@datadir")
        self.assertEqual(Path(self.q.fetchone()[0]).name, "direct-warehouse-mysql")
        self.schema = "dw_test_" + uuid.uuid4().hex
        self.q.execute(f"CREATE DATABASE `{self.schema}` CHARACTER SET utf8mb4")
        self.q.execute(f"USE `{self.schema}`")
        self.q.execute("""CREATE TABLE erp_warehouse (
            id bigint PRIMARY KEY, tenant_id bigint NOT NULL, dept_id bigint,
            name varchar(20) NOT NULL, remark varchar(100), status int NOT NULL DEFAULT 0,
            deleted bit(1) NOT NULL DEFAULT b'0', warehouse_code varchar(64),
            update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        ) ENGINE=InnoDB""")
        self.q.execute("""CREATE TABLE system_dept (
            id bigint PRIMARY KEY, tenant_id bigint NOT NULL, name varchar(30) NOT NULL,
            deleted bit(1) NOT NULL DEFAULT b'0') ENGINE=InnoDB""")
        for index, wid in enumerate(IDS):
            dept_name = "甘孜分公司" if index == 0 else "部门" + str(wid)
            if index == 1:
                dept_name = "长" * 30
            self.q.execute("INSERT INTO system_dept (id,tenant_id,name) VALUES (%s,1,%s)", (wid, dept_name))
            self.q.execute("""INSERT INTO erp_warehouse
                (id,tenant_id,dept_id,name,remark,status,warehouse_code,update_time)
                VALUES (%s,1,%s,%s,%s,%s,%s,'2026-09-01 12:00:00')""",
                (wid, wid, "自定义名称" if index == 2 else "直发仓", REMARK,
                 1 if index == 3 else 0, "WH" + str(wid)))
        self.q.execute("""INSERT INTO erp_warehouse (id,tenant_id,dept_id,name,deleted) VALUES
            (90,1,7,'普通直发仓',b'0'),(91,2,7,'直发仓',b'0'),(92,1,NULL,'直发仓',b'1')""")
        self.q.execute("CREATE TABLE erp_stock (id bigint PRIMARY KEY,warehouse_id bigint,count decimal(18,6))")
        self.q.execute("INSERT INTO erp_stock VALUES (1,7,123.456)")
        self.q.execute("CREATE TABLE erp_stock_move_item (id bigint PRIMARY KEY,to_warehouse_id bigint)")
        self.q.execute("INSERT INTO erp_stock_move_item VALUES (1,7)")
        self.q.execute("SET @dw_schema=%s,@dw_tenant_id=1,@dw_expected_count=7,@dw_ids=%s",
                       (self.schema, json.dumps(IDS)))

    def script(self, suffix):
        delimiter = ";"
        lines = []
        results = []
        for line in (SQL_DIR / f"erp_direct_warehouse_20260927_{suffix}.sql").read_text(encoding="utf-8").splitlines():
            if not line.strip() or line.lstrip().startswith("--"):
                continue
            if line.startswith("DELIMITER "):
                delimiter = line.split()[1]
                continue
            lines.append(line)
            if line.rstrip().endswith(delimiter):
                sql = "\n".join(lines).rstrip()[:-len(delimiter)]
                self.q.execute(sql)
                if self.q.description:
                    results.append(self.q.fetchall())
                while self.q.nextset():
                    if self.q.description:
                        results.append(self.q.fetchall())
                lines = []
        self.assertFalse(lines)
        return results

    def warehouse_snapshot(self):
        self.q.execute("SELECT * FROM erp_warehouse ORDER BY id")
        return self.q.fetchall()

    def test_migrate_repeat_and_rollback_preserve_identity_and_relations(self):
        self.script("schema")
        self.script("schema")
        before = self.warehouse_snapshot()
        self.assertEqual([((7, 7),)], self.script("data"))
        self.q.execute("SELECT name,direct_warehouse+0 FROM erp_warehouse WHERE id=7")
        self.assertEqual(("甘孜分公司直发仓", 1), self.q.fetchone())
        self.q.execute("SELECT CHAR_LENGTH(name) FROM erp_warehouse WHERE id=21")
        self.assertEqual((33,), self.q.fetchone())
        self.q.execute("SELECT name,direct_warehouse+0 FROM erp_warehouse WHERE id=22")
        self.assertEqual(("自定义名称", 1), self.q.fetchone())
        self.q.execute("SELECT SUM(direct_warehouse+0) FROM erp_warehouse WHERE id IN (90,91,92)")
        self.assertEqual(0, self.q.fetchone()[0])
        after = self.warehouse_snapshot()
        self.assertEqual([((0, 7),)], self.script("data"))
        self.assertEqual(after, self.warehouse_snapshot())
        self.assertEqual([((7,),)], self.script("rollback"))
        self.assertEqual(before, self.warehouse_snapshot())
        self.assertEqual([((0,),)], self.script("rollback"))
        self.q.execute("SELECT warehouse_id,count FROM erp_stock WHERE id=1")
        self.assertEqual((7, Decimal('123.456')), self.q.fetchone())
        self.q.execute("SELECT to_warehouse_id FROM erp_stock_move_item WHERE id=1")
        self.assertEqual((7,), self.q.fetchone())

    def assert_migration_rejected_without_changes(self, message):
        before = self.warehouse_snapshot()
        with self.assertRaisesRegex(pymysql.MySQLError, message):
            self.script("data")
        self.assertEqual(before, self.warehouse_snapshot())
        self.q.execute("SELECT COUNT(*) FROM erp_direct_warehouse_backup_20260927")
        self.assertEqual((0,), self.q.fetchone())

    def test_missing_department_aborts(self):
        self.script("schema")
        self.q.execute("UPDATE erp_warehouse SET dept_id=NULL WHERE id=7")
        self.assert_migration_rejected_without_changes("Invalid department")

    def test_wrong_tenant_department_aborts(self):
        self.script("schema")
        self.q.execute("UPDATE system_dept SET tenant_id=2 WHERE id=7")
        self.assert_migration_rejected_without_changes("Invalid department")

    def test_duplicate_enabled_warehouse_aborts(self):
        self.script("schema")
        self.q.execute("UPDATE erp_warehouse SET dept_id=7 WHERE id=21")
        self.assert_migration_rejected_without_changes("Multiple enabled")

    def test_unreviewed_candidate_aborts(self):
        self.script("schema")
        self.q.execute("UPDATE erp_warehouse SET name='直发仓' WHERE id=90")
        self.assert_migration_rejected_without_changes("Unreviewed historical")

    def test_wrong_expected_count_aborts(self):
        self.script("schema")
        self.q.execute("SET @dw_expected_count=8")
        self.assert_migration_rejected_without_changes("ID count mismatch")

    def test_unexpected_postcheck_rolls_back_update_and_backup(self):
        self.script("schema")
        self.q.execute("""CREATE TRIGGER simulate_unexpected_update BEFORE UPDATE ON erp_warehouse
            FOR EACH ROW SET NEW.direct_warehouse=b'0'""")
        self.assert_migration_rejected_without_changes("Postcheck failed")

    def test_changed_warehouse_blocks_rollback(self):
        self.script("schema")
        self.script("data")
        self.q.execute("UPDATE erp_warehouse SET name='用户后续改名' WHERE id=7")
        before = self.warehouse_snapshot()
        with self.assertRaisesRegex(pymysql.MySQLError, "changed since migration"):
            self.script("rollback")
        self.assertEqual(before, self.warehouse_snapshot())

    def test_new_direct_warehouse_blocks_rollback(self):
        self.script("schema")
        self.script("data")
        self.q.execute("UPDATE erp_warehouse SET direct_warehouse=b'1' WHERE id=90")
        before = self.warehouse_snapshot()
        with self.assertRaisesRegex(pymysql.MySQLError, "New direct warehouses"):
            self.script("rollback")
        self.assertEqual(before, self.warehouse_snapshot())


if __name__ == "__main__":
    unittest.main(verbosity=2)
