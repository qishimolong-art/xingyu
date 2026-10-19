-- 修复销售单正式打印模板的同名单字段绑定：
-- 1. I7 明细备注保持 items.remark，避免设计器保存时误识别为 document.remark；
-- 2. 主表“单位”显示客户 customer.name，使用“客户”作为占位符名称。
-- 目标：开发库租户 1、sale_out 模块、模板 17/18。

DROP PROCEDURE IF EXISTS sp_fix_sale_out_template_field_binding_20261016;
DELIMITER $$

CREATE PROCEDURE sp_fix_sale_out_template_field_binding_20261016()
BEGIN
    DECLARE v_target_count INT DEFAULT 0;
    DECLARE v_shape_count INT DEFAULT 0;
    DECLARE v_valid_count INT DEFAULT 0;

    SELECT COUNT(*) INTO v_target_count
      FROM erp_print_template
     WHERE tenant_id = 1
       AND module_key = 'sale_out'
       AND deleted = b'0'
       AND status = 0
       AND ((id = 17 AND name = '销售单-无价') OR (id = 18 AND name = '销售单-带价'));
    IF v_target_count <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '销售单正式模板 17/18 不完整，停止更新';
    END IF;

    SELECT COUNT(*) INTO v_shape_count
      FROM erp_print_template
     WHERE tenant_id = 1
       AND module_key = 'sale_out'
       AND deleted = b'0'
       AND status = 0
       AND id IN (17, 18)
       AND JSON_VALID(template_json) = 1
       AND JSON_EXTRACT(template_json, '$.version') = 2
       AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[5].cell')) = 'A3'
       AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[5].code')) = 'customer.name'
       AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.detailTable.bodyRows.start')) = '7'
       AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.detailTable.bodyRows.end')) = '7'
       AND (
           (id = 17
            AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[29].cell')) = 'I7'
            AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[29].code')) = 'items.remark')
           OR
           (id = 18
            AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[31].cell')) = 'I7'
            AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[31].code')) = 'items.remark')
       );
    IF v_shape_count <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '销售单模板结构与预期不一致，停止更新';
    END IF;

    START TRANSACTION;

    UPDATE erp_print_template
       SET template_json = JSON_SET(
               template_json,
               '$.cells[5].html', '<strong>单位：</strong><span class="token">${客户}</span>',
               '$.cells[5].text', '单位：${客户}',
               '$.cells[5].name', '客户',
               '$.cells[5].code', 'customer.name',
               '$.cells[5].placeholderCodes', JSON_OBJECT('客户', 'customer.name'),
               '$.cells[29].html', '<span class="token">${备注}</span>',
               '$.cells[29].text', '${备注}',
               '$.cells[29].name', '备注',
               '$.cells[29].code', 'items.remark',
               '$.cells[29].placeholderCodes', JSON_OBJECT('备注', 'items.remark')
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
               '$.cells[5].html', '<strong>单位：</strong><span class="token">${客户}</span>',
               '$.cells[5].text', '单位：${客户}',
               '$.cells[5].name', '客户',
               '$.cells[5].code', 'customer.name',
               '$.cells[5].placeholderCodes', JSON_OBJECT('客户', 'customer.name'),
               '$.cells[31].html', '<span class="token">${备注}</span>',
               '$.cells[31].text', '${备注}',
               '$.cells[31].name', '备注',
               '$.cells[31].code', 'items.remark',
               '$.cells[31].placeholderCodes', JSON_OBJECT('备注', 'items.remark')
           ),
           updater = '1',
           update_time = NOW()
     WHERE tenant_id = 1
       AND module_key = 'sale_out'
       AND deleted = b'0'
       AND status = 0
       AND id = 18
       AND name = '销售单-带价';
    SELECT COUNT(*) INTO v_valid_count
      FROM erp_print_template
     WHERE tenant_id = 1
       AND module_key = 'sale_out'
       AND deleted = b'0'
       AND status = 0
       AND id IN (17, 18)
       AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[5].html')) = '<strong>单位：</strong><span class="token">${客户}</span>'
       AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[5].name')) = '客户'
       AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[5].code')) = 'customer.name'
       AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[5].placeholderCodes."客户"')) = 'customer.name'
       AND (
           (id = 17
            AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[29].cell')) = 'I7'
            AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[29].code')) = 'items.remark'
            AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[29].placeholderCodes."备注"')) = 'items.remark')
           OR
           (id = 18
            AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[31].cell')) = 'I7'
            AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[31].code')) = 'items.remark'
            AND JSON_UNQUOTE(JSON_EXTRACT(template_json, '$.cells[31].placeholderCodes."备注"')) = 'items.remark')
       );
    IF v_valid_count <> 2 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '销售单模板更新后校验失败，已回滚';
    END IF;

    COMMIT;
END$$

DELIMITER ;
CALL sp_fix_sale_out_template_field_binding_20261016();
DROP PROCEDURE sp_fix_sale_out_template_field_binding_20261016;

SELECT t.id,
       t.name,
       JSON_UNQUOTE(JSON_EXTRACT(t.template_json, '$.cells[5].html')) AS main_customer_html,
       JSON_UNQUOTE(JSON_EXTRACT(t.template_json, '$.cells[5].code')) AS main_customer_code,
       CASE WHEN t.id = 17
            THEN JSON_UNQUOTE(JSON_EXTRACT(t.template_json, '$.cells[29].code'))
            ELSE JSON_UNQUOTE(JSON_EXTRACT(t.template_json, '$.cells[31].code'))
       END AS i7_code,
       t.update_time
  FROM erp_print_template t
 WHERE t.tenant_id = 1
   AND t.module_key = 'sale_out'
   AND t.deleted = b'0'
   AND t.status = 0
   AND t.id IN (17, 18)
 ORDER BY t.id;
