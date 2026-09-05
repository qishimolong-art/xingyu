-- =====================================================
-- 调拨出库单：解锁手推车额外总公司部门配置
-- =====================================================
-- 用途：
--   跨部门销售手推车调拨出库单，除单据部门的上级/祖先部门外，
--   允许配置中的部门及其下级部门执行“解锁手推车”。
--
-- 配置说明：
--   config_key = erp.stock.transferOut.unlockCartHeadDeptIds
--   value      = 逗号分隔的部门 ID，例如：123,456
--   默认空值表示不启用额外部门，保持旧逻辑。
-- =====================================================

INSERT INTO `infra_config`
(`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `create_time`,
 `updater`, `update_time`, `deleted`)
SELECT 'erp', 2, '调拨出库解锁手推车额外总公司部门',
       'erp.stock.transferOut.unlockCartHeadDeptIds', '', b'1',
       '逗号分隔部门 ID；配置部门及其下级部门可解锁跨部门销售手推车调拨出库单',
       '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_config`
   WHERE `config_key` = 'erp.stock.transferOut.unlockCartHeadDeptIds'
     AND `deleted` = b'0'
);
