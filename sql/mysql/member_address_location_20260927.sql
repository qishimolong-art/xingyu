-- 收货位置 GCJ-02。执行前确认目标环境并检查 member_address 结构。
-- 仅新增可空列，不修改历史数据。MySQL DDL 自动提交，发生错误立即停止并核对已添加列。
-- 兼容旧应用；回退时保留新增列和已采集位置。可重复执行。

SET @address_location_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'member_address' AND column_name = 'longitude'), 'SELECT ''longitude exists'' AS result', 'ALTER TABLE member_address ADD COLUMN longitude DECIMAL(10,6) NULL COMMENT ''收货经度 GCJ-02''');
PREPARE address_location_stmt FROM @address_location_ddl;
EXECUTE address_location_stmt;
DEALLOCATE PREPARE address_location_stmt;

SET @address_location_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'member_address' AND column_name = 'latitude'), 'SELECT ''latitude exists'' AS result', 'ALTER TABLE member_address ADD COLUMN latitude DECIMAL(10,6) NULL COMMENT ''收货纬度 GCJ-02''');
PREPARE address_location_stmt FROM @address_location_ddl;
EXECUTE address_location_stmt;
DEALLOCATE PREPARE address_location_stmt;

SET @address_location_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'member_address' AND column_name = 'map_name'), 'SELECT ''map_name exists'' AS result', 'ALTER TABLE member_address ADD COLUMN map_name VARCHAR(200) NULL COMMENT ''地图地点名称''');
PREPARE address_location_stmt FROM @address_location_ddl;
EXECUTE address_location_stmt;
DEALLOCATE PREPARE address_location_stmt;

SET @address_location_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'member_address' AND column_name = 'map_address'), 'SELECT ''map_address exists'' AS result', 'ALTER TABLE member_address ADD COLUMN map_address VARCHAR(500) NULL COMMENT ''地图地点地址''');
PREPARE address_location_stmt FROM @address_location_ddl;
EXECUTE address_location_stmt;
DEALLOCATE PREPARE address_location_stmt;

SELECT column_name, column_type, is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'member_address' AND column_name IN ('longitude', 'latitude', 'map_name', 'map_address');
