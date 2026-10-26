-- 销售单送货快递单兼容主图安全回填（v278）。
-- MySQL 5.7 / 8.0 兼容，可重复执行。
-- 规则：只填空值；最新送货提交优先，同批次 sort、附件 id 升序；绝不覆盖已有快递单。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS backfill_erp_sale_out_delivery_express_v278;

DELIMITER //
CREATE PROCEDURE backfill_erp_sale_out_delivery_express_v278()
BEGIN
    DECLARE expected_count BIGINT DEFAULT 0;
    DECLARE affected_count BIGINT DEFAULT 0;
    DECLARE remaining_count BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF DATABASE() IS NULL OR DATABASE() = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v278: no target database selected';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'erp_sale_out'
          AND column_name = 'express_file_url'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'erp_sale_pick_delivery_submit'
          AND column_name = 'sale_out_id'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'erp_sale_pick_delivery_submit_file'
          AND column_name = 'file_url'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v278: required table or column is missing';
    END IF;

    START TRANSACTION;

    SELECT COUNT(DISTINCT sale_out.id)
      INTO expected_count
      FROM erp_sale_out sale_out
     WHERE sale_out.deleted = b'0'
       AND (sale_out.express_file_url IS NULL OR TRIM(sale_out.express_file_url) = '')
       AND EXISTS (
           SELECT 1
             FROM erp_sale_pick_delivery_submit submit
             INNER JOIN erp_sale_pick_delivery_submit_file submit_file
                     ON submit_file.submit_id = submit.id
                    AND submit_file.tenant_id = submit.tenant_id
                    AND submit_file.deleted = b'0'
                    AND submit_file.file_url IS NOT NULL
                    AND TRIM(submit_file.file_url) <> ''
            WHERE submit.sale_out_id = sale_out.id
              AND submit.tenant_id = sale_out.tenant_id
              AND submit.deleted = b'0'
              AND submit.type = 20
       );

    UPDATE erp_sale_out sale_out
       SET sale_out.express_file_url = (
           SELECT submit_file.file_url
             FROM erp_sale_pick_delivery_submit submit
             INNER JOIN erp_sale_pick_delivery_submit_file submit_file
                     ON submit_file.submit_id = submit.id
                    AND submit_file.tenant_id = submit.tenant_id
                    AND submit_file.deleted = b'0'
                    AND submit_file.file_url IS NOT NULL
                    AND TRIM(submit_file.file_url) <> ''
            WHERE submit.sale_out_id = sale_out.id
              AND submit.tenant_id = sale_out.tenant_id
              AND submit.deleted = b'0'
              AND submit.type = 20
            ORDER BY submit.submit_time DESC, submit.id DESC,
                     COALESCE(submit_file.sort, 2147483647) ASC, submit_file.id ASC
            LIMIT 1
       )
     WHERE sale_out.deleted = b'0'
       AND (sale_out.express_file_url IS NULL OR TRIM(sale_out.express_file_url) = '')
       AND EXISTS (
           SELECT 1
             FROM erp_sale_pick_delivery_submit submit
             INNER JOIN erp_sale_pick_delivery_submit_file submit_file
                     ON submit_file.submit_id = submit.id
                    AND submit_file.tenant_id = submit.tenant_id
                    AND submit_file.deleted = b'0'
                    AND submit_file.file_url IS NOT NULL
                    AND TRIM(submit_file.file_url) <> ''
            WHERE submit.sale_out_id = sale_out.id
              AND submit.tenant_id = sale_out.tenant_id
              AND submit.deleted = b'0'
              AND submit.type = 20
       );

    SET affected_count = ROW_COUNT();

    SELECT COUNT(DISTINCT sale_out.id)
      INTO remaining_count
      FROM erp_sale_out sale_out
     WHERE sale_out.deleted = b'0'
       AND (sale_out.express_file_url IS NULL OR TRIM(sale_out.express_file_url) = '')
       AND EXISTS (
           SELECT 1
             FROM erp_sale_pick_delivery_submit submit
             INNER JOIN erp_sale_pick_delivery_submit_file submit_file
                     ON submit_file.submit_id = submit.id
                    AND submit_file.tenant_id = submit.tenant_id
                    AND submit_file.deleted = b'0'
                    AND submit_file.file_url IS NOT NULL
                    AND TRIM(submit_file.file_url) <> ''
            WHERE submit.sale_out_id = sale_out.id
              AND submit.tenant_id = sale_out.tenant_id
              AND submit.deleted = b'0'
              AND submit.type = 20
       );

    IF affected_count > expected_count OR remaining_count <> 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v278: post-check failed, transaction rolled back';
    END IF;

    COMMIT;
    SELECT expected_count AS expected_rows,
           affected_count AS affected_rows,
           remaining_count AS remaining_rows;
END //
DELIMITER ;

CALL backfill_erp_sale_out_delivery_express_v278();
DROP PROCEDURE IF EXISTS backfill_erp_sale_out_delivery_express_v278;

