-- Customer assistant compatibility migration. Run in development/test first.
-- Existing rows remain ADMIN-owned; no business, role or permission data is deleted or overwritten.
DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_erp_customer_assistant_v2$$
CREATE PROCEDURE migrate_erp_customer_assistant_v2()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_assistant_conversation' AND column_name='owner_type') THEN
    ALTER TABLE erp_assistant_conversation ADD COLUMN owner_type varchar(16) NOT NULL DEFAULT 'ADMIN' AFTER user_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_assistant_conversation' AND column_name='customer_id') THEN
    ALTER TABLE erp_assistant_conversation ADD COLUMN customer_id bigint NULL AFTER owner_type;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_assistant_conversation' AND column_name='entry') THEN
    ALTER TABLE erp_assistant_conversation ADD COLUMN entry varchar(32) NULL AFTER customer_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='erp_assistant_conversation' AND index_name='idx_subject_owner') THEN
    ALTER TABLE erp_assistant_conversation ADD KEY idx_subject_owner (tenant_id,owner_type,user_id,customer_id,deleted,update_time);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_assistant_message' AND column_name='owner_type') THEN
    ALTER TABLE erp_assistant_message ADD COLUMN owner_type varchar(16) NOT NULL DEFAULT 'ADMIN' AFTER user_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_assistant_message' AND column_name='customer_id') THEN
    ALTER TABLE erp_assistant_message ADD COLUMN customer_id bigint NULL AFTER owner_type;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_assistant_message' AND column_name='entry') THEN
    ALTER TABLE erp_assistant_message ADD COLUMN entry varchar(32) NULL AFTER customer_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='erp_assistant_message' AND index_name='idx_subject_conversation') THEN
    ALTER TABLE erp_assistant_message ADD KEY idx_subject_conversation (tenant_id,owner_type,user_id,customer_id,conversation_id,deleted,create_time);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_assistant_audit' AND column_name='owner_type') THEN
    ALTER TABLE erp_assistant_audit ADD COLUMN owner_type varchar(16) NOT NULL DEFAULT 'ADMIN' AFTER user_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_assistant_audit' AND column_name='customer_id') THEN
    ALTER TABLE erp_assistant_audit ADD COLUMN customer_id bigint NULL AFTER owner_type;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_assistant_audit' AND column_name='entry') THEN
    ALTER TABLE erp_assistant_audit ADD COLUMN entry varchar(32) NULL AFTER customer_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='erp_assistant_audit' AND index_name='idx_audit_subject') THEN
    ALTER TABLE erp_assistant_audit ADD KEY idx_audit_subject (tenant_id,owner_type,user_id,customer_id,create_time);
  END IF;
END$$
CALL migrate_erp_customer_assistant_v2()$$
DROP PROCEDURE migrate_erp_customer_assistant_v2$$
DELIMITER ;

-- Post-checks (expected: each table returns 3 columns and one new compound index).
SELECT table_name,COUNT(*) AS subject_column_count
FROM information_schema.columns
WHERE table_schema=DATABASE()
  AND table_name IN ('erp_assistant_conversation','erp_assistant_message','erp_assistant_audit')
  AND column_name IN ('owner_type','customer_id','entry')
GROUP BY table_name;
SELECT table_name,index_name,COUNT(*) AS index_column_count
FROM information_schema.statistics
WHERE table_schema=DATABASE()
  AND index_name IN ('idx_subject_owner','idx_subject_conversation','idx_audit_subject')
GROUP BY table_name,index_name;
SELECT owner_type,COUNT(*) AS row_count FROM erp_assistant_conversation GROUP BY owner_type;
