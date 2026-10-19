-- 修复销售单正式打印模板日期占位符未绑定的问题。
-- 目标：开发库租户 1、sale_out 模块、模板 17/18。
-- 范围：仅补齐日期单元格的 name/code/placeholderCodes，不改模板布局、默认状态或启停状态。

DROP PROCEDURE IF EXISTS sp_fix_sale_out_template_date_binding_20261016;
DELIMITER $$

CREATE PROCEDURE sp_fix_sale_out_template_date_binding_20261016()
BEGIN
    DECLARE v_target_count INT DEFAULT 0;
    DECLARE v_default_count INT DEFAULT 0;
    DECLARE v_unpriced_match_count INT DEFAULT 0;
    DECLARE v_priced_match_count INT DEFAULT 0;
    DECLARE v_unpriced_ordinal INT DEFAULT 0;
    DECLARE v_priced_ordinal INT DEFAULT 0;
    DECLARE v_postcheck_count INT DEFAULT 0;

    SELECT COUNT(*),
           SUM(CASE WHEN defaulted = b'1' THEN 1 ELSE 0 END)
      INTO v_target_count, v_default_count
      FROM erp_print_template
     WHERE tenant_id = 1
       AND module_key = 'sale_out'
       AND deleted = b'0'
       AND status = 0
       AND ((id = 17 AND name = '销售单-无价') OR (id = 18 AND name = '销售单-带价'));
    IF v_target_count <> 2 OR v_default_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '销售单正式模板数量或默认状态不符合预期，停止更新';
    END IF;

    SELECT COUNT(*), MAX(j.ord)
      INTO v_unpriced_match_count, v_unpriced_ordinal
      FROM erp_print_template t
      JOIN JSON_TABLE(t.template_json, '$.cells[*]' COLUMNS (
          ord FOR ORDINALITY,
          cell VARCHAR(16) PATH '$.cell',
          html TEXT PATH '$.html'
      )) j
     WHERE t.tenant_id = 1
       AND t.module_key = 'sale_out'
       AND t.deleted = b'0'
       AND t.status = 0
       AND t.id = 17
       AND t.name = '销售单-无价'
       AND JSON_VALID(t.template_json) = 1
       AND JSON_EXTRACT(t.template_json, '$.version') = 2
       AND j.cell = 'D3'
       AND j.html LIKE '%${日期}%';
    IF v_unpriced_match_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '无价模板日期单元格不是唯一的 D3，停止更新';
    END IF;

    SELECT COUNT(*), MAX(j.ord)
      INTO v_priced_match_count, v_priced_ordinal
      FROM erp_print_template t
      JOIN JSON_TABLE(t.template_json, '$.cells[*]' COLUMNS (
          ord FOR ORDINALITY,
          cell VARCHAR(16) PATH '$.cell',
          html TEXT PATH '$.html'
      )) j
     WHERE t.tenant_id = 1
       AND t.module_key = 'sale_out'
       AND t.deleted = b'0'
       AND t.status = 0
       AND t.id = 18
       AND t.name = '销售单-带价'
       AND JSON_VALID(t.template_json) = 1
       AND JSON_EXTRACT(t.template_json, '$.version') = 2
       AND j.cell = 'E3'
       AND j.html LIKE '%${日期}%';
    IF v_priced_match_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '带价模板日期单元格不是唯一的 E3，停止更新';
    END IF;

    START TRANSACTION;

    UPDATE erp_print_template
       SET template_json = JSON_SET(
               template_json,
               CONCAT('$.cells[', v_unpriced_ordinal - 1, '].name'), '日期',
               CONCAT('$.cells[', v_unpriced_ordinal - 1, '].code'), 'document.sourceCreateTime',
               CONCAT('$.cells[', v_unpriced_ordinal - 1, '].placeholderCodes'),
                   JSON_OBJECT('日期', 'document.sourceCreateTime')
           ),
           updater = '1',
           update_time = NOW()
     WHERE tenant_id = 1
       AND module_key = 'sale_out'
       AND deleted = b'0'
       AND status = 0
       AND id = 17
       AND name = '销售单-无价';

    UPDATE erp_print_template
       SET template_json = JSON_SET(
               template_json,
               CONCAT('$.cells[', v_priced_ordinal - 1, '].name'), '日期',
               CONCAT('$.cells[', v_priced_ordinal - 1, '].code'), 'document.sourceCreateTime',
               CONCAT('$.cells[', v_priced_ordinal - 1, '].placeholderCodes'),
                   JSON_OBJECT('日期', 'document.sourceCreateTime')
           ),
           updater = '1',
           update_time = NOW()
     WHERE tenant_id = 1
       AND module_key = 'sale_out'
       AND deleted = b'0'
       AND status = 0
       AND id = 18
       AND name = '销售单-带价';

    SELECT COUNT(*)
      INTO v_postcheck_count
      FROM erp_print_template t
      JOIN JSON_TABLE(t.template_json, '$.cells[*]' COLUMNS (
          cell VARCHAR(16) PATH '$.cell',
          html TEXT PATH '$.html',
          field_name VARCHAR(128) PATH '$.name' NULL ON EMPTY,
          field_code VARCHAR(128) PATH '$.code' NULL ON EMPTY,
          placeholder_codes JSON PATH '$.placeholderCodes' NULL ON EMPTY
      )) j
     WHERE t.tenant_id = 1
       AND t.module_key = 'sale_out'
       AND t.deleted = b'0'
       AND t.status = 0
       AND ((t.id = 17 AND t.name = '销售单-无价' AND j.cell = 'D3')
         OR (t.id = 18 AND t.name = '销售单-带价' AND j.cell = 'E3'))
       AND j.html LIKE '%${日期}%'
       AND j.field_name = '日期'
       AND j.field_code = 'document.sourceCreateTime'
       AND JSON_UNQUOTE(JSON_EXTRACT(j.placeholder_codes, '$."日期"')) = 'document.sourceCreateTime';
    IF v_postcheck_count <> 2 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '销售单日期绑定更新后校验失败，已回滚';
    END IF;

    COMMIT;
END$$

DELIMITER ;
CALL sp_fix_sale_out_template_date_binding_20261016();
DROP PROCEDURE sp_fix_sale_out_template_date_binding_20261016;

SELECT t.id,
       t.name,
       t.defaulted,
       j.cell,
       j.field_name,
       j.field_code,
       JSON_UNQUOTE(JSON_EXTRACT(j.placeholder_codes, '$."日期"')) AS date_placeholder_code,
       t.update_time
  FROM erp_print_template t
  JOIN JSON_TABLE(t.template_json, '$.cells[*]' COLUMNS (
      cell VARCHAR(16) PATH '$.cell',
      html TEXT PATH '$.html',
      field_name VARCHAR(128) PATH '$.name' NULL ON EMPTY,
      field_code VARCHAR(128) PATH '$.code' NULL ON EMPTY,
      placeholder_codes JSON PATH '$.placeholderCodes' NULL ON EMPTY
  )) j
 WHERE t.tenant_id = 1
   AND t.module_key = 'sale_out'
   AND t.deleted = b'0'
   AND t.status = 0
   AND t.id IN (17, 18)
   AND j.html LIKE '%${日期}%'
 ORDER BY t.id;
