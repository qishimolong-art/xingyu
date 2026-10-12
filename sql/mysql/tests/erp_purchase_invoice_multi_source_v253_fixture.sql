-- Disposable fixture for erp_purchase_invoice_multi_source_v253.sql.
-- Run only against an isolated local MySQL instance. Never run on a shared database.

DROP DATABASE IF EXISTS erp_invoice_v253_test;
CREATE DATABASE erp_invoice_v253_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE erp_invoice_v253_test;

CREATE TABLE erp_purchase_invoice (
  id BIGINT NOT NULL PRIMARY KEY,
  status INT NOT NULL,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE erp_purchase_invoice_item (
  id BIGINT NOT NULL PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  source_in_id BIGINT NULL,
  source_in_no VARCHAR(64) NULL,
  source_in_item_id BIGINT NULL,
  total_price DECIMAL(24,6) NULL,
  creator VARCHAR(64) NULL,
  create_time DATETIME NULL,
  updater VARCHAR(64) NULL,
  update_time DATETIME NULL,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE erp_purchase_in_items (
  id BIGINT NOT NULL PRIMARY KEY,
  in_id BIGINT NOT NULL,
  `count` DECIMAL(24,6) NOT NULL,
  original_product_price DECIMAL(24,6) NULL,
  product_price DECIMAL(24,6) NOT NULL,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE erp_purchase_in (
  id BIGINT NOT NULL PRIMARY KEY,
  no VARCHAR(64) NOT NULL,
  supplier_id BIGINT NOT NULL,
  dept_id BIGINT NULL,
  in_time DATETIME NULL,
  status INT NOT NULL,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE erp_purchase_price_adjust (
  id BIGINT NOT NULL PRIMARY KEY,
  no VARCHAR(64) NOT NULL,
  supplier_id BIGINT NOT NULL,
  dept_id BIGINT NULL,
  adjust_time DATETIME NULL,
  status INT NOT NULL,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE erp_purchase_price_adjust_item (
  id BIGINT NOT NULL PRIMARY KEY,
  adjust_id BIGINT NOT NULL,
  in_item_id BIGINT NULL,
  `count` DECIMAL(24,6) NOT NULL,
  adjust_price DECIMAL(24,6) NOT NULL,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE erp_purchase_return_items (
  id BIGINT NOT NULL PRIMARY KEY,
  return_id BIGINT NOT NULL,
  `count` DECIMAL(24,6) NOT NULL,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE erp_purchase_return (
  id BIGINT NOT NULL PRIMARY KEY,
  no VARCHAR(64) NOT NULL,
  supplier_id BIGINT NOT NULL,
  dept_id BIGINT NULL,
  return_time DATETIME NULL,
  status INT NOT NULL,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

INSERT INTO erp_purchase_invoice (id,status,deleted,tenant_id) VALUES
  (100,20,b'0',1),
  (101,10,b'0',1),
  (102,20,b'0',1);

INSERT INTO erp_purchase_in_items
  (id,in_id,`count`,original_product_price,product_price,deleted,tenant_id) VALUES
  (1,10,10,NULL,100,b'0',1),
  (2,11,5,NULL,20,b'0',1);

INSERT INTO erp_purchase_in
  (id,no,supplier_id,dept_id,in_time,status,deleted,tenant_id) VALUES
  (10,'CGRK001',100,1,'2026-10-01 09:00:00',20,b'0',1),
  (11,'CGRK002',100,1,'2026-10-02 09:00:00',20,b'0',1);

-- Three historical invoice rows, including rows from two approved invoices, point to the same inbound item.
-- Migration must still create only one full-close allocation for that source item.
INSERT INTO erp_purchase_invoice_item
  (id,invoice_id,source_in_id,source_in_no,source_in_item_id,total_price,
   creator,create_time,updater,update_time,deleted,tenant_id) VALUES
  (1001,100,10,'CGRK001',1,600,'1','2026-10-01 10:00:00','1','2026-10-01 10:00:00',b'0',1),
  (1002,100,10,'CGRK001',1,400,'1','2026-10-01 10:00:00','1','2026-10-01 10:00:00',b'0',1),
  (1003,101,11,'CGRK002',2,100,'1','2026-10-02 10:00:00','1','2026-10-02 10:00:00',b'0',1),
  (1004,102,10,'CGRK001',1,1000,'1','2026-10-06 10:00:00','1','2026-10-06 10:00:00',b'0',1);

INSERT INTO erp_purchase_price_adjust
  (id,no,supplier_id,dept_id,adjust_time,status,deleted,tenant_id) VALUES
  (20,'CGTJ001',100,1,'2026-10-03 09:00:00',20,b'0',1),
  (21,'CGTJ002',100,1,'2026-10-04 09:00:00',20,b'0',1);

INSERT INTO erp_purchase_price_adjust_item
  (id,adjust_id,in_item_id,`count`,adjust_price,deleted,tenant_id) VALUES
  (201,20,1,10,100,b'0',1),
  (202,21,2,5,-10,b'0',1);

INSERT INTO erp_purchase_return
  (id,no,supplier_id,dept_id,return_time,status,deleted,tenant_id) VALUES
  (30,'CGTH001',100,1,'2026-10-05 09:00:00',20,b'0',1);

INSERT INTO erp_purchase_return_items (id,return_id,`count`,deleted,tenant_id) VALUES
  (301,30,2,b'0',1);
