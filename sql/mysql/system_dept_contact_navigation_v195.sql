-- system_dept contact and navigation fields (v195).
-- Safe to rerun. This script only changes columns on system_dept.

DROP PROCEDURE IF EXISTS add_system_dept_contact_navigation_v195;

DELIMITER //
CREATE PROCEDURE add_system_dept_contact_navigation_v195()
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'system_dept'
           AND COLUMN_NAME = 'phone'
    ) THEN
        ALTER TABLE `system_dept`
            MODIFY COLUMN `phone` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'system_dept'
           AND COLUMN_NAME = 'address'
    ) THEN
        ALTER TABLE `system_dept`
            ADD COLUMN `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '详细地址'
            AFTER `phone`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'system_dept'
           AND COLUMN_NAME = 'longitude'
    ) THEN
        ALTER TABLE `system_dept`
            ADD COLUMN `longitude` decimal(10,6) NULL DEFAULT NULL COMMENT '经度'
            AFTER `address`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'system_dept'
           AND COLUMN_NAME = 'latitude'
    ) THEN
        ALTER TABLE `system_dept`
            ADD COLUMN `latitude` decimal(10,6) NULL DEFAULT NULL COMMENT '纬度'
            AFTER `longitude`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'system_dept'
           AND COLUMN_NAME = 'map_name'
    ) THEN
        ALTER TABLE `system_dept`
            ADD COLUMN `map_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地图显示名称'
            AFTER `latitude`;
    END IF;
END//
DELIMITER ;

CALL add_system_dept_contact_navigation_v195();

DROP PROCEDURE IF EXISTS add_system_dept_contact_navigation_v195;
