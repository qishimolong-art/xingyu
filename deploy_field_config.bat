@echo off
chcp 65001 >nul
echo ============================================================
echo 字段配置功能 - 一键部署脚本
echo ============================================================
echo.

:: 设置数据库连接信息（请根据实际情况修改）
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=ruoyi-vue-pro
set DB_USER=root
set DB_PASS=123456

echo [1/5] 检查MySQL连接...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -e "SELECT 1;" >nul 2>&1
if errorlevel 1 (
    echo ❌ MySQL连接失败，请检查数据库连接信息
    echo.
    echo 请修改本脚本中的数据库连接信息：
    echo   DB_HOST=%DB_HOST%
    echo   DB_PORT=%DB_PORT%
    echo   DB_NAME=%DB_NAME%
    echo   DB_USER=%DB_USER%
    echo   DB_PASS=%DB_PASS%
    echo.
    pause
    exit /b 1
)
echo ✅ MySQL连接成功
echo.

echo [2/5] 执行字段配置表创建和初始化...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% < sql\mysql\erp_field_config_v7.sql
if errorlevel 1 (
    echo ❌ 字段配置表创建失败
    pause
    exit /b 1
)
echo ✅ 字段配置表创建成功（143条记录）
echo.

echo [3/5] 执行菜单创建...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% < sql\mysql\erp_menu_v7_fixed.sql
if errorlevel 1 (
    echo ❌ 菜单创建失败
    pause
    exit /b 1
)
echo ✅ 菜单创建成功（4条菜单+权限）
echo.

echo [4/5] 验证部署结果...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% < sql\mysql\verify_field_config.sql
echo.

echo [5/5] 后端编译验证...
call mvn compile -pl yudao-module-erp -am -DskipTests -q
if errorlevel 1 (
    echo ❌ 后端编译失败
    pause
    exit /b 1
)
echo ✅ 后端编译成功
echo.

echo ============================================================
echo 部署完成！
echo ============================================================
echo.
echo 接下来请执行以下步骤：
echo.
echo 1. 登录管理后台
echo 2. 系统管理 → 菜单管理 → 点击"刷新缓存"按钮
echo 3. 退出重新登录
echo 4. 导航到：ERP 系统 → 系统配置 → 字段配置
echo.
echo 详细使用说明请查看：docs\字段配置功能-部署和使用指南.md
echo.
pause
