-- ==============================================================
-- ERP 字段配置：新增显示/隐藏配置
-- 部署方式：mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_field_config_visible_v48.sql
-- ==============================================================

ALTER TABLE `erp_field_config`
  ADD COLUMN `visible` BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否显示'
  AFTER `required`;
