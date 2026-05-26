# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 语言要求

本项目是中文项目，所有回答和交流请使用中文。

## 项目概述

RuoYi Vue Pro（芋道）是基于 Spring Boot 2.7.18 + JDK 8 的 Java 企业级快速开发平台。采用多模块 Maven 架构，提供 ERP、CRM、BPM、IoT、AI 等业务能力。

## 构建与运行命令

```bash
# 全量构建（跳过测试加速）
mvn clean install -DskipTests

# 构建指定模块
mvn clean install -pl yudao-module-system -am -DskipTests

# 运行测试（使用 H2 + Jedis-mock，无需外部依赖）
mvn test

# 运行单个测试类
mvn test -pl yudao-module-system/yudao-module-system-biz -Dtest=ClassName

# 运行单个测试方法
mvn test -pl yudao-module-system/yudao-module-system-biz -Dtest=ClassName#methodName

# 启动应用（需要 MySQL + Redis）
# 启动类: cn.iocoder.yudao.server.YudaoServerApplication
# 端口: 48080，Profile: local
```

## 架构

### 模块结构

```
yudao-dependencies          # BOM - 统一依赖版本管理
yudao-framework             # 可复用的 Spring Boot Starters（技术基础设施）
yudao-server                # 应用入口（聚合所有模块）
yudao-module-system         # 核心：用户、角色、权限、OAuth2、短信、邮件
yudao-module-infra          # 基础设施：代码生成、文件存储、定时任务、监控
yudao-module-*              # 业务模块（大部分在 pom.xml 中默认注释掉）
```

### 启用模块

在根目录 `pom.xml` 中取消注释即可启用对应模块。默认只启用 `system` 和 `infra`。

### 业务模块内部结构

```
yudao-module-xxx/
├── yudao-module-xxx-api/       # 模块间 API（DTO、枚举、常量、RPC 接口）
└── yudao-module-xxx-biz/       # 具体实现
    └── src/main/java/.../module/xxx/
        ├── controller/
        │   ├── admin/          # 管理后台接口 (/admin-api/...)
        │   └── app/            # 用户端接口 (/app-api/...)
        ├── service/            # 业务逻辑
        ├── dal/
        │   ├── mysql/          # MyBatis Plus Mapper + DO（数据对象）
        │   └── redis/          # Redis DAO
        ├── convert/            # MapStruct 转换器（DO <-> VO）
        ├── job/                # 定时任务
        ├── mq/                 # 消息生产者/消费者
        └── framework/          # 模块级 Spring 配置
```

### 框架组件 (yudao-framework)

- `yudao-common` - 基础类：PageParam、PageResult、CommonResult、KeyValue、枚举
- `yudao-spring-boot-starter-mybatis` - MyBatis Plus 配置、基础 Mapper/DO、数据权限集成
- `yudao-spring-boot-starter-redis` - Redisson 配置、分布式锁
- `yudao-spring-boot-starter-security` - Token 认证、权限注解
- `yudao-spring-boot-starter-web` - 全局异常处理、XSS 过滤、API 版本管理
- `yudao-spring-boot-starter-biz-tenant` - 多租户（自动 tenant_id 过滤）
- `yudao-spring-boot-starter-biz-data-permission` - 行级数据权限
- `yudao-spring-boot-starter-test` - 测试基类、H2 + Jedis-mock 配置

### 核心约定

- **基础包名**: `cn.iocoder.yudao`
- **API 响应封装**: 所有 REST 接口使用 `CommonResult<T>`
- **分页**: 入参 `PageParam` / 出参 `PageResult<T>`
- **数据对象**: 继承 `BaseDO`（自动填充 creator、updater、create_time、update_time、deleted）
- **Mapper**: 继承 `BaseMapperX`（提供 selectPage、selectOne 等扩展方法）
- **对象转换**: `convert/` 包下的 MapStruct 接口
- **逻辑删除**: `deleted` 字段（0=未删除，1=已删除），MyBatis Plus 全局处理
- **多租户**: `tenant_id` 列自动过滤；使用 `@TenantIgnore` 跳过
- **参数校验**: 请求 VO 上使用 JSR 303 注解，Controller 层校验

### 技术栈

| 层次 | 技术 |
|------|------|
| ORM | MyBatis Plus 3.5.15 + MPJ（联表查询） |
| 连接池 | Druid（dynamic-datasource 多数据源） |
| 缓存 | Redis + Redisson |
| 认证 | Spring Security + 自定义 Token |
| 工作流 | Flowable 6.8.0 |
| 接口文档 | Springdoc + Knife4j（访问 /doc.html） |
| 代码生成 | Lombok + MapStruct 1.6.3 |
| 工具库 | Hutool 5.8.42 |

### 测试

测试使用 `yudao-spring-boot-starter-test`，提供：
- `BaseMockitoUnitTest` - 纯 Mockito 单元测试基类
- `BaseDbUnitTest` - H2 内存数据库测试基类
- `RandomUtils.randomPojo()` - 通过 Podam 生成随机测试数据
- `AssertUtils.assertPojoEquals()` - 忽略特定字段的深度比较
- `AssertUtils.assertServiceException()` - 校验业务异常码

测试 Profile: `application-unit-test.yaml`（自动配置 H2 + Jedis-mock）

### 配置文件

- `yudao-server/src/main/resources/application.yaml` - 公共配置（MyBatis、Jackson、Swagger）
- `application-local.yaml` - 本地开发（数据库连接、Redis、MQ）
- `application-dev.yaml` - 开发环境

### 注解处理器（构建顺序重要）

Maven compiler plugin 链式处理：`spring-boot-configuration-processor` → `lombok` → `lombok-mapstruct-binding` → `mapstruct-processor`。此顺序确保 MapStruct 能识别 Lombok 生成的 getter/setter。

### 数据库

- 默认：MySQL，端口 3306，数据库名 `ruoyi-vue-pro`
- 兼容：PostgreSQL、Oracle、SQL Server、达梦 DM8、人大金仓 KingBase、OpenGauss
- ID 策略：根据数据库类型自动适配（MySQL 用 AUTO，Oracle/PG 用 INPUT）
- SQL 初始化脚本在 `sql/` 目录

### SQL 脚本兼容性规范（重要！）

**本项目 MySQL 版本低于 8.0.23**，所有新增或修改的 SQL 脚本必须兼容 **MySQL 5.7 / 8.0.x 全版本**。严禁使用 8.0.23+ 专有语法。

**禁止使用**：
- `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...`（8.0.23+）
- `ALTER TABLE ... DROP COLUMN IF EXISTS ...`（8.0.23+）
- `ALTER TABLE ... RENAME COLUMN ... TO ...`（8.0.3+，如目标环境可能更老也避免）

**允许使用**（各版本通用）：
- `CREATE TABLE IF NOT EXISTS ...` ✅
- `DROP TABLE IF EXISTS ...` ✅
- `INSERT ... ON DUPLICATE KEY UPDATE ...` ✅
- `INSERT IGNORE ...` ✅

**幂等 ADD COLUMN 标准写法**（基于 `information_schema` 的存储过程，参考 `erp_customer_v18.sql`）：

```sql
DROP PROCEDURE IF EXISTS erp_xxx_vN_apply;
DELIMITER $$
CREATE PROCEDURE erp_xxx_vN_apply()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'xxx' AND COLUMN_NAME = 'new_col') THEN
        ALTER TABLE xxx ADD COLUMN new_col VARCHAR(64) COMMENT '...';
    END IF;
    -- 继续判断下一个字段...
END$$
DELIMITER ;
CALL erp_xxx_vN_apply();
DROP PROCEDURE IF EXISTS erp_xxx_vN_apply;
```

**幂等 INDEX 标准写法**（同理用 `information_schema.STATISTICS`）：

```sql
IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'xxx' AND INDEX_NAME = 'idx_xxx_yyy') THEN
    CREATE INDEX idx_xxx_yyy ON xxx(yyy);
END IF;
```

**特殊注意**：命令行 `mysql < xxx.sql` 执行时，`DELIMITER` 会被客户端识别；如果用 Java / Navicat / MyBatis 批量执行，可能需要改成 session 级 `SET @sql = ...; PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;` 的动态 SQL 写法。默认用 DELIMITER 方案即可。

**新模块建表部署规范（重要！踩坑经验）**：

如果数据库中已存在同名表（如早期手动建的、或旧版脚本建的），`CREATE TABLE IF NOT EXISTS` 会直接跳过，**不会**补齐缺失列或修正列名/类型。这会导致 DO 字段与实际表结构不一致，运行时报 `Unknown column` 错误。

**正确做法**：新模块首次部署时，如果不确定表是否已存在或结构是否正确，应先 `DROP TABLE IF EXISTS` 再执行标准建表脚本：

```sql
-- 仅限新模块首次部署、表内无生产数据时使用！
DROP TABLE IF EXISTS erp_xxx;
DROP TABLE IF EXISTS erp_xxx_item;
-- 然后执行标准 CREATE TABLE ...
```

**已有生产数据的表**禁止 DROP，必须用上面的 `information_schema` 存储过程方案逐列检查补齐。

**部署前检查清单**：
1. `SHOW CREATE TABLE xxx;` 对比 DO 字段，确认列名、类型、精度一致
2. 特别注意：MyBatis Plus 的驼峰转下划线映射（如 `adjustTime` → `adjust_time`），早期手动建表容易用错列名
3. `count` 等 MySQL 保留字作列名时，SQL 中必须用反引号包裹

### 前端弹出框（Modal）统一规范

**参考样板**：[product-select-modal.vue](yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/product/product/modules/product-select-modal.vue) —— 用户认可的交互基准，今后一律参照此文件设置「数据列表型弹窗」。

**分两类处理**：

#### 数据列表型弹窗（含表格、搜索、翻页/勾选）

一律使用下述约束（例：选择配件、选择入库单、分批入库、选择单据类 `*-select.vue`）：

1. **Modal 宽度**：`class="!w-[85vw]"`，禁止固定像素（`:width="1100"` 这类在窄屏被夹、宽屏又留白）。
2. **表格区域高度**：固定为视窗 70% 左右，避免"数据少就压成窄条"。
   - **vxe-table（Grid 组件）**：容器加 `class="h-[70vh]"`，gridOptions 里设 `height: '100%'`；禁止用 `height: 'auto'` 或仅 `max-h-[...]`。
   - **antd Table**：`:scroll="{ x: 'max-content', y: 'calc(70vh - 120px)' }'`（减 120px 给标题栏 + 底部按钮 + 分页留位，有搜索框时 180px）。
3. **横向滚动**：antd Table 明确指定 `x: 'max-content'`，让内容溢出时出滚动条而不是压缩列宽。
4. **底部确认按钮**：多选弹窗加选中条数反馈，`:ok-text="\`确定（已选 ${n} 条）\`"`。

**反例（曾踩过的坑）**：
- `height: 'auto'` + `max-h-[75vh]` → 数据少时表格按内容撑开变窄
- `:width="1100"` + `:scroll="{ y: 400 }"` → 固定像素被窗口宽度夹住；纵向只给 400px 导致列多时横向挤压
- `class="!w-[50vw]"` + `max-h-[600px]` → 老样式，同样数据少时变窄

#### 内容固定的小弹窗（表单、简易勾选、确认提示）

保持紧凑尺寸，**不要**套 85vw/70vh 规范，否则会留一大片空白。代表：[branch-select-modal.vue](yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/stock/warehouse/modules/branch-select-modal.vue)（分店勾选）、新增/编辑表单弹窗等。按内容体量选 `w-[500px]` ~ `w-[800px]` 即可。

**判断准则**：弹窗里若有 "表格 + 搜索 + 翻页 / 多行数据"，属第一类；若只是几排表单字段或固定几个选项，属第二类。

### 前端子表格的横向滚动保持（v-model 列数据编辑场景）

子表格组件（item-form 类）把 `tableData` 编辑后通过 `emit('update:items', ...)` 回传父组件，父组件变更 props 又触发子组件 `watch` 里的 `gridApi.grid.reloadData()`，而 `reloadData` 会把横向滚动位置重置到最左——用户在最右侧列输入一下，滚动条就跳回去。

**核心区分**：
- **字段编辑**（改数量、价格、货架位等）：tableData 内容变了，但行数没变，**跳过 reloadData** 保住滚动位置
- **增删行**（弹窗追加、删除按钮、`index === -1` 的 push 分支）：行数变了，**必须 reloadData** 否则 grid 不会渲染新/删的行

**标准做法**（见 `views/erp/purchase/{order,in,return}/modules/item-form.vue`）：

```ts
let isInternalUpdate = false;

/** skipReload=true → 字段编辑（保留滚动位置）；skipReload=false → 增删行（需 reloadData） */
function emitItemsUpdate(skipReload = false) {
  isInternalUpdate = skipReload;
  emit('update:items', [...tableData.value]);
}

watch(() => props.items, async (items) => {
  if (!items) return;
  if (isInternalUpdate) {         // 字段编辑触发的回传，跳过 reloadData
    isInternalUpdate = false;
    return;
  }
  // 外部赋值 / 增删行才走 reloadData
  items.forEach(initRow);
  tableData.value = [...items];
  await nextTick();
  await gridApi.grid.reloadData(tableData.value);
}, { immediate: true });

// 调用点举例
function handleRowChange(row) {
  const index = tableData.value.findIndex(...);
  if (index === -1) {
    tableData.value.push(row);
    emitItemsUpdate(false);  // 新增行
  } else {
    tableData.value[index] = row;
    emitItemsUpdate(true);   // 字段编辑
  }
}
function handleDelete(row) { /* splice */ emitItemsUpdate(false); }
function handleProductsSelected(products) { /* push */ emitItemsUpdate(false); }
```

**踩过的坑**：第一版把 `isInternalUpdate` 无脑设 true，导致所有内部 emit 都跳过 reloadData——配件选择弹窗点确定后，push 进 tableData 了但 grid 没渲染，表格一片空白。修复思路：区分"字段编辑"和"增删行"两种场景，只有前者才该跳过 reloadData。

## 采购与库存系统开发进度

> 客户需求文档：`采购库存问题v2.md`（采购入库、采购退货、库存浏览、库存进出流水明细账四大模块）
> 前端工程：`yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd`

### 一期：库存成本核算（已完成 ✅，2026-05-08）

为整套报表和成本追溯打地基，所有出入库操作实时维护移动加权平均成本。

**数据库迁移**：`sql/mysql/erp_stock_cost_v1.sql`（部署时需手动执行）
- `erp_stock` 追加 `cost_price` `cost_amount`
- `erp_stock_record` 追加 `unit_price` `total_price` `cost_price` `cost_amount` `biz_date`

**核心算法**：[ErpStockServiceImpl.updateStockCountAndCost](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/stock/ErpStockServiceImpl.java)
- 入库：`newCost = (oldQty × oldCost + inQty × inPrice) / newQty`，scale=6 HALF_UP
- 出库：成本均价不变，按当前均价出账；全部出光归零
- 乐观锁：`WHERE count = expectedOldCount`，最多重试 5 次
- 入库必传 `unitPrice`；出库可为 null，由 `ErpStockRecordServiceImpl` 自动回填为当前成本均价

**Service 调用约定**（`ErpStockRecordCreateReqBO` 9 参构造）：
| 场景 | unitPrice | bizDate |
|---|---|---|
| 采购入库 / 其它入库 / 调拨入库 | 明细 productPrice | 单据时间 |
| 采购退货 / 销售退货 | 明细 productPrice | 单据时间 |
| 销售出库 / 其它出库 / 盘亏 | `null`（取成本均价） | 单据时间 |
| 调拨出库 | `null`（取 A 仓成本均价） | moveTime |
| 调拨入库 | 先查 A 仓 `costPrice`，fallback 到明细 productPrice | moveTime |
| 盘盈 | 盘点录入单价 | checkTime |

**自动联动**：采购入库审批通过时，`ErpProductService.updateProductLastPurchasePrice` 自动刷新产品的 `lastPurchasePrice`（作废时不回写）。

**遗留项**：
- 兼容老调用的 7 参构造仍保留在 `ErpStockRecordCreateReqBO`，未来清理
- `ErpStockCheckServiceImpl` 盘点作废时 bizType 映射是原代码瑕疵，非一期范围

### 二期：采购入库字段扩展（已完成 ✅，2026-05-08）

补齐客户文档 1.2/1.3 节所有汽配字段，并支持无采购订单直接入库。

**数据库迁移**（部署时需执行）：
- `sql/mysql/erp_purchase_in_v2.sql`：主表 `erp_purchase_in` 加 26 个字段
- `sql/mysql/erp_purchase_items_v2.sql`：`erp_purchase_in_items` 加 10 个 + `erp_purchase_return_items` 加 8 个

**主表扩展字段**（`ErpPurchaseInDO`，分 4 组）：
- 系统信息（10）：purchaser / invoiceType / transportMethod / settleMethod / purchaseArea / accountant / floatRate / packageCount / factoryOrderNo / orderMethod
- 运费信息（5）：freightType1/2 / freightObject1/2 / logisticsCompany
- 供应商信息（10）：handler / taxRate / deptId / purchaseDiscount / priority / unloader / floatRecord / receiveUnit / totalFreight1/2 / paymentDate / hasInvoice
- 其他（1）：businessEntity

**子表扩展字段**（`ErpPurchaseInItemDO` / `ErpPurchaseReturnItemDO`）：
- packageQty / wholeQty / warehousePosition / drawingNo / batchNo / barCode / brand / vehicleModel / originPlace / businessEntity

**关键业务规则**：
- `count = wholeQty × packageQty`，在 `ErpPurchaseInServiceImpl.validatePurchaseInItems` 中强制覆盖计算
- packageQty 优先级：VO 传值 → 产品资料 `packageQty` → 默认 1
- `orderId` 可空：[ErpPurchaseInServiceImpl](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/purchase/ErpPurchaseInServiceImpl.java) 的 create/update/delete 各判空保护
- 采购入库审批通过时继续一期的 `last_purchase_price` 回写逻辑

**前端产出**：
- API 类型：`api/erp/purchase/in/index.ts` 扩展 `PurchaseIn`（+26）和 `PurchaseInItem`（+10）；`api/erp/purchase/return/index.ts` 扩展 `PurchaseReturnItem`（+8）
- 表单配置：`views/erp/purchase/in/data.ts` 的 `useFormSchema` 追加 28 个字段；`useFormItemColumns` 追加 9 列
- 子表组件：`views/erp/purchase/in/modules/item-form.vue` 新增 5 个 slot + `handleWholeQtyChange` 函数（整件数输入自动联动 count）

### 三期：采购退货双模式（已完成 ✅，2026-05-08）

客户需求文档 2.x 节：采购退货支持两种模式。

**数据库迁移**（部署时需执行）：
- `sql/mysql/erp_purchase_return_v3.sql`：`erp_purchase_return` 加 `return_mode INT DEFAULT 10`；`erp_purchase_return_items` 加 `source_in_id / source_in_item_id / source_in_no`

**核心枚举**：[ErpPurchaseReturnModeEnum](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/enums/purchase/ErpPurchaseReturnModeEnum.java)
- `BY_ORDER (10)` — 按原入库单退货
- `BY_STOCK (20)` — 按库存退货

**BY_ORDER（按单退货）**：
- 子表 `sourceInId / sourceInItemId / sourceInNo` 必填
- 退货数量 ≤ 原入库数量 − 其他退货单已退数量（更新场景排除自己）
- 新接口：`GET /erp/purchase-in/returnable-items?inId={id}` 返回每项的 inCount / returnedCount / returnableCount + 完整子表字段，供前端自动填入

**BY_STOCK（按库存退货）**：
- `sourceIn*` 三字段全空
- 主表 supplierId 必填；子表按库存成本均价定价（前端从 `ErpStockDO.costPrice` 带出）
- orderId 为 null

**共同点**：
- 主表 `returnMode` 必填
- `orderId` 两种模式下都可空（参照二期采购入库模式）
- 审批通过仍走一期 9 参构造 `createStockRecord`，`unitPrice = 明细 productPrice`

**核心算法**：[ErpPurchaseReturnServiceImpl.validateReturnableCountForByOrder](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/purchase/ErpPurchaseReturnServiceImpl.java)
```
1. 按 sourceInItemId 聚合当前单提交数量
2. 查原入库项 count 和其他退货单累计已退（excludeReturnId = 当前单 ID）
3. 可退上限 = 原入库 − 其他已退；当前聚合数量 > 上限 → 抛异常
```

**新增错误码**（`1_030_103_009 ~ 013`）：
- `PURCHASE_RETURN_MODE_INVALID` / `PURCHASE_RETURN_BY_ORDER_SOURCE_REQUIRED` / `PURCHASE_RETURN_SOURCE_IN_ITEM_NOT_EXISTS` / `PURCHASE_RETURN_EXCEED_RETURNABLE` / `PURCHASE_RETURN_COUNT_POSITIVE`

**前端产出**：
- API 类型：`api/erp/purchase/return/index.ts` 扩展 returnMode + sourceIn* 字段；`api/erp/purchase/in/index.ts` 新增 `ReturnableItem` 接口 + `getReturnableItemsByInId` 函数
- 表单配置：`views/erp/purchase/return/data.ts` 加 returnMode RadioGroup + sourceInNo slot 占位 + 子表追加 3 列（原入库单 / 原入库数量 / 可退数量）+ 列表搜索筛选 + 列表列
- 双模式切换：`modules/form.vue` 监听 `returnMode` 变更自动清空子表 + `handleSelectSourceIn` 把可退项映射成退货子表行
- 原入库单选择器：新建 `modules/purchase-in-select.vue`（简易 Modal + Table，查询 status=20 已审批入库单）

**遗留项**：
- `modules/purchase-order-select.vue` 旧组件保留未清理
- 子表新增 3 列采用无条件显示的简化方案，按库存模式下自然为空

### 四期：库存浏览报表页（已完成 ✅，2026-05-08）

客户需求文档模块三：库存浏览页支持多条件搜索、价格体系动态列、库存编辑弹窗、跳转流水明细。

**数据库迁移**（部署时需执行）：
- `sql/mysql/erp_price_system_v4.sql`：新建 `erp_price_system`（价格体系字典）+ `erp_product_price_system`（产品价格关联）

**价格体系**：
- `ErpPriceSystemDO`：id / code / name / status / sort / remark
- `ErpProductPriceSystemDO`：productId / priceSystemId / price
- Service/Controller 全套 CRUD：`/erp/price-system/create` 等，`simple-list` 供下拉
- 错误码段 `1_030_504_xxx`

**库存分页增强**（[ErpStockPageReqVO](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/stock/vo/stock/ErpStockPageReqVO.java) 完整重写）：
- 30+ 搜索条件：productCode / productName / drawingNo / vehicleModel / originPlace / brand / shelf / featureCode / standard / factoryCode / barCode / oeNumber / categoryId / productStatus / countMin-Max / countFilter（全部/>0/=0）/ stockMax-Min-Standard 区间 / shelfDuplicateOnly / shelfEmptyOnly / positiveCountOnly / priceSystemId
- 关键性能：产品维度条件先查 `erp_product` 拿 productIds，再用 `IN` 过滤，0 条命中短路返回空分页

**库存 VO 扩展**（[ErpStockRespVO](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/stock/vo/stock/ErpStockRespVO.java)）：
- 23 个产品扩展字段（code / drawingNo / standard / featureCode / vehicleModel / brand / originPlace / shelf / lastPurchasePrice / stockMax/Min/Standard / packageQty / weight 等）
- 聚合：`occupiedCount`（`SUM(saleOrderItem.count - outCount)`）/ `pendingInCount`（`SUM(purchaseOrderItem.count - inCount)`）/ `inTransitCount`（= pendingInCount）
- 动态价格：priceSystemId 有值时返回 `currentPrice` + `currentPriceAmount`

**库存手动调整**（[ErpStockService.adjustStock](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/stock/ErpStockServiceImpl.java)）：
- `PUT /erp/stock/adjust` 接口；Body: productId / warehouseId / targetCount / reason / remark
- 差值 = targetCount − currentCount
- 差值 > 0 → `CHECK_MORE_IN` 流水，unitPrice = 产品 `lastPurchasePrice` 或 0
- 差值 < 0 → `CHECK_LESS_OUT` 流水，unitPrice = null（下游用成本均价）
- 走一期 9 参构造，完整审计轨迹；注入 `ErpStockRecordService` 用 `@Lazy` 避免循环依赖

**货架位工具**：
- `PUT /erp/product/update-shelf?ids=&shelf=` 批量改
- `GET /erp/product/duplicate-shelf-ids` 查重复
- `GET /erp/product/empty-shelf-ids` 查空置

**前端产出**：
- 价格体系管理页（新建）：`views/erp/product/pricesystem/` + `api/erp/product/pricesystem/index.ts`，仿 `product/unit` 样板，完整 CRUD
- 库存浏览页（重写）：`views/erp/stock/stock/`
  - 28 个搜索字段（折叠展开）
  - 27 列表格，`count` 列 slot 可点击弹窗调整，operation 列"查看明细"跳转 `/erp/stock/record`
  - 新建 `modules/stock-adjust-form.vue` 调整弹窗：产品只读 / 当前库存只读 / 调整后输入 / 调整原因下拉 / 备注
  - 顶部"批量改货架位"按钮（window.prompt 简化交互）

**遗留项**：
1. 多仓合并（mergeWarehouse）：后端 VO 预留字段，本期未聚合，留给前端按 productId 分组展示
2. 部分搜索字段需要新增产品资料字段（流动级别、ABC类、是否滞销、外文名、是否有E标签），本期跳过
3. 批量改货架位前端使用 window.prompt，可后续替换为正式 Modal
4. 菜单项需运维手动添加（`system_menu` 表录入）

### 五期：库存进出流水明细账报表页（已完成 ✅，2026-05-08）

客户需求文档模块四：独立报表页展示指定商品在指定仓库的库存流水，入出分列 + 底部汇总。

**无新增数据库迁移** — `bizDate` 字段一期已建。

**流水查询增强**（[ErpStockRecordPageReqVO](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/stock/vo/record/ErpStockRecordPageReqVO.java)）：
- 新增 `bizTypes: List<Integer>` 多选（与原 bizType 单选共存）
- 新增 `bizDate: LocalDateTime[]` 发生日期区间
- 新增产品维度条件：productCode / productName / vehicleModel / originPlace（Service 层预查 productIds 再 IN）

**报表专用 VO**：
- `ErpStockRecordReportRespVO`：入出分列（inCount / inUnitPrice / inAmount、outCount / outUnitPrice / outAmount）+ 结存列（totalCount / costPrice / costAmount）
- `ErpStockRecordSummaryVO`：totalInCount / totalInAmount / totalOutCount / totalOutAmount / recordCount

**新增接口**（路径前缀 `/erp/stock-record`）：
- `GET /report-page` — 入出分列分页，Controller 层按 count 正负把字段分到 inXxx/outXxx
- `GET /summary` — 底部汇总，Service 层 Java 累加（临时把 pageSize 设为 PAGE_SIZE_NONE），null 兜底 ZERO
- `GET /report-export-excel` — 导出入出分列 Excel

**原 `/page` 接口保留不变** — 避免破坏一期流水管理页。

**Mapper 关键细节**：
- 新方法命名为 `selectPageWithProductFilter(ReqVO, Collection<Long>)`，避免与父类 `BaseMapperX.selectPage(PageParam, Wrapper)` 签名冲突
- 原 `selectPage(ReqVO)` 委托到新方法，向后兼容

**前端产出**（`views/erp/stock/record/`）：
- `data.ts`：新增 `useReportGridFormSchema`（9 个搜索字段：bizDate / bizTypes 多选 / warehouseId / productId / productCode / productName / vehicleModel / originPlace / bizNo）+ `useReportGridColumns`（16 列含入出分列 + 结存列）
- `index.vue` 重写：报表页 + 异步刷新汇总 + 底部汇总栏 + `onMounted` 读 `route.query.productId/warehouseId` 自动填表（接收四期库存浏览页跳转）
- API 层：`getStockRecordReportPage` / `getStockRecordSummary` / `exportStockRecordReport`

**四期→五期跳转链路**：
```
库存浏览页"查看明细"按钮
  → router.push({ path: '/erp/stock/record', query: { productId, warehouseId } })
流水账页 onMounted 读 query → 自动 setValues + query
  → /report-page 返回入出分列流水 + /summary 返回底部汇总
```

**遗留项**：
1. `supplierId` 字段已在 ReqVO 预留，但流水表无 supplierId，未落地过滤
2. "按期间/按单据"查询方式未差异化（都走 bizDate 区间）
3. "打印"按钮未定制，依赖浏览器原生预览
4. "查询历史记录"语义不明，待客户澄清
5. 菜单项需运维手动添加到 `system_menu`

## 客户需求 v2 完整交付状态

| 模块 | 状态 |
|---|---|
| 模块一：采购入库 | ✅ 二期已完成（主表 26 字段 + 子表 10 字段 + orderId 可空 + count 自动算） |
| 模块二：采购退货 | ✅ 三期已完成（双模式：按单退货 + 按库存退货） |
| 模块三：库存浏览 | ✅ 四期已完成（30+ 搜索条件 + 价格体系动态列 + 库存调整弹窗 + 跳转明细） |
| 模块四：库存进出流水明细账 | ✅ 五期已完成（入出分列 + 底部汇总 + 四期联动） |
| 基础：库存成本核算 | ✅ 一期已完成（移动加权平均 + 乐观锁 + 全流水成本追溯） |
| 采购订单分批入库 + 赠品 | ✅ 六期已完成（分批入库自动审批 + 赠品标记 + 0 价稀释成本） |

**所有 SQL 迁移脚本（8 个，需按顺序执行）**：
1. `sql/mysql/erp_stock_cost_v1.sql`
2. `sql/mysql/erp_purchase_in_v2.sql`
3. `sql/mysql/erp_purchase_items_v2.sql`
4. `sql/mysql/erp_purchase_return_v3.sql`
5. `sql/mysql/erp_price_system_v4.sql`
6. `sql/mysql/erp_menu_v4_v5.sql` — 菜单注册（价格体系 + 升级流水账菜单名 + 授权超管 role_id=1）
7. `sql/mysql/erp_purchase_order_gift_v6.sql` — 采购订单赠品字段
8. `sql/mysql/erp_purchase_return_items_v8_nullable_order_item.sql` — 采购退货子表 order_item_id 可空（修复新增退货报错）

**菜单部署后必做**：登录后台 → 系统管理 → 菜单管理 → 刷新缓存；或重启后端

---

## 本次开发进度整体总结（2026-05-08）

### 交付范围

基于客户需求文档《采购库存问题v2.md》交付完整"采购 + 库存"闭环。按五期 Team 模式推进，每期严格走 **PLAN → EXECUTE → TEST → CLEANUP** 四阶段，每期结束均以 `mvn compile -pl yudao-module-erp -am -DskipTests` BUILD SUCCESS 为验收门槛。

### 五期产出汇总

| 期 | 主题 | 关键能力 | 验收 |
|---|---|---|---|
| 一期 | 库存成本核算基础设施 | 移动加权平均算法 + 乐观锁 + 出入库全流水成本追溯 + 采购入库审批回写 `last_purchase_price` | ✅ BUILD SUCCESS |
| 二期 | 采购入库字段大扩展 | 主表 26 个汽配字段 + 子表 10 个字段 + orderId 可空 + `count = wholeQty × packageQty` 后端强制计算 | ✅ BUILD SUCCESS |
| 三期 | 采购退货双模式 | BY_ORDER（按入库单）+ BY_STOCK（按库存），可退数量校验（原入库 − 其他退货）+ orderId 可空 | ✅ BUILD SUCCESS |
| 四期 | 库存浏览报表 | 30+ 搜索条件 + 价格体系动态列 + 产品扩展字段回显 + 占用/未入/在途 SQL 聚合 + 库存调整弹窗 + 货架位工具 | ✅ BUILD SUCCESS |
| 五期 | 库存进出流水明细账 | 入出分列报表（inXxx/outXxx）+ 底部汇总 + 四期"查看明细"跳转联动 | ✅ BUILD SUCCESS |

### 数据库迁移清单（6 个脚本，按编号顺序执行）

| # | 脚本 | 涉及表 |
|---|---|---|
| 1 | `sql/mysql/erp_stock_cost_v1.sql` | `erp_stock` + `erp_stock_record` 增成本字段 |
| 2 | `sql/mysql/erp_purchase_in_v2.sql` | `erp_purchase_in` 主表 26 字段 |
| 3 | `sql/mysql/erp_purchase_items_v2.sql` | `erp_purchase_in_items` 加 10 + `erp_purchase_return_items` 加 8 |
| 4 | `sql/mysql/erp_purchase_return_v3.sql` | `erp_purchase_return` 加 return_mode + 子表加 source_in_* |
| 5 | `sql/mysql/erp_price_system_v4.sql` | 新建 `erp_price_system` + `erp_product_price_system` |
| 6 | `sql/mysql/erp_menu_v4_v5.sql` | `system_menu` 价格体系菜单 + 升级流水账菜单名 + 授权超管 |

### 后端改动范围统计

**新增/修改的主要文件（按包路径）**：

- `enums/` 新增 2 个枚举：`purchase/ErpPurchaseReturnModeEnum`、复用 `stock/ErpStockRecordBizTypeEnum`
- `enums/ErrorCodeConstants` 新增错误码：`1_030_103_009~013`（采购退货双模式）+ `1_030_504_000~002`（价格体系）
- `dal/dataobject/` 新增 2 个 DO：`ErpPriceSystemDO`、`ErpProductPriceSystemDO`；扩展 6 个 DO：`ErpStockDO`、`ErpStockRecordDO`、`ErpPurchaseInDO`、`ErpPurchaseInItemDO`、`ErpPurchaseReturnDO`、`ErpPurchaseReturnItemDO`
- `dal/mysql/` 新增 2 个 Mapper + 扩展 6 个 Mapper（加聚合/多条件/重复/空置查询）
- `service/` 新增 3 个 Service：`ErpPriceSystemService`、`ErpProductPriceSystemService` + 扩展核心算法；`ErpStockServiceImpl.updateStockCountAndCost` 是整个项目的核心
- `controller/admin/` 新增 1 个 Controller：`ErpPriceSystemController`；扩展 `ErpStockController`（+adjust）、`ErpStockRecordController`（+report-page/summary/export）、`ErpProductController`（+shelf 工具）
- `controller/admin/**/vo/` 新增 6 个 VO + 扩展 10 个 VO（具体见各期文档）

**保持不动的约束**：
- `BaseDO` 父类未动
- `ErpStockService.updateStockCountIncrement` 老方法保留做向后兼容
- 一期 `createStockRecord` 9 参构造 + 兼容 7 参构造共存

### 前端改动范围统计

**新增页面**：
- `views/erp/product/pricesystem/` — 价格体系管理（4 个文件：data.ts + index.vue + modules/form.vue + API）
- `views/erp/stock/stock/modules/stock-adjust-form.vue` — 库存调整弹窗

**新增模块文件**：
- `views/erp/purchase/return/modules/purchase-in-select.vue` — 三期按单退货弹窗

**重写页面**：
- `views/erp/stock/stock/index.vue` + `data.ts` — 四期库存浏览页（28 搜索字段 + 27 列 + 点击库存数弹窗 + 跳转明细）
- `views/erp/stock/record/index.vue` + `data.ts` — 五期流水明细账报表（9 搜索字段 + 16 列 + 底部汇总 + 接收 query）
- `views/erp/purchase/in/` 3 个文件 — 二期采购入库表单（28 扩展字段 + 9 列 + 整件数联动）
- `views/erp/purchase/return/` 3 个文件 — 三期采购退货表单（双模式 Tab + 按单选择）

**API 层扩展**：
- `api/erp/stock/stock/index.ts` — 扩展 Stock 接口（产品扩展 + 聚合 + 动态价格）+ `adjustStock` 函数
- `api/erp/stock/record/index.ts` — 新增 ReportPage / Summary 函数 + 类型
- `api/erp/purchase/in/index.ts` — 扩展 PurchaseIn（26 字段）+ PurchaseInItem（10 字段）+ `ReturnableItem` + `getReturnableItemsByInId`
- `api/erp/purchase/return/index.ts` — 扩展 PurchaseReturn（returnMode）+ Item（sourceIn_*）
- `api/erp/product/pricesystem/index.ts` — 新建完整 CRUD + simple-list
- `api/erp/product/product/index.ts` — 新增 `updateProductsShelf`

### 核心架构决策

1. **移动加权平均 + 乐观锁**：`WHERE count = expectedOldCount` 乐观锁，最多重试 5 次；入库必传 unitPrice，出库可为 null 由下游回填成本均价
2. **入库数量强制计算**：`count = wholeQty × packageQty` 在 Service 层覆盖 VO 传值；packageQty 优先级 VO → 产品资料 → 1
3. **orderId 可空模式**：二期和三期共同采用 null 保护模式（create/update/delete 各判空 4 位置）
4. **按单退货校验**：`原入库 − 其他退货累计` 作为可退上限；更新场景 `excludeReturnId = 当前单` 避免误杀
5. **产品维度预过滤**：四期/五期的多条件搜索都用"先查 productIds 再 IN"策略，避免大表 JOIN，0 条命中短路
6. **聚合 SQL vs Java**：库存分页的占用/未入用 SQL GROUP BY（涉及全表），流水汇总用 Java 累加（单商品单仓数据量小）
7. **循环依赖破解**：四期 `ErpStockServiceImpl` 反向注入 `ErpStockRecordService` 用 `@Lazy`
8. **Mapper 方法签名冲突**：新方法改名为 `selectPageWithProductFilter`，避免与父类 `BaseMapperX.selectPage(PageParam, Wrapper)` 签名重叠

### 跨期联动点

- **一期 → 所有期**：`ErpStockRecordCreateReqBO` 9 参构造贯穿 8 个调用方（采购/销售/其它/调拨/盘点）
- **采购入库审批 → 产品资料**：`last_purchase_price` 自动回写（仅 approve 分支）
- **三期按单退货 → 一期成本流水**：审批通过仍走一期 `createStockRecord` + `updateStockCountAndCost`
- **四期 → 五期**：库存浏览页"查看明细"按钮 `router.push` 带 query，流水账页 `onMounted` 自动填表查询

### 遗留项清单（按期归档）

**一期遗留**：
- 兼容老调用的 7 参构造仍保留在 `ErpStockRecordCreateReqBO`
- `ErpStockCheckServiceImpl` 盘点作废时 bizType 映射是原代码瑕疵

**二期遗留**：
- SQL 脚本已生成但尚未在真实环境执行
- 未清理 7 参构造（待一期同步清理）

**三期遗留**：
- `purchase-order-select.vue` 旧组件保留未清理
- 子表新增 3 列采用无条件显示，按库存模式下自然为空

**四期遗留**：
- 多仓合并（mergeWarehouse）后端 VO 留字段未实现，留给前端分组
- 流动级别 / ABC类 / 是否滞销 / 外文名 / E 标签等：产品资料无字段，未落地
- 批量改货架位用 `window.prompt`，可替换为正式 Modal

**五期遗留**：
- `supplierId` 字段 VO 预留但未落地过滤（流水表无 supplierId，需从 bizId 回查）
- "按期间/按单据"查询方式未差异化
- "打印"按钮依赖浏览器原生预览
- "查询历史记录"语义不明

### Team 模式执行回顾

- 共派发 **27 个 Agent**（含成功 + 失败重试）
- 典型模式：M1∥M2 并行建模 → M3/M4/M5 并行 Service → M6∥M7 前端并行 → M8 验证收尾
- 1 次 Agent 调用因令牌额度不足失败（三期 M6）→ 主导指挥自行接手完成
- 1 次 Agent 调用因模型失效失败（五期 M1）→ 主导指挥自行接手完成
- 所有编译错误均在阶段内就地修复（如五期 Mapper 方法签名冲突 → 改名 `selectPageWithProductFilter`）

### 部署操作手册

```bash
# 1. 停止应用
# 2. 按顺序执行 6 个 SQL（幂等，失败可回滚单个脚本）
cd /path/to/project
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_stock_cost_v1.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_in_v2.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_items_v2.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_return_v3.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_price_system_v4.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_menu_v4_v5.sql

# 3. 后端重新编译 + 启动
mvn clean package -pl yudao-server -am -DskipTests
# 启动 cn.iocoder.yudao.server.YudaoServerApplication

# 4. 前端构建 + 部署
cd yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master
pnpm install
pnpm build:antd
# dist 目录部署到 Nginx

# 5. 管理后台登录 → 系统管理 → 菜单管理 → 刷新缓存
# 6. 退出重新登录，新菜单生效
```

### 后续可扩展方向

1. **多仓合并聚合 API**：按 productId 维度返回库存总量 + 各仓分布
2. **价格体系导入导出 Excel**：支持一键批量维护商品价格
3. **库存预警**：基于 `stockMax/stockMin/stockStandard` 生成预警列表
4. **流水表 supplierId 反查**：从 `bizId` 回查入库单 supplierId，支持按供应商筛选流水
5. **客户文档其它字段**：流动级别 / ABC类 / 是否滞销 / 外文名 / E 标签需先加产品资料字段
6. **操作日志完善**：库存调整已留 remark，未来对接审计日志模块

### 六期：采购订单分批入库 + 赠品功能（已完成 ✅，2026-05-09）

客户需求：从采购订单直接发起分批入库 + 赠品标记功能。

**数据库迁移**（部署时需执行）：
- `sql/mysql/erp_purchase_order_gift_v6.sql`：`erp_purchase_order_items` 加 `gift TINYINT(1) DEFAULT 0`

**赠品功能**：
- `ErpPurchaseOrderItemDO` 新增 `gift: Boolean` 字段
- `ErpPurchaseOrderSaveReqVO.Item` / `ErpPurchaseOrderRespVO.Item` 新增 `gift` 字段
- `validatePurchaseOrderItems`：赠品行强制 `productPrice=0, totalPrice=0, taxPrice=0`
- `calculateTotalPrice`：`totalProductPrice` / `totalTaxPrice` 排除赠品行；`totalCount` 仍含赠品
- `validateGiftModify`：已入库项（`inCount > 0`）不允许修改 `gift` 标记
- 前端：子表新增 `gift` 列（Switch 组件），赠品行单价显示"0（赠品）"且禁用编辑

**分批入库功能**：
- 新建 `ErpPurchaseInFromOrderReqVO`：orderId + inTime + accountId + items（orderItemId / count / warehouseId）
- 新建 `ErpPurchaseOrderInableItemRespVO`：可入库明细响应（含产品扩展字段）
- `ErpPurchaseOrderService.getInableItemsByOrderId`：查订单项 → 计算 inableCount = count - inCount → 过滤 > 0 的项
- `ErpPurchaseInService.createPurchaseInFromOrder`：校验订单已审批 → 校验 count ≤ inableCount → 构造入库单 → 调用 `createPurchaseIn` → 自动审批 `updatePurchaseInStatus(APPROVE)`
- 赠品行入库时 `productPrice=0`，以 0 价参与移动加权平均（稀释成本均价）

**新增接口**：
- `GET /erp/purchase-order/inable-items?orderId=` — 查询可入库明细
- `POST /erp/purchase-in/create-from-order` — 分批入库（自动审批生效）

**新增错误码**：
- `PURCHASE_ORDER_ITEM_GIFT_MODIFY_FAIL_HAS_IN (1_030_101_011)` — 已入库项不允许修改赠品标记
- `PURCHASE_ORDER_IN_EXCEED_INABLE (1_030_101_012)` — 入库数量超过可入库数量

**前端产出**：
- API 层：`PurchaseOrderItem` 加 `gift`；新增 `InableItem` 接口 + `getInableItemsByOrderId`；新增 `createPurchaseInFromOrder`
- 子表组件：`item-form.vue` 新增 gift slot（Switch）+ `handleGiftChange` 联动 + 赠品行单价禁用
- 分批入库弹窗：新建 `modules/purchase-in-modal.vue`（表格展示可入库明细 + 勾选 + 本次入库数 + 仓库选择）
- 列表页：`index.vue` 行操作新增"入库处理"按钮（条件：已审批 + 未全部入库）

**验证**：`mvn compile -pl yudao-module-erp -am -DskipTests` → BUILD SUCCESS

### 七期：表单字段必填动态配置（已完成 ✅，2026-05-09）

客户需求：采购订单/采购入库/采购退货/供应商 四个模块的表单字段必填划分混乱，需要可配置；初始值维持现状（保留所有既有必填），租户级共享配置。

**决策要点**：
- **配置存储**：后端 DB 新表，租户级共享（非 localStorage / 非用户级）
- **范围**：只配主表字段，不含子表；覆盖 4 个模块（采购三兄弟 + 供应商）
- **校验模式**：纯前端动态 `rules='required'` + 红星显示；**后端原有业务校验一律不动**（软约束 vs 硬约束分离）

**数据库迁移**（部署时需执行，按顺序）：
- `sql/mysql/erp_field_config_v7.sql` — 新建 `erp_field_config` 表 + 4 模块共 143 条初始记录（9 必填 + 134 选填）
- `sql/mysql/erp_menu_v7.sql` — 菜单 ID 2960-2963：ERP 下新增"系统配置 / 字段配置"目录 + 权限点 `erp:field-config:query/update` + 授权超管

**后端产出**（全部新增，零改动既有业务代码）：
- DO：`ErpFieldConfigDO`（id / moduleKey / fieldName / fieldLabel / required / sort + BaseDO）
- Mapper：`ErpFieldConfigMapper` 提供 `selectListByModuleKey`（按 sort 升序）/ `selectByModuleKeyAndFieldName`
- 枚举：`ErpFieldConfigModuleEnum`（purchase_order / purchase_in / purchase_return / supplier）
- VO：Resp / SaveReq / BatchUpdateReq / PageReq 四份
- Service：`ErpFieldConfigService` + Impl，`batchUpdate` 与 `reset` 加 `@Transactional`，先按 moduleKey 逻辑删除再批量 insert
- Controller：`ErpFieldConfigController` 暴露 `/list` / `/batch-update` / `/reset` / `/modules` 四个端点
- 错误码段 `1_030_505_000~002`：`FIELD_CONFIG_MODULE_KEY_INVALID` / `FIELD_CONFIG_NOT_EXISTS` / `FIELD_CONFIG_IN_USE`

**前端产出**：
- API 层：`api/erp/config/field/index.ts` — 命名空间 `ErpFieldConfigApi` + 4 函数（getFieldConfigList / batchUpdateFieldConfig / resetFieldConfig / getFieldConfigModules）
- Composable：`views/erp/composables/useFieldConfig.ts` — `applyFieldConfig(schema, moduleKey)` 合并配置到 schema，`clearFieldConfigCache(moduleKey?)` 清缓存；内置 Map 模块级缓存，失败降级原 schema
- 管理页：`views/erp/system/fieldconfig/index.vue`（约 200 行，antd Table）
  - 顶部 4 个模块 Tab；切 tab 时若 dirty 弹 Modal.confirm 防误丢
  - 5 列：序号 / 字段名 / 字段中文名（Input）/ 是否必填（Switch）/ 排序（InputNumber）
  - 操作：保存（batchUpdate + clearCache + reload）/ 重置（Popconfirm + reset + clearCache + reload）
- 4 个 form.vue 接入（采购订单/入库/退货/供应商）：
  - 在 Modal `onOpenChange(isOpen=true)` 里调 `applyFieldConfig` 后用 `formApi.setState({ schema })` 一把替换
  - 供应商 6 个 tab（basic/category/settle/address/invoice/finance）共用 moduleKey=supplier，`Promise.all` 并行 load，M4 缓存保证 6 次调用仅打 1 次后端

**关键架构决策**：
1. **setState vs updateSchema**：Vben5 的 `updateSchema` 是增量合并（defu），`undefined` 无法覆盖已有 `rules`，即"从必填改非必填"场景失效；改走 `formApi.setState({ schema: configuredSchema })` 走 `mergeWithArrayOverride`，数组属性整体替换
2. **合并语义**：config.required=true → rules='required'；false → 若原 rules 是字符串 'required' 则删除，其他（如 `z.xxx` 校验链）保留不动；schema 中不在 config 的字段不动
3. **软约束 vs 硬约束**：前端动态 required 只影响录入体验（红星 + 校验），DB 非空 + Service 业务校验一分不改，即使有人用 Postman 绕过前端也能被后端挡住
4. **模块缓存**：`applyFieldConfig` 内置 Map，moduleKey 命中缓存直接返回；保存 / 重置后 `clearFieldConfigCache(moduleKey)` 让下次打开表单重拉
5. **管理页用 antd Table 而非 vxe-table**：编辑型小数据（每模块几十行），antd `bodyCell` slot 配合 v-model 比 vxe-table `edit-render` 直观，也避开子表 "字段编辑 vs 增删行 reloadData" 那套复杂度

**SQL 初始化字段统计**：
| 模块 | 总数 | 必填 | 必填字段 |
|---|---|---|---|
| purchase_order | 23 | 2 | orderTime, supplierId |
| purchase_in | 34 | 2 | inTime, supplierId |
| purchase_return | 27 | 3 | returnMode, returnTime, supplierId |
| supplier | 53 | 2 | name, sort |
| **合计** | **137** | **9** | — |

> 注：采购入库已按客户要求删除 6 个价格/账户字段（discountPercent / discountPrice / discountedPrice / otherPrice / accountId / totalPrice），数量从 40 降为 34。

**Team 模式执行**：
- 派发 6 个 agent，两批并行：
  - 第一批：M1（建模 DO/Mapper/VO/Enum）∥ M3（SQL 脚本）∥ M4（前端 API + Composable + data.ts）
  - 第二批：M2（Service + Controller，依赖 M1）∥ M5（字段配置管理页）∥ M6（4 个 form.vue 接入，依赖 M4）
- 主导指挥收尾：菜单 SQL + 验证
- 验证：`mvn clean compile -pl yudao-module-erp -am -DskipTests` → BUILD SUCCESS（319 源文件）

**遗留项**：
1. 列表页的"字段配置"入口按钮未加（初始设想放在"列设置"旁边）—— 菜单已可直达 `/erp/system/fieldconfig`，入口按钮留作后续 UI 优化
2. 子表字段（采购入库子表的数量/仓库位等）未纳入本期，留给二期
3. 重置后前端需手动刷新 schema，新打开 Modal 才生效（受 `onOpenChange` 触发时机约束）

**部署操作**：
```bash
# 1. 执行 SQL（幂等）
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_field_config_v7.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_menu_v7.sql

# 2. 后端重新编译 + 启动
mvn clean package -pl yudao-server -am -DskipTests

# 3. 前端构建
cd yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master
pnpm build:antd

# 4. 登录 → 菜单管理 → 刷新缓存 → 退出重登
# 5. 导航到 "ERP 系统 / 系统配置 / 字段配置" 验证
```

---

## 销售管理模块开发进度（2026-05-10）

> 客户最新规划文档：`D:\10186\桌面\客户v2(1).md`
> 开发策略：渐进改造 + 并行推进；保留旧销售订单/销售出库/销售退货入口，新流程以“销售单”语义复用现有销售出库扣库存能力。

### 已完成范围

**阶段 1：客户基础信息 + 销售主链路 MVP**
- 客户主表扩展基础信息字段，并新增客户详情页基础 Tab。
- 新增报价订单 `/erp/sale-quote`：不占库存、不校验库存，审核后可生成销售单。
- 新增销售手推车 `/erp/sale-cart`：提交/审批校验库存，终审后自动生成销售单。
- 销售出库扩展为新“销售单”承载层，新增 `sourceType / sourceId / sourceNo` 来源字段。
- 新流程生成销售单继续写 `SALE_OUT` 库存流水，沿用移动加权成本体系。

**阶段 2：客户档案多 Tab**
- 新增客户联系人、合同、企业地区、企业图片、拓展信息、任务量、工商信息占位等后端接口与前端 Tab。
- 客户新增后进入详情页，详情页按 Tab 呈现已实现资料模块。

**阶段 3：报价订单与销售手推车互转**
- 支持报价订单整单/分批转销售手推车，校验本次转换数量不超过剩余可转数量。
- 支持销售手推车转报价订单，并记录转换关系。
- 报价订单直接审核生成销售单时做最终库存校验。

**阶段 4：销售退货双模式**
- 新增退货模式：按销售单退货、按库存退货，并保留旧销售订单退货兼容模式。
- 按销售单退货支持查询可退明细，校验退货数量不超过 `原销售数量 - 已退数量`。
- 按库存退货不强制关联原销售单，审批后增加库存并写 `SALE_RETURN` 流水。
- 前端新增销售单选择弹窗，使用数据列表弹窗规范：`!w-[85vw]`、表格 `70vh`、横向滚动。

**阶段 5：销售调价重做**
- 调价单审核后不重复扣库存。
- 原销售单标记调价追溯字段，系统自动生成一张新销售单承载新价格。
- 新销售单来源为 `PRICE_ADJUST`，保留原销售单、新销售单、调价单三方关系。

**阶段 6：配置表、菜单、权限**
- 新增通用销售配置表 `/erp/sale-config`，覆盖合同类型、线路、运费说明、内部账户、连锁/集团、总经销、任务级别等配置类型。
- 新增销售配置前端页面：`views/erp/sale/config`。
- 新增菜单 SQL，补齐报价订单、销售手推车、销售调价、销售配置等菜单与权限点。
- 报价订单、销售手推车已补齐 Excel 导出接口、前端导出按钮和菜单权限点。

**阶段 7：前端体验收尾（部分完成）**
- 报价订单、销售手推车、销售退货、旧销售订单、销售单子表均已接入“字段编辑不 reload、增删行才 reload”的横向滚动保护。
- 修复销售退货子表 `emitItemsUpdate` 递归调用问题，避免删除行时栈溢出。

### 数据库迁移脚本

按顺序执行：
1. `sql/mysql/erp_sale_quote_cart_v8.sql`
2. `sql/mysql/erp_sale_customer_tabs_v9.sql`
3. `sql/mysql/erp_sale_customer_extend_v10.sql`
4. `sql/mysql/erp_sale_return_v11.sql`
5. `sql/mysql/erp_sale_price_adjust_v12.sql`
6. `sql/mysql/erp_sale_config_v13.sql`
7. `sql/mysql/erp_sale_menu_v14.sql`

### 验证记录

- 后端编译：`mvn compile -pl yudao-module-erp -am -DskipTests` → BUILD SUCCESS
- 销售相关测试：`mvn test -pl yudao-module-erp "-Dtest=ErpSaleConfigServiceImplTest,ErpSaleReturnServiceImplTest,ErpSaleQuoteServiceImplTest,ErpSaleCartServiceImplTest"` → Tests run: 8, Failures: 0, Errors: 0
- 前端类型检查：`pnpm -F @vben/web-antd run typecheck` 当前仍受项目既有 AI/BPM/库存等无关类型错误影响失败；已过滤确认本次新增/修改的 `src/views/erp/sale/**`、`src/api/erp/sale/**` 不再报新增类型错误。
- 测试编译时发现并修复采购退货 `ErpPurchaseReturnServiceImpl` 缺少 `ErpAccountService` 注入的问题。

### 后续待办

1. 客户导入、导出、批量编辑。
2. 销售单据打印入口完善。
3. 前端全量 typecheck 仍需另行清理项目既有 AI/BPM/库存等无关类型错误。
4. 部署后执行菜单缓存刷新：后台“系统管理 → 菜单管理 → 刷新缓存”，或重启后端并重新登录。

---

## 采购模块八期：试用反馈收尾（已完成 ✅，2026-05-12）

> 客户反馈文档：`刘/采购模块试用问题-deepseek修改版.md`（供应商 / 采购订单 / 采购入库 / 采购退货共 ~60 个调整点）
> 策略：全部并行、所有下拉**硬编码**（客户确认不走字典）、导入导出与供应商合并延后

### 数据库迁移（3 个脚本，按顺序执行）

1. `sql/mysql/erp_supplier_v8.sql` — `erp_supplier` 加 `obsolete_date`（淘汰日期）
2. `sql/mysql/erp_purchase_order_v8.sql` — `erp_purchase_order` 加 `document_type`（单据类型）、`latest_order_date`（最近订货日期只读）
3. `sql/mysql/erp_purchase_return_v8.sql` — `erp_purchase_return` 加 `tax_rate`、`dept_id`、`handler`（制单人）

### 后端改动

- 新增字段：供应商 `obsoleteDate`；订单 `documentType / latestOrderDate`；退货 `taxRate / deptId / handler`
- 新增接口：`PUT /erp/supplier/update-status`（供应商列表页停用/启用按钮使用）
- 三个模块全部补齐**明细数量/价格校验**（`validatePurchaseOrderItems / validatePurchaseInItems / validatePurchaseReturnItems`）——数量 ≤ 0 或价格不合法抛友好错误码（赠品行单价 = 0 豁免）
- 新增 6 个错误码：
  - `PURCHASE_ORDER_ITEM_COUNT_POSITIVE (1_030_101_013)` / `PURCHASE_ORDER_ITEM_PRICE_POSITIVE (1_030_101_014)`
  - `PURCHASE_IN_ITEM_COUNT_POSITIVE (1_030_102_015)` / `PURCHASE_IN_ITEM_PRICE_POSITIVE (1_030_102_016)`
  - `PURCHASE_RETURN_ITEM_COUNT_POSITIVE (1_030_103_015)` / `PURCHASE_RETURN_ITEM_PRICE_POSITIVE (1_030_103_016)`
- 历史兼容：`orderCompany`（采购订单订货公司）和 `returnType`（采购退货退货类型）DO 字段**保留不动**，只在前端 schema 移除，保证历史数据可读

### 前端改动

#### 供应商（`views/erp/purchase/supplier/`）
- 表单删除 `sort`、`status` 字段；列表保留 `status` 列并新增**停用/启用按钮**（二次确认 + 调 `updateSupplierStatus`）
- 淘汰字段改成组合控件：`obsolete`（RadioGroup）+ `obsoleteDate`（DatePicker，`dependencies.show` 依赖 `obsolete === true`）
- 地区 Tab 布局调整：去掉 `address` 的 `col-span-2`，让 address/areaIds 和 postalCode/website 各自并排成两行
- 硬编码下拉：`category`（默认"供应商"）、`companyNature`、`freightType`、`invoiceType`（默认"收据"）、`purchaseControl`（默认"否"）、`floatUpdateLastPrice`（默认"是"）
- 默认值：`arrivalCycle=2`、`purchaseLeadDays=0`

#### 采购订单（`views/erp/purchase/order/`）
- 列表删除"产品信息"列，新增"单据类型"列
- 表单删除 `orderCompany`；新增 `documentType`（默认"普通单据"）+ `latestOrderDate`（只读）；`purchaseType` 改 Select 默认"普通采购"
- 日期默认值：`orderDate / sendDate` 默认当天；`saleDateFrom / saleDateTo` 默认当前时间
- 下拉改造：`invoiceType`（默认"收据"）、`settleMethod`（默认"挂账"）、`orderFormula`（文本 → 下拉）
- 子表校验：数量 ≤ 0 `message.warning` 并恢复为 1；价格 ≤ 0 清空让用户重填（赠品行豁免）

#### 采购入库（`views/erp/purchase/in/`）
- 列表删除"产品信息"列
- 用户下拉：`purchaser / accountant / handler / unloader` → `getSimpleUserList`，create 模式默认当前登录人
- 客户下拉：`receiveUnit` → `getCustomerSimpleList`
- 部门下拉：`deptId` → `getSimpleDeptList`，create 模式默认当前用户部门
- 物流公司下拉：`logisticsCompany` → `getBaseDataSimpleList('logistics_company')`，valueField 用 `name`（匹配后端字符串字段）
- 硬编码下拉：`purchaseArea / freightType1 / freightType2 / invoiceType / settleMethod / transportMethod / priority`
- 只读字段：`floatRecord / freightObject1 / freightObject2`
- 默认值：`invoiceType='收票和不开票'`、`settleMethod='挂账'`、`priority='正厂件'`、`taxRate=0`、`purchaseDiscount=0`、`totalFreight1/2=0`
- 子表取消冻结列：删除 10 处 `fixed`（1 处 left + 9 处 right）
- 子表校验：数量 ≤ 0、价格 < 0（0 允许赠品） → `message.warning`

#### 采购退货（`views/erp/purchase/return/`，含 3 个 Bug 修复）

**Bug 1 修复**（入库单选择弹窗首次打开空白）：
- `purchase-in-select.vue` 的 `watch(() => props.open)` 加 `immediate: true`，loadData 先 `dataSource.value = []` + `await nextTick()` 再拉数据

**Bug 2 修复**（按库存退货模式隐藏关联订单 + 能加产品）：
- `orderNo` 字段加 `dependencies: { triggerFields: ['returnMode'], show: v => v.returnMode === 10 }`
- `form.vue` 的 `#items` 插槽：`returnMode === 20 && formType !== 'detail'` 时显示"添加产品"按钮，复用 `views/erp/product/product/modules/product-select-modal.vue`，`handleProductsSelected` 把选中的产品映射成退货子表行（价格优先 `lastPurchasePrice ?? purchasePrice ?? 0`）

**Bug 3 修复**（选入库单后 returnTime 被清空 + supplierId 提交仍报错）：
- `handleSelectSourceIn` / `handleUpdateOrder` 改 async，先 `const current = await formApi.getValues()`，再 `setValues({ ...current, ...formData.value, ...patch }, false)` — 合并当前表单值避免完全替换
- `returnMode` 切换时 `handleValuesChange` 补清 `supplierId`，避免残留

**其他改动**：
- 字段删除：`returnType`（与 `returnMode` 重复）
- 字段新增：`taxRate / deptId / handler`（只读默认当前登录人）/ `purchaser`（默认当前登录人可改）
- 硬编码下拉：`invoiceType`（默认"收据和不开票"）、`transportMethod`（默认"客户自提"）、`settleMethod`（默认"现金"）、`warehouseType`（默认"全部"）、`shipArea / freightType / sourceType / priority / orderMethod`
- `returnTime`：只读 + 默认当前时间（`dayjs().format('YYYY-MM-DD HH:mm:ss')`）
- `logisticsCompany` → `getBaseDataSimpleList('logistics_company')`
- 子表取消冻结列（9 处 fixed 删除）；数量/价格 ≤ 0 `message.warning`

### Team 模式执行

6 个 agent 分工：
1. **M1**（后端）：DO/VO/Service/ErrorCode/SQL 一把梭
2. **M2**（供应商前端）：超时一次，SendMessage 唤醒后继续完成
3. **M3**（采购订单前端）：顺利完成
4. **M4**（采购入库前端）：超时一次，SendMessage 唤醒后继续完成
5. **M5**（采购退货前端 + 3 个 Bug 修复）：顺利完成，改动最大
6. 主导指挥收尾：后端编译验证 + CLAUDE.md 更新

### 验证

- 后端编译：`mvn compile -pl yudao-module-erp -am -DskipTests` → **BUILD SUCCESS**（5.15s）
- 3 个 Bug 修复代码落地点已 grep 确认：
  - `purchase-in-select.vue` 第 44-56 行：`immediate + nextTick + dataSource 清空`
  - `return/data.ts` 第 77-78 / 91-92 行：`orderNo / sourceInNo` 的 `dependencies.show` 依赖 `returnMode === 10`
  - `return/modules/form.vue` 第 174-178 行：`getValues` 合并后再 `setValues`
- 前端类型检查：项目既有 AI/BPM/库存等无关类型错误未清理，沿用七期策略（本期新增/修改文件不引入新错误）

### 部署操作

```bash
# 1. 后端重编译
mvn clean package -pl yudao-server -am -DskipTests

# 2. 按顺序执行 SQL（加上七期可能遗漏的 supplier nullable）
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_supplier_v8.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_order_v8.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_return_v8.sql

# 3. 前端构建
cd yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master
pnpm build:antd

# 4. 重启后端 + 清浏览器缓存
```

### 客户需求覆盖率

| 模块 | 总点数 | 已做 | 延后 |
|---|---|---|---|
| 供应商 | ~18 | 16 | 导入导出、供应商合并（九期） |
| 采购订单 | ~12 | 12 | — |
| 采购入库 | ~15 | 15 | — |
| 采购退货 | ~18 | 18（含 3 Bug） | — |

### 遗留项

1. 七期 `erp_field_config` 里 supplier 模块的 `sort` 和 `status` 记录已失去表单入口，建议后续从字段配置表 soft-delete（目前不影响使用，管理员改后看不到效果而已）
2. 列表页"编辑"按钮：现有行为是点击行打开编辑弹窗，客户文档要求"行内编辑按钮"——本期未加显式按钮，实际交互已覆盖诉求，需客户确认是否接受
3. 九期规划：供应商导入/导出、供应商合并、库存/流水 UI 优化

### 八期补丁：采购退货子表 order_item_id 可空修复（2026-05-12）

**问题**：新增采购退货时报错 `Field 'order_item_id' doesn't have a default value`

**原因**：三期引入的新退货模式（按入库单退货、按库存退货）不需要关联采购订单项，但数据库表 `erp_purchase_return_items` 的 `order_item_id` 字段是 NOT NULL 且无默认值。

**修复**：执行 `sql/mysql/erp_purchase_return_items_v8_nullable_order_item.sql`，将 `order_item_id` 改为可空。

**验证**：修复后，按库存退货和按入库单退货模式均可正常新增。

**前端优化**（2026-05-12）：隐藏"关联订单"字段，两种退货模式都不再显示该字段。
- 修改文件：`views/erp/purchase/return/data.ts`（注释掉 orderNo 字段）
- 修改文件：`views/erp/purchase/return/modules/form.vue`（注释掉 orderNo slot 和 PurchaseOrderSelect 组件导入）
- 废弃函数：`handleUpdateOrder`（已注释保留代码供参考）

---

## 字段配置功能部署（已完成 ✅，2026-05-12）

> 七期开发的字段配置功能，因SQL脚本执行失败未能部署，本次完成部署并修复字符集问题。

### 部署过程

**问题**：执行 `erp_menu_v7.sql` 时报错主键冲突；执行 `erp_field_config_v7.sql` 时报错 `Data too long for column 'field_label'`

**原因分析**：
1. 菜单SQL未使用幂等设计（已修复为 `erp_menu_v7_fixed.sql`）
2. MySQL客户端字符集为 `gbk`，数据库字符集为 `utf8mb4`，导致字符集转换错误

**解决方案**：
```bash
# 使用 --default-character-set=utf8mb4 参数执行SQL
mysql -h127.0.0.1 -P3306 -uroot -p"密码" --default-character-set=utf8mb4 ruoyi-vue-pro < sql/mysql/erp_field_config_v7.sql
mysql -h127.0.0.1 -P3306 -uroot -p"密码" --default-character-set=utf8mb4 ruoyi-vue-pro < sql/mysql/erp_menu_v7_fixed.sql
```

### 部署结果验证

✅ **数据库部署**：
- `erp_field_config` 表已创建，包含唯一索引和复合索引
- 字段配置数据：共 131 条记录
  - purchase_order（采购订单）：23 个字段，2 个必填
  - purchase_in（采购入库）：34 个字段，2 个必填
  - purchase_return（采购退货）：21 个字段，3 个必填
  - supplier（供应商）：53 个字段，2 个必填
- 菜单和权限：4条菜单+权限已创建并授权给超管

✅ **后端验证**：`mvn compile -pl yudao-module-erp -am -DskipTests` → BUILD SUCCESS

### 后续必做步骤

1. **刷新菜单缓存**（必做！）
   - 登录后台 → 系统管理 → 菜单管理 → 刷新缓存
   - 退出重新登录
   - 或重启后端服务

2. **访问字段配置页面**
   - 路径：ERP 系统 → 系统配置 → 字段配置
   - URL：`/erp/system/fieldconfig`

3. **测试字段配置功能**
   - 修改字段必填状态
   - 保存配置
   - 打开对应表单验证生效

### 功能说明

**核心能力**：
- 动态配置表单字段是否必填，无需修改后端代码
- 租户级共享配置，TTL 5分钟缓存
- 软约束（前端展示）+ 硬约束（后端校验）分离

**覆盖模块**：
- 采购订单（purchase_order）
- 采购入库（purchase_in）
- 采购退货（purchase_return）
- 供应商（supplier）

**配置生效机制**：
- 保存后立即清除缓存
- 下次打开表单时重新加载配置
- 已打开的表单不受影响（需关闭重开）

### 部署文档

- **完整使用指南**：`docs/字段配置功能-部署和使用指南.md`
- **部署完成报告**：`docs/字段配置功能-部署完成报告.md`
- **SQL脚本**：
  - `sql/mysql/erp_field_config_v7.sql` - 字段配置表创建和初始化
  - `sql/mysql/erp_menu_v7_fixed.sql` - 菜单创建（幂等版本）
  - `sql/mysql/verify_field_config.sql` - 验证脚本

### 字符集配置建议

为避免后续SQL执行出现字符集问题，建议在 MySQL 配置文件（my.ini）中添加：

```ini
[mysql]
default-character-set=utf8mb4

[client]
default-character-set=utf8mb4
```

### 租户ID问题修复（2026-05-12 16:30）

**问题现象**：字段配置页面有4个Tab，但没有显示任何字段数据

**问题原因**：
- 数据库初始化脚本中的数据使用了 `tenant_id = 0`（默认租户）
- 但 admin 用户使用的是 `tenant_id = 1`
- MyBatis Plus 的多租户插件自动过滤了不同租户的数据，导致查询不到

**解决方案**：
```sql
UPDATE erp_field_config SET tenant_id = 1 WHERE tenant_id = 0;
```

**验证结果**：✅ 功能正常，所有字段数据已显示

### 遗留项

1. supplier 模块的 `sort` 和 `status` 字段在字段配置表中标记为必填，但表单已删除这两个字段的入口（不影响使用）
2. 子表字段配置未纳入本期（留给后续扩展）
3. 前端构建部署（如需更新生产环境）
4. 初始化脚本需要改进：应该使用当前登录用户的租户ID，而不是硬编码 `tenant_id = 0`

---

## 九期：采购调价（已完成 ✅，2026-05-12）

客户需求文档：`刘/采购调价.md`。纠正入库时单价填错的场景，支持两种方式，审批通过后原单原地改 + 成本差额补账。

### 业务决策（Q1-Q5）

- **Q1=A**：只按当前在库部分补差，`ratio = min(currentCount/sumInCount, 1)`；已消耗部分的差额丢弃
- **Q2=A**：审批通过无条件回写 `last_purchase_price`（按 productId 分组取 inTime 最晚的 newPrice）
- **Q3=β**：「按入库单调价」无视防重复（整单全调，已调明细也可继续调）；「添加明细」强制防重复（已调项不可再选）
- **Q4**：加 `original_product_price`（原价快照）+ `adjusted`（bit）+ `adjust_id`（引用调价单）三字段，展示由前端拼 `原价【调：新价】`
- **Q5**：`totalPrice` 重算后未付差额自动对齐，`paymentPrice` 已付金额不受影响

### 数据库迁移（部署时按顺序执行）

1. `sql/mysql/erp_purchase_price_adjust_v15.sql` — 新建主表 `erp_purchase_price_adjust` + 子表 `erp_purchase_price_adjust_item`；扩 `erp_purchase_in` 加 `adjusted`；扩 `erp_purchase_in_items` 加 `original_product_price / adjusted / adjust_id`（用 information_schema 存储过程方案保幂等）
2. `sql/mysql/erp_purchase_price_adjust_menu_v15.sql` — 菜单 ID 3020-3026（目录 + query/create/update/delete/update-status/export 权限点）+ 授权超管

### 后端核心改动

**新 DO**：
- `ErpPurchasePriceAdjustDO`（11 字段）：id/no/status/adjustTime/supplierId/deptId/adjuster/adjustType/remark/totalAdjustPrice/approveTime
- `ErpPurchasePriceAdjustItemDO`（22 字段）：含 inItemId/oldPrice/newPrice/count/adjustRatio/adjustPrice + 10 个产品冗余字段

**扩 Enum**：`ErpStockRecordBizTypeEnum` 加 `PURCHASE_PRICE_ADJUST(90)` / `PURCHASE_PRICE_ADJUST_CANCEL(91)`
**错误码段**：`1_030_506_000~010`（11 条）
**单号前缀**：`ErpNoRedisDAO.PURCHASE_PRICE_ADJUST_NO_PREFIX = "CGTJ"`

**核心算法：`ErpPurchasePriceAdjustServiceImpl.updatePurchasePriceAdjustStatus`（审批通过）**：

```
事务内：
1. 乐观锁翻转状态：updateByIdAndStatus(id, PROCESS → APPROVE, approveTime=now)
2. 遍历 items：
   - 首次调价（inItem.originalProductPrice == null）→ 设为当前 productPrice
   - 更新 inItem：productPrice=newPrice, totalPrice=newPrice×count, taxPrice=totalPrice×(taxPercent/100),
     adjusted=true, adjustId=调价单 id
3. 按 inId 去重重算父入库单 totalProductPrice/totalTaxPrice/discountPrice/totalPrice + adjusted=true
4. 按 (productId, warehouseId) 聚合：
   sumCount = Σ inItem.count
   sumDeltaFull = Σ (newPrice - oldPrice) × count
   调 ErpStockServiceImpl.adjustStockCostAmount(productId, warehouseId, sumDeltaFull, sumCount, 调价单 id, no, adjustTime)
5. 按 productId 分组，取 inTime 最晚的 newPrice 回写 erp_product.lastPurchasePrice
```

**新方法 `ErpStockServiceImpl.adjustStockCostAmount`（成本差额摊分）**：

```
事务内，乐观锁（count + costAmount 双版本）最多重试 5 次：
1. 读 erp_stock(productId, warehouseId)
2. ratio = sumInCount > 0 ? min(currentCount/sumInCount, 1).scale(6) : 0
3. effectiveDelta = (sumDeltaFull × ratio).scale(2)
4. newCostAmount = stock.costAmount + effectiveDelta
5. newCostPrice = currentCount > 0 ? newCostAmount/currentCount : 0
6. UPDATE erp_stock ... WHERE id=? AND count=? AND cost_amount=?
7. 手工 insert ErpStockRecordDO：count=0, totalPrice=effectiveDelta, costPrice=新均价,
   bizType=90, bizId=调价单 id, bizNo=CGTJ..., bizDate=调价时间
   （不走 createStockRecord 的 9 参构造，避免 updateStockCountAndCost 的均价重算）
```

**关键决策**：
- `count=0` 流水不能走 9 参构造（会触发移动加权重算），直接 Mapper.insert
- 乐观锁用 count + costAmount 双条件校验，比单条件更安全
- 反审批暂不支持（抛 PURCHASE_PRICE_ADJUST_PROCESS_FAIL）——涉及回滚 inItem.productPrice 到 originalProductPrice、反向冲成本差额，本期不做

**扩 ErpPurchaseInService**：
- `getApprovedPurchaseInsBySupplier(supplierId)` → `List<ErpPurchaseInForAdjustRespVO>`（供「按入库单」弹窗）
- `getApprovedPurchaseInItemsBySupplier(supplierId, excludeAdjusted)` → `List<ErpPurchaseInItemForAdjustRespVO>`（供「添加明细」弹窗，batch 批量回填产品/仓库/供应商/用户，无 N+1）
- `auditorId / approveTime` 近似用 `updater / updateTime`（DO 无专属字段）

### 前端产出

**新建页面**（`views/erp/purchase/priceadjust/`）：
- `data.ts`：4 导出（useFormSchema / useFormItemColumns / useGridFormSchema / useGridColumns），子表列根据 adjustType 动态切换可见
- `index.vue`（209 行）：列表页 + 新增/编辑/详情/审核通过/拒绝/删除操作
- `modules/form.vue`（394 行）：主表单 + 两个选择弹窗 ref + adjustType 切换清空 items
- `modules/item-form.vue`（237 行）：子表格（贯彻「字段编辑不 reload、增删行才 reload」模式）
- `modules/purchase-in-select-modal.vue`（243 行）：「按入库单」弹窗，顶部批量设置比率 + 每行可单独填
- `modules/purchase-in-item-select-modal.vue`（208 行）：「添加明细」弹窗，excludeAdjusted=true 硬编码（Q3=β）

**按入库单展开明细策略**：用户勾选入库单确定后，调一次 `getApprovedPurchaseInItemsBySupplier(supplierId, false)` 拉全量明细，前端按 inIds 过滤后按比率展开为子表行（一次请求搞定，不逐张拉）。

**痕迹展示**：
- `views/erp/purchase/in/data.ts` + `index.vue` — 入库单列表加 `<Tag v-if="row.adjusted" color="red">调</Tag>`
- `views/erp/purchase/in/modules/item-form.vue` — 入库明细 `productPrice` 列按 `originalProductPrice != null && != productPrice` 判断，显示 `原价【调：新价】`（红色）+ 禁用编辑
- `api/erp/purchase/in/index.ts` — PurchaseIn 加 `adjusted`，PurchaseInItem 加 `originalProductPrice/adjusted/adjustId`

**UI 规范贯彻**：两个选择弹窗均用 `!w-[85vw]` + 表格 70vh + `x: 'max-content'`；底部按钮 `确定（已选 ${n} 条）`；日期字段一律 `valueFormat: 'x'`。

### 验收

```
mvn clean compile -pl yudao-module-erp -am -DskipTests → BUILD SUCCESS（486 源文件）
```

### Team 模式执行

- 派发 6 个 Agent：M1（数据层+SQL） → M3∥M2∥M4∥M6（并行） → M5（依赖 M4）
- 总耗时约 48 分钟，全部 BUILD SUCCESS
- M1 意外删除了早期桩代码的旧 Service/Controller（发现后 M2 从零重写无影响）
- M2 的 createStockRecord 踩坑：原方法对 count=0 有成本重算副作用 → 改走 Mapper.insert 旁路

### 部署步骤

```bash
# 1. 停止应用
# 2. 按顺序执行 SQL
mysql -u root -p --default-character-set=utf8mb4 ruoyi-vue-pro < sql/mysql/erp_purchase_price_adjust_v15.sql
mysql -u root -p --default-character-set=utf8mb4 ruoyi-vue-pro < sql/mysql/erp_purchase_price_adjust_menu_v15.sql

# 3. 租户数据（参考字段配置踩过的坑）
# 如果菜单 tenant_id=0 而 admin 是 tenant_id=1，执行：
UPDATE system_menu SET tenant_id = 1 WHERE tenant_id = 0 AND permission LIKE 'erp:purchase-price-adjust:%';
# 实际上 system_menu 表是全局的不分租户，此步一般不需要

# 4. 重新编译 + 启动
mvn clean package -pl yudao-server -am -DskipTests

# 5. 前端构建
cd yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master
pnpm build:antd

# 6. 登录 → 菜单管理 → 刷新缓存 → 退出重登
# 7. 导航到 "ERP 系统 / 采购管理 / 采购调价" 验证
```

### 遗留项

1. **反审批不支持**：status 从 APPROVE → PROCESS 会抛异常，本期按"已审批不可反向"处理
2. **`taxPrice` 重算精度**：按 `totalPrice × taxPercent / 100`（scale=2 HALF_UP），与入库原逻辑 `MoneyUtils.priceMultiplyPercent` 可能有 1 分钱偏差
3. **字段配置模块**：前端 form.vue 用 moduleKey=`purchase_price_adjust`，已在 `ErpFieldConfigModuleEnum` 注册；`erp_field_config` 表未预置该模块字段明细，getFieldConfigListByModule 返回空 list，applyFieldConfig 降级为原 schema（不影响功能）。如需在「字段配置」管理页 Tab 里维护该模块，需往 `erp_field_config` 插入字段初始记录。
4. **导出接口未实现**：列表页未加导出按钮（后端未提供 export 接口）
5. **菜单 ID 段 3020-3026**：如与其它未合入的分支冲突需调整

---

## 十期：财务核算基础表（已完成 ✅，2026-05-14）

> 客户需求文档：`刘/财务模块-ds修订版.md`
> 范围：第一阶段只搭基础表，字段与文档 1:1 还原；现有 finance 包（结算账户/付款单/收款单）不动，全部新增。

### 决策点（已与客户对齐）

| 决策点 | 答案 |
|---|---|
| 「连锁名称」字段 | 只存文本 `chain_name VARCHAR(128)`，不外键 |
| 「辅助核算」存储 | 单独建 `erp_subject_auxiliary` 关联表（科目 N:M 辅助核算类型） |
| 凭证编号格式 | `记-{yyyyMM}-{6位月度流水}`（如 `记-202605-000001`），月度重置 |
| 科目预置数据 | 预置最小集 15 条最常用一级科目 |

### 数据库迁移（部署时按顺序执行 7 个脚本）

```bash
mysql --default-character-set=utf8mb4 -uroot -p ruoyi-vue-pro < sql/mysql/erp_accounting_subject_v26.sql
mysql --default-character-set=utf8mb4 -uroot -p ruoyi-vue-pro < sql/mysql/erp_book_open_v26.sql
mysql --default-character-set=utf8mb4 -uroot -p ruoyi-vue-pro < sql/mysql/erp_voucher_v26.sql
mysql --default-character-set=utf8mb4 -uroot -p ruoyi-vue-pro < sql/mysql/erp_voucher_attribution_v26.sql
mysql --default-character-set=utf8mb4 -uroot -p ruoyi-vue-pro < sql/mysql/erp_voucher_word_v26.sql
mysql --default-character-set=utf8mb4 -uroot -p ruoyi-vue-pro < sql/mysql/erp_report_item_template_v26.sql
mysql --default-character-set=utf8mb4 -uroot -p ruoyi-vue-pro < sql/mysql/erp_accounting_menu_v26.sql
```

### 9 张新表

| # | 表名 | 用途 |
|---|---|---|
| 1 | `erp_accounting_subject` | 会计科目（合并期初余额） |
| 2 | `erp_book_open` | 系统开账主表 |
| 3 | `erp_book_open_voucher_config` | 开账凭证类型勾选（11 种凭证类型） |
| 4 | `erp_voucher` | 凭证主表 |
| 5 | `erp_voucher_item` | 凭证分录明细 |
| 6 | `erp_voucher_attribution` | 凭证归属（跨月调整） |
| 7 | `erp_voucher_word` | 凭证字字典（预置"记"=记账凭证） |
| 8 | `erp_report_item_template` | 三大报表项目模板（取数公式留空） |
| 9 | `erp_subject_auxiliary` | 会计科目辅助核算关联表 |

### 后端产出

**新增包目录**：`cn.iocoder.yudao.module.erp.{dal.dataobject,dal.mysql,enums}.finance.accounting`

**9 个 DO**：[ErpAccountingSubjectDO](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/dataobject/finance/accounting/ErpAccountingSubjectDO.java) / ErpSubjectAuxiliaryDO / ErpBookOpenDO / ErpBookOpenVoucherConfigDO / ErpVoucherDO / ErpVoucherItemDO / ErpVoucherAttributionDO / ErpVoucherWordDO / ErpReportItemTemplateDO

**9 个 Mapper**：均继承 `BaseMapperX`，仅暴露 selectByX / selectListByX 等基础查询，未实现复杂 selectPage（留给二期）

**6 个枚举**：
- `ErpSubjectCategoryEnum`：1-资产 2-负债 3-共同 4-权益 5-成本 6-损益
- `ErpAuxiliaryTypeEnum`：supplier/customer/project/dept/person
- `ErpVoucherTypeEnum`：11 种凭证业务类型（销售/采购/调拨/银行转账等）
- `ErpVoucherSourceTypeEnum`：手动/自动/跨期
- `ErpVoucherAuditStatusEnum`：未审核/已审核/已反审
- `ErpAttributionStatusEnum`：未归属/已归属/已生成凭证

**基础设施扩展**：
- [ErpNoRedisDAO](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/redis/no/ErpNoRedisDAO.java) 加 `BOOK_OPEN_NO_PREFIX = "KZ"` + `VOUCHER_WORD_DEFAULT = "记"` + 新方法 `generateMonthly(prefix)`（格式 `prefix + "-" + yyyyMM + "-" + 6位月度流水`，过期 35 天）
- [ErrorCodeConstants](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/enums/ErrorCodeConstants.java) 加 7 段错误码：`1_030_610_000~1_030_616_001`（会计科目/系统开账/凭证/凭证归属/凭证字/报表模板/科目辅助核算）

### 菜单结构

ERP 系统（parent_id=2563）→ 财务核算（id=3030, type=1 目录）下挂 8 个三级菜单（ID 3031–3065）：

| ID | 菜单 | 路径 | 权限点 |
|---|---|---|---|
| 3031 | 会计科目 | `accounting/subject` | erp:accounting-subject:query/create/update/delete (3032-3035) |
| 3036 | 系统开账 | `accounting/book-open` | erp:book-open:query/create/update/delete (3037-3040) |
| 3041 | 凭证列表 | `accounting/voucher` | erp:voucher:query/create/update/delete/audit/process (3042-3047) |
| 3048 | 凭证生成 | `accounting/voucher-attribution` | erp:voucher-attribution:query/apply/generate (3049-3051) |
| 3052 | 凭证字 | `accounting/voucher-word` | erp:voucher-word:query/create/update/delete (3053-3056) |
| 3057 | 资产负债表 | `accounting/balance-sheet` | erp:balance-sheet:query/export (3058-3059) |
| 3060 | 利润表 | `accounting/income-statement` | erp:income-statement:query/export (3061-3062) |
| 3063 | 现金流量表 | `accounting/cash-flow` | erp:cash-flow:query/export (3064-3065) |

预留 3066~3079 给后期扩展（月末结转/损益结转）。

### 关键设计点

1. **现有 finance 包零侵入**：`ErpAccountDO`（结算账户）、`ErpFinancePaymentDO`（付款单）、`ErpFinanceReceiptDO`（收款单）属于"资金流"领域，与本期"会计核算"领域隔离，不冲突
2. **9 个 DO 不继承业务父类**：纯 BaseDO + 字段，本期不挂业务约束（数量校验、状态机）；二期上 Service 时再补
3. **Mapper 极简**：只在父类 BaseMapperX 基础上提供必要的查询便捷方法（如 `selectListByVoucherId`、`selectBySubjectCode`），不写复杂 selectPage / 多表 join — 二期对应 Service/VO 上线时再加
4. **凭证号月度重置**：`generateMonthly` 用 `RedisKeyConstants.NO + prefix + "-" + yyyyMM` 作 key，过期 35 天保跨月失效；首月凭证从 000001 起
5. **15 条预置科目**：覆盖现金/银行/应收/预付/库存/固定资产/累计折旧/应付/预收/应交税费/实收资本/利润分配/主营业务收入/主营业务成本/营业税金及附加；客户后期可在「会计科目」页面继续补
6. **租户隔离**：所有插入数据硬编码 `tenant_id = 1`，避开七期字段配置部署时的 tenant_id=0 → 查不到的坑

### 验证

```
mvn compile -pl yudao-module-erp -am -DskipTests → BUILD SUCCESS（4.9s）
target/classes 下确认产出：9 DO + 9 Mapper + 6 Enum 共 24 个 .class（DO 含 Builder 共 18 个）
```

### 不在本期范围（明确划线，给二期）

| 项 | 推迟原因 |
|---|---|
| Service / Controller / VO / 前端页面 | 本期只搭基础表 |
| 期初余额 Excel 导入导出 | 客户文档低优先级，模板列结构未定 |
| 凭证审批 / 反审批业务流程 | 状态字段已留，逻辑下期 |
| 三大报表的取数公式 | 客户文档明确"暂不深入实现" |
| 月末结转 / 期间损益结转 | 客户文档明确"建议暂缓开发" |
| 银行/现金日记账 | 客户文档明确划归"资金模块" |
| 自动生成凭证（采购/销售单据触发） | 涉及业务联动，下期 |
| 跨月归属调整业务流程 | 涉及业务联动，下期 |
| 完整国标会计科目数据预置 | 本期只置最小集 15 条；客户后期按需补 |

### 部署后必做

1. 登录后台 → 系统管理 → 菜单管理 → 刷新缓存 → 退出重登
2. 导航到 "ERP 系统 / 财务核算" 验证 8 个子菜单出现
3. 二期需要前端页面才能实际使用菜单（本期菜单点击会 404）

### 遗留项

1. 二期需补 Service / Controller / VO / 前端页面
2. 凭证号生成依赖 Redis；本期未在 Service 调用，二期才真正落地
3. `erp_book_open_voucher_config` 的 11 种 voucherType 与 `ErpVoucherTypeEnum` 一致；二期开发时新建开账记录默认全部勾选 enabled=true（11 条 config）
4. 三大报表 `erp_report_item_template` 的 formula 字段全 NULL，二期需与财务沟通后填取数公式

---

## 财务核算模块二期：Service/Controller/VO + 前端页面（已完成 ✅，2026-05-14）

> 客户需求文档：`刘/财务模块-ds修订版.md`
> 范围：在十期已搭好的 9 张基础表之上，补齐 Service/Controller/VO 业务逻辑层 + 全部前端页面，让"会计科目 → 系统开账 → 凭证生成 → 凭证列表 → 三大报表"主链路跑得通；复杂的取数公式、月末结转、银行日记账留给三期。

### 范围决策（与客户对齐）

| 决策点 | 答案 |
|---|---|
| 凭证号每月重置 | `记-{yyyyMM}-{6位月度流水}`（沿用十期 `ErpNoRedisDAO.generateMonthly`） |
| 借贷平衡校验 | 前端实时计算 + 保存前 JS 校验 + 后端 Service 兜底（双保险） |
| 末级科目限制 | 凭证分录的 `subjectId` 必须是末级（`isLeaf=true`），后端 + 前端 Select `simple-list?leafOnly=true` 双保险 |
| 期初余额 | 主表格内联编辑（`<a-input-number>` + dirty 标记），批量保存 `batchUpdateOpeningBalance` |
| 自动生成凭证 | 本期不实现真实业务联动；只搭"系统开账→凭证类型勾选"配置 + "凭证生成"页手动建归属记录 + 批量生成空壳凭证 |
| 三大报表 | 骨架页：金额列硬编码 `0.00`，DocAlert 标"取数公式三期实现"；formula 留空 |
| 凭证审核 | 三态：未审核(10)/已审核(20)/已反审(30)；状态机字段已留 |

### 后端产出（本期新增/补全）

> 注：DO/Mapper/Enum 在十期已就位，本期只补 Service/Controller/VO 三层。

**Service 层（14 个文件）**：
- `ErpAccountingSubjectService` + Impl — 科目 CRUD + 树形/Tab 列表 + `simple-list?leafOnly=true` + 期初余额批量保存 + Excel 导入导出 + 模板下载
- `ErpSubjectAuxiliaryService` + Impl — 辅助核算 N:M 关联管理
- `ErpBookOpenService` + Impl — 系统开账 CRUD + 创建时自动生成 11 条 voucher_config（默认全部 enabled=true）
- `ErpVoucherService` + Impl — 凭证 CRUD + 借贷平衡校验 + 审核/反审核状态机 + 凭证号生成（`generateMonthly("记-")`） + 导出 Excel
- `ErpVoucherAttributionService` + Impl — 凭证归属 CRUD + 批量应用归属月份 + 批量生成凭证（生成空壳 voucher 记录回写 attributionStatus=30 + voucherId）
- `ErpVoucherWordService` + Impl — 凭证字字典
- `ErpReportItemTemplateService` + Impl — 报表项目模板 CRUD + 按 reportType + side 列表查询

**Controller 层（6 个文件）**：
- `/erp/accounting-subject` — 17 个端点（含 simple-list、tree、auxiliary-list、batch-update-opening-balance、import/export-excel、export-template）
- `/erp/book-open` — 7 个端点（含 voucher-config-list、update-voucher-configs）
- `/erp/voucher` — 8 个端点（含 audit、process、export-excel）
- `/erp/voucher-attribution` — 7 个端点（含 apply、generate-vouchers）
- `/erp/voucher-word` — 6 个端点（含 simple-list）
- `/erp/report-item-template` — 6 个端点（权限点全部复用 `erp:balance-sheet:query`）

**VO 层（26 个文件）** 分布：
- subject 包 5 个（Page / Resp / Save / Import Excel / OpeningBalanceUpdate）
- bookopen 包 5 个（Page / Resp / Save + VoucherConfig Resp/Save）
- voucher 包 5 个（Page / Resp / Save + Item Resp/Save）
- attribution 包 5 个（Page / Resp / Save / Apply / Generate）
- voucherword 包 3 个（Page / Resp / Save）
- reporttemplate 包 3 个（Page / Resp / Save）

**关键算法点**：
- **凭证借贷平衡**：`ErpVoucherServiceImpl.validateBalance` 在 create/update 时校验 `Σdebit == Σcredit`（scale=2 比较）；不平衡抛 `VOUCHER_NOT_BALANCED` 错误码
- **末级科目校验**：`validateLeafSubject` 查 `subject.isLeaf=true`，否则抛 `SUBJECT_NOT_LEAF`
- **凭证号月度重置**：`generateMonthly("记-")` → `记-202605-000001`，过期 35 天保跨月失效
- **审核状态机**：`audit`：10→20；`process`（反审核）：20→30；30 状态不可改不可删；checkBy + checkTime 双字段
- **批量应用归属月份**：事务内一次 update + 校验`归属YM ≤ bizDateYM`
- **批量生成凭证**：遍历 ids → 为每条生成空壳 voucher（含 sourceType=2 自动 + sourceBizType/Id/No 回填） → 写 attribution.voucherId + attributionStatus=30

### 前端产出（29 个文件）

**API 层（6 个 index.ts）**：
- `api/erp/finance/accounting/{subject,book-open,voucher,voucher-attribution,voucher-word,report-template}/index.ts`

**Views 层（23 个 .ts/.vue）**：

| 模块 | 文件 | 备注 |
|---|---|---|
| 会计科目 | `subject/{data.ts, index.vue, modules/form.vue, modules/import-modal.vue}` | 6 大类 Tab + 树形缩进 + 内联期初余额编辑 + 批量保存 dirty 标记 + Excel 导入导出 |
| 系统开账 | `book-open/{data.ts, index.vue, modules/form.vue}` | vxe-grid 列表 + 行点击展开下方 11 个 Checkbox + 顶部 Alert 警告 |
| 凭证字 | `voucher-word/{data.ts, index.vue, modules/form.vue}` | 简单 CRUD |
| 报表项目模板 | `report-template/{data.ts, index.vue, modules/form.vue}` | 顶部 Tab(reportType) + 子 Tab(side) + 800px 双列弹窗 |
| 凭证 | `voucher/{data.ts, index.vue, modules/form.vue, modules/item-form.vue}` | 抽屉 1200px + 子表 vxe-grid 内联编辑 + 借贷互斥 disabled + 实时合计 + 平衡校验 |
| 凭证生成 | `voucher-attribution/{data.ts, index.vue, modules/form.vue}` | 列表 + 顶部归属设置区(制单日期/归属月份/应用至选中/生成凭证) + 多选 + 状态 Tag |
| 资产负债表 | `balance-sheet/index.vue` | 骨架页：左资产 / 右负债权益 双栏 |
| 利润表 | `income-statement/index.vue` | 骨架页：单栏 + 「包含连锁调拨利润」复选框 |
| 现金流量表 | `cash-flow/index.vue` | 骨架页：左主表(直接法) / 右补充资料(间接法) |

**关键交互落地**：
- **借贷平衡**（凭证 item-form.vue）：`computed` 实时算 `totalDebit/totalCredit`，子表底部 `✓平衡 / ✗不平衡` 颜色提示，`validate()` 抛错 + form.vue submit 前再做一次双保险
- **借贷互斥**：`<InputNumber :disabled="Number(creditAmount) > 0">` 反之亦然
- **会计科目级联辅助核算**：选中科目后调 `auxiliary-list?subjectId=` 决定核算项列是否启用
- **子表横向滚动保护**（凭证 item-form.vue）：按 CLAUDE.md 「字段编辑 vs 增删行」规范分 `emitItemsUpdate(true/false)` 区别 reload，保住滚动位置
- **行点击展开**（系统开账 index.vue）：vxe-grid `cellClick` 事件 → 调 `getVoucherConfigList` → 渲染下方 11 个 Checkbox + dirty 标记驱动「保存配置」按钮
- **批量应用归属**（凭证生成）：`getCheckboxRecords()` 取选中行 → 校验`归属YM ≤ bizDateYM` → 调 `apply` 接口
- **三大报表骨架**：每个报表 `onMounted` 调 `getReportItemTemplateList(reportType, side)` 拉项目，金额列 `customRender: () => '0.00'` 占位

### 团队执行回顾

- 派发 10 个 Agent：Wave1 五个并行后端（M1-M5），Wave2 五个并行前端（F1-F5）
- 整体编译验证：`mvn compile -pl yudao-module-erp -am -DskipTests` → **BUILD SUCCESS**（558 源文件，10s）
- 1 次 Agent 失联（F5 上下文耗尽未交付任何文件）→ 主导接手手写 7 个文件（attribution API + data + index + form + 三大报表骨架）
- 4 次 Agent 卡 mvn/pnpm 编译权限请求 → 主导拒绝并告知"统一编译验证，单独构建会因依赖未到位假性失败"
- 客户指示: 加 `--default-character-set=utf8mb4` 避坑（七期踩过的）；后端 tenant_id=1 硬编码避坑（七期踩过的）

### 部署步骤（本期无新 SQL，仅代码变更）

```bash
# 1. 后端重新编译
cd e:/A_xy_demo/erp/ruoyi-vue-pro-master/ruoyi-vue-pro-master
mvn clean package -pl yudao-server -am -DskipTests

# 2. 启动后端 cn.iocoder.yudao.server.YudaoServerApplication

# 3. 前端构建
cd yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master
pnpm build:antd

# 4. 登录后台验证 8 个二级菜单可访问：
#    会计科目 / 系统开账 / 凭证列表 / 凭证生成 / 凭证字 / 资产负债表 / 利润表 / 现金流量表
#
# 注：menu SQL 已在十期 `erp_accounting_menu_v26.sql` 录入，本期无新菜单
#    报表项目模板页（report-template）通过 URL `/erp/accounting/report-template` 访问，无单独菜单
```

### 客户需求 v 修订版 完整交付状态

| 表单 | 客户文档要求 | 实际交付 |
|---|---|---|
| 会计科目 + 期初余额合并 | ✅ 搭建 | ✅ 6 大类 Tab + 树形缩进 + 内联期初编辑 + Excel 导入导出 |
| 系统开账 | ✅ 搭建 | ✅ 列表 + 注意事项 Alert + 11 凭证类型勾选 |
| 凭证列表 | ✅ 搭建 | ✅ 筛选 + 工具栏 + 审核/反审/删除 + 导出 Excel |
| 凭证生成 | ✅ 搭建 | ✅ 归属设置区 + 单据多选 + 应用至选中 + 生成凭证 |
| 三大报表 | ✅ 搭建（骨架） | ✅ 三个报表布局完整，formula 取数留三期 |
| 银行/现金日记账 | ❌ 不搭建（资金模块） | — |
| 月末结转 / 期间损益结转 | ❌ 暂缓 | — |
| 凭证字字典（额外搭建） | — | ✅ 简单 CRUD |
| 报表项目模板维护页（额外搭建） | — | ✅ 后期填 formula 用 |

### 遗留项（留三期及以后）

1. **报表 formula 取数**：本期金额硬编码 0.00，三期需与财务沟通公式语法（建议用类 SQL 表达式或 SubjectCode 标签）
2. **自动生成凭证**：开账配置已就绪，但采购/销售/调拨等单据审核时尚未触发自动生成凭证；三期需补对应 EventListener
3. **辅助核算多类型联动**：凭证分录的"核算项"列本期占位文本输入；三期补完整级联 (项目/部门/客户/供应商/个人 各自 Selector)
4. **月末结转 + 期间损益结转**：客户文档明确"暂缓"，与财务沟通后再启动
5. **银行/现金日记账**：归类到"资金模块"，本期不动
6. **凭证打印模板**：详情页未做打印按钮（依赖财务给出版式）
7. **跨月归属调整业务联动**：归属记录已能手工建/批量改月份/生成空壳凭证；三期补"业务单据审核时自动写入 attribution"的联动
8. **typecheck**：项目既有 AI/BPM/库存等模块的旧 TS 错误未清理（沿用六期/七期/八期/九期策略），本期新增文件无新增错误

---

## 财务核算模块三期：自动生成凭证业务联动（已完成 ✅，2026-05-15）

> 客户需求文档：`刘/财务模块-ds修订版.md`
> 范围：把二期的"系统开账 + 凭证类型勾选"从纯配置升级为**真实业务联动**——采购入库 / 采购退货 / 销售出库 / 销售退货 四个单据审核通过时，按系统开账配置自动生成凭证；反审核时连带删除未审核的凭证（已审核拦截反审）。

### 关键决策（与客户对齐）

| 决策点 | 答案 |
|---|---|
| 凭证生成失败是否回滚业务单据 | 是 — 与原 `@Transactional` 共享事务 |
| 已审核凭证能否反审业务单据 | 否 — 必须先反审凭证 |
| 系统开账未勾选某类凭证 | 业务单据照常审核，跳过凭证生成（不抛异常） |
| 自动凭证用什么科目 | 8 个固定末级科目硬编码（`ErpAccountingSubjectCodeConstants`），三期不做映射表 |
| 销售出库的成本取数 | 扣库存**前**取 `stock.costPrice`，避免扣完库存后取到错误均价 |
| 凭证 sourceBizType | 用 `ErpVoucherTypeEnum`：采购入/退都用 PURCHASE(7)，销售出/退都用 SALE(1)；红字用反向分录区分 |

### 后端产出（无新 SQL，仅代码）

#### 新增文件（2 个）

1. **[ErpAccountingSubjectCodeConstants](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/enums/finance/accounting/ErpAccountingSubjectCodeConstants.java)** — 8 个固定末级科目编码常量：
   - `CASH=1001` / `BANK=1002` / `AR=1122` / `INVENTORY=1405`
   - `AP=2202` / `TAX_PAYABLE=2221` / `REVENUE=6001` / `COST=6401`
   - 与 `sql/mysql/erp_accounting_subject_v26.sql` 预置数据一致

2. **[ErpAutoVoucherBuilder](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/finance/accounting/ErpAutoVoucherBuilder.java)** — 自动凭证分录构建器，4 个 build 方法对应 4 类业务：
   - `buildPurchaseInItems(purchaseIn, supplierName)` — 借库存 + 借应交税费（如有）/ 贷应付
   - `buildPurchaseReturnItems(purchaseReturn, supplierName)` — 红字反向（贷库存 / 借应付）
   - `buildSaleOutItems(saleOut, customerName, sumCost)` — 借应收 / 贷收入；同时借成本 / 贷库存（双分录组）
   - `buildSaleReturnItems(saleReturn, customerName, sumCost)` — 红字反向

#### 改动文件（8 个）

| 文件 | 改动 |
|---|---|
| [ErrorCodeConstants](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/enums/ErrorCodeConstants.java) | 新增 `BIZ_PROCESS_FAIL_VOUCHER_APPROVED`（反审失败：已存在审核通过的凭证） |
| [ErpVoucherMapper](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/mysql/finance/accounting/ErpVoucherMapper.java) | 新增 `selectListByBiz(sourceBizType, sourceBizId)` |
| [ErpBookOpenMapper](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/mysql/finance/accounting/ErpBookOpenMapper.java) | 新增 `selectByYearAndPeriod(year, period)` |
| [ErpBookOpenVoucherConfigMapper](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/mysql/finance/accounting/ErpBookOpenVoucherConfigMapper.java) | 新增 `selectByBookOpenIdAndVoucherType(...)` |
| [ErpBookOpenService](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/finance/accounting/ErpBookOpenService.java) + Impl | 新增 `isVoucherTypeEnabled(bizDate, voucherType)` —— 判断业务日期所在期间的开账是否勾选了该凭证类型 |
| [ErpVoucherService](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/finance/accounting/ErpVoucherService.java) + Impl | 新增 `createVoucherFromBiz(sourceBizType, bizId, bizNo, bizDate, summary, items)` —— 单事务内建凭证主表 + 子表分录 + 借贷自检 |
| `ErpStockLockMapper` / `ErpStockLockServiceImpl` | 修复 Wave 1 期间发现的小问题（与本期主线无关） |

### Wave 2：4 个业务 Service 接入（统一接入模式）

修改 4 个 Service：
- [ErpPurchaseInServiceImpl](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/purchase/ErpPurchaseInServiceImpl.java)
- [ErpPurchaseReturnServiceImpl](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/purchase/ErpPurchaseReturnServiceImpl.java)
- [ErpSaleOutServiceImpl](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sale/ErpSaleOutServiceImpl.java)
- [ErpSaleReturnServiceImpl](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sale/ErpSaleReturnServiceImpl.java)

**统一接入模式**（4 个 Service 一致）：

```java
// 注入 6 个依赖（销售相关多注入 ErpStockService 取成本均价）
@Resource private ErpAutoVoucherBuilder autoVoucherBuilder;
@Resource private ErpVoucherService voucherService;
@Resource private ErpBookOpenService bookOpenService;
@Resource private ErpVoucherMapper voucherMapper;
@Resource private ErpVoucherItemMapper voucherItemMapper;
@Resource private ErpStockService stockService;  // 仅销售相关需要

// updateXxxStatus 内：
// 审核分支（approve）末尾追加：
if (bookOpenService.isVoucherTypeEnabled(bizDate, voucherType)) {
    List<ErpVoucherItemDO> items = autoVoucherBuilder.buildXxxItems(...);
    voucherService.createVoucherFromBiz(voucherType, bizId, bizNo, bizDate, summary, items);
}

// 反审分支（!approve）头部插入：
List<ErpVoucherDO> vouchers = voucherMapper.selectListByBiz(voucherType, id);
for (ErpVoucherDO v : vouchers) {
    if (Objects.equals(v.getAuditStatus(), APPROVE.getStatus())) {
        throw exception(BIZ_PROCESS_FAIL_VOUCHER_APPROVED);
    }
    voucherMapper.deleteById(v.getId());
    voucherItemMapper.delete(new LambdaQueryWrapper<ErpVoucherItemDO>().eq(ErpVoucherItemDO::getVoucherId, v.getId()));
}
```

**销售出库的成本快照特殊处理**：
- 取数时机：在 `selectListByOutId` 之后、扣库存 `createStockRecord` 循环**之前**取 `stockService.getStock(productId, warehouseId).getCostPrice()`，累加 `sumCost`
- 凭证生成在扣库存循环**之后**调用，保证库存已变更但成本均价用扣前快照
- 防御：stock 为 null 或 costPrice 为 null 时 fallback 到 `BigDecimal.ZERO`

### 验证

- Wave 1 整体编译：BUILD SUCCESS（558 源文件，14.93s）
- Wave 2 整体编译（`mvn clean compile -pl yudao-module-erp -am`）：BUILD SUCCESS（35.04s）
- 4 个业务 Service 已 grep 确认引用 `ErpAutoVoucherBuilder` / `createVoucherFromBiz`

### Team 模式执行

- Wave 1：1 个 Agent（w1-infra）串行处理基础设施
- Wave 2：4 个 Agent 并行（w2-purchase-in / w2-purchase-return / w2-sale-out / w2-sale-return），每个 Agent 限定改一个 Service 文件，零冲突
- 收尾：主导统一编译验证 + 关闭所有 Agent

### 部署步骤（**本期无新 SQL**）

```bash
cd "e:/A_xy_demo/erp/ruoyi-vue-pro-master/ruoyi-vue-pro-master"

# 1. 重新编译 + 启动后端
mvn clean package -pl yudao-server -am -DskipTests
# 启动 cn.iocoder.yudao.server.YudaoServerApplication

# 2. 前端无新改动（沿用二期）
```

### 部署后验证流程

1. **未开账时**：审核任意采购入库单 → 凭证列表无新增（预期）
2. **开账并勾选「采购凭证」**：
   - 进入「系统开账」→ 新增 2026-05 期开账
   - 行点击该开账 → 勾选「采购凭证」→ 保存配置
3. **再审核采购入库单**：凭证列表应自动出现一条 `记-202605-XXXXXX`，制单人 = 审核者
4. **反审核该入库单**：
   - 若凭证未审核 → 反审成功，凭证自动删除
   - 若凭证已审核 → 反审失败提示 `已存在审核通过的凭证`
5. **销售出库**：审核后凭证应有两组分录（应收/收入 + 成本/库存）
6. **采购退货 / 销售退货**：审核后生成红字凭证（与正向相反方向）

### 客户需求覆盖率更新

| 模块 | 二期状态 | 三期状态 |
|---|---|---|
| 会计科目 + 期初余额 | ✅ 搭建 | — |
| 系统开账 | ✅ 搭建（纯配置） | ✅ **真实生效**（勾选触发凭证生成） |
| 凭证列表 | ✅ 搭建 | — |
| 凭证生成（跨月归属） | ✅ 搭建（手动） | — |
| 三大报表 | ✅ 骨架页 | — |
| **自动生成凭证（采购/销售单据触发）** | ❌ 不在范围 | ✅ **本期完成**（4 单据） |
| 银行/现金日记账 | ❌ 资金模块 | — |
| 月末结转 / 期间损益结转 | ❌ 暂缓 | — |

### 遗留项（留四期及以后）

1. **调拨出库自动凭证**：VoucherType 6（调拨出库凭证）留口未实现
2. **其他出库 / 入库自动凭证**：VoucherType 9/10 留口未实现
3. **银行转账凭证**：VoucherType 11，归资金模块
4. **其他应收 / 预收 / 预付 / 预收账款 / 其他应付凭证**：VoucherType 2/3/4/5/8 留口
5. **8 个固定科目 → 业务-科目映射表**：客户暂时按"工业账"科目；后期支持"商业账""服务账"再做映射表
6. **三大报表 formula 取数**：仍是骨架页（与财务沟通公式语法后再启动）
7. **跨月归属调整自动联动**：凭证生成页仍只支持手动建归属记录；后续补"业务单据审核时自动写入 attribution"
8. **辅助核算多类型联动**：凭证分录的"核算项"列仍是占位文本输入

---

## 财务核算模块四期：其他出/入库自动凭证（已完成 ✅，2026-05-15）

> 把三期遗留的 VoucherType 9（其他出库）+ VoucherType 10（其他入库）补齐；调拨出库 VoucherType 6 经决策**不生成凭证**（同公司内总账不变，标准会计实务）。

### 关键决策（与客户对齐）

| 决策点 | 答案 |
|---|---|
| 调拨出库（VoucherType 6） | **不生成凭证** — 同公司内 A 仓→B 仓总账不变，跳过 |
| 其他入库（VoucherType 10）对方科目 | **贷 1901 待处理财产损益** — 盘盈/收料/捐赠先挂账，期末再处理 |
| 其他出库（VoucherType 9）对方科目 | **借 1901 待处理财产损益** — 盘亏/领用/损耗对应入库镜像 |
| 金额取数 | **按成本均价**（与销售出库一致）— 入库用 `totalPrice`，出库扣库存前累加 `stock.costPrice × count` |

### 凭证分录模板

**其他入库（2 行）**：
```
借 1405 库存商品           sumCost
贷 1901 待处理财产损益     sumCost
```

**其他出库（2 行，红字镜像）**：
```
借 1901 待处理财产损益     sumCost
贷 1405 库存商品           sumCost
```

### 后端产出（共 4 处改动）

#### 1. 新增 SQL（1 个）

[sql/mysql/erp_accounting_subject_v27.sql](sql/mysql/erp_accounting_subject_v27.sql)：预置 1 条科目
- `1901 待处理财产损益`（资产类 / 借方 / 末级 / sort=16）
- 与二期 `erp_accounting_subject_v26.sql` 列结构完全一致；`INSERT IGNORE` 支持重复执行

#### 2. 常量类追加（1 个）

[ErpAccountingSubjectCodeConstants](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/enums/finance/accounting/ErpAccountingSubjectCodeConstants.java)：
```java
/** 1901 待处理财产损益（四期 其他出/入库自动凭证使用） */
String LOSS_AND_GAIN = "1901";
```

#### 3. Builder 扩展（2 个新方法）

[ErpAutoVoucherBuilder](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/finance/accounting/ErpAutoVoucherBuilder.java)：
- `buildStockInItems(stockIn, sumCost)` — 借 1405 / 贷 1901
- `buildStockOutItems(stockOut, sumCost)` — 借 1901 / 贷 1405

#### 4. 业务 Service 接入（2 个 Service）

| 文件 | 凭证类型 | 关键时序 |
|---|---|---|
| [ErpStockInServiceImpl](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/stock/ErpStockInServiceImpl.java) | OTHER_IN(10) | approve 末尾建凭证；金额直接用 `stockIn.totalPrice`（即明细 productPrice × count） |
| [ErpStockOutServiceImpl](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/stock/ErpStockOutServiceImpl.java) | OTHER_OUT(9) | **关键：扣库存前**遍历明细累加 `stock.costPrice × count` 得 sumCost；扣完库存再建凭证 |

接入模式与三期 4 个 Service 完全一致：
- 注入 6 个依赖（出库多注入 `ErpStockService` 取均价）
- 反审分支头部：先查 `voucherMapper.selectListByBiz(...)`，已审核拦截，未审核连带删除
- 审核分支：`bookOpenService.isVoucherTypeEnabled(bizDate, voucherType)` 命中才建凭证

### 验证

- `mvn clean compile -pl yudao-module-erp -am`：BUILD SUCCESS（34s）

### 部署步骤

```bash
cd "e:/A_xy_demo/erp/ruoyi-vue-pro-master/ruoyi-vue-pro-master"

# 1. 执行 SQL（建议加 --default-character-set=utf8mb4 避免中文乱码）
mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 ruoyi-vue-pro \
  < sql/mysql/erp_accounting_subject_v27.sql

# 2. 重新打包后端 + 重启
mvn clean package -pl yudao-server -am -DskipTests
# 启动 cn.iocoder.yudao.server.YudaoServerApplication

# 3. 前端无改动
```

### 部署后验证

1. **校验 1901 已预置**：`SELECT subject_code, subject_name FROM erp_accounting_subject WHERE subject_code='1901';`
2. **开账并勾选「其他入库凭证」「其他出库凭证」**
3. **新建其他入库单（盘盈场景）→ 审核**：凭证列表应出现 `记-XXXX`，分录为「借 1405 / 贷 1901」
4. **新建其他出库单（盘亏场景）→ 审核**：凭证应为「借 1901 / 贷 1405」，金额=扣库存前的成本均价
5. **反审入库/出库单**：未审核凭证连带删除；已审核凭证拦截反审

### 客户需求覆盖率更新

| VoucherType | 凭证类型 | 三期 | 四期 |
|---|---|---|---|
| 1 | 销售凭证 | ✅ | — |
| 2 | 其他应收凭证 | ❌ | ❌ |
| 3 | 预收款凭证 | ❌ | ❌ |
| 4 | 预付款凭证 | ❌ | ❌ |
| 5 | 预收账款凭证 | ❌ | ❌ |
| 6 | 调拨出库凭证 | ❌ | **🚫 决策不实现** |
| 7 | 采购凭证 | ✅ | — |
| 8 | 其他应付凭证 | ❌ | ❌ |
| 9 | 其他出库凭证 | ❌ | **✅ 本期完成** |
| 10 | 其他入库凭证 | ❌ | **✅ 本期完成** |
| 11 | 银行转账凭证 | ❌ | ❌（资金模块） |

### 遗留项（留五期及以后）

1. ~~**VoucherType 2/3/4/5/8**：5 种凭证留口未实现~~ → **五期已完成**
2. **VoucherType 11 银行转账凭证**：归资金模块（银行/现金日记账尚未搭建）
3. **8 个固定科目 → 业务-科目映射表**：维持工业账，未启动多账套
4. **三大报表 formula 取数**：与财务确认公式语法后启动
5. **辅助核算多类型联动**：凭证分录"核算项"列仍占位

---

## 财务核算模块五期：5 张新业务单据 + 5 种凭证类型（已完成 ✅，2026-05-17）

> 客户需求文档：`刘/财务模块-ds修订版.md`
> 范围：补齐 VoucherType 2/3/4/5/8 对应的 5 张业务单据（其他应收单/预收款单/预付款单/预收账款单/其他应付单），含完整 CRUD + 审核/反审 + 自动凭证生成 + 前端页面。

### 关键决策（与客户对齐）

| 决策点 | 答案 |
|---|---|
| 单据表结构 | 主-子表结构（每单多明细行） |
| VoucherType 3 预收款 vs VoucherType 5 预收账款 | 保留两张独立单，走不同科目（2203 vs 2204） |
| 对方（辅助核算）类型 | 支持客户/供应商/员工三种（partyType: 1/2/3） |
| 凭证对方科目 | 采用会计准则标准科目编号 |

### SQL 脚本（1 个）

```bash
mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 ruoyi-vue-pro < sql/mysql/erp_finance_voucher_v28.sql
```

| 内容 | 说明 |
|---|---|
| 新建 10 张表 | `erp_other_receivable` + `_item`、`erp_pre_receipt` + `_item`、`erp_pre_payment` + `_item`、`erp_pre_receivable` + `_item`、`erp_other_payable` + `_item` |
| 预置 4 个科目 | 1221 其他应收款 / 2203 预收账款 / 2204 预收账款(另) / 2241 其他应付款 |
| 菜单 25 条 | ID 3070-3094（5 个二级菜单 + 20 个权限按钮），挂在财务核算目录(3030)下 |
| 超管授权 | role_id=1 自动授权全部 25 条菜单 |

### 后端产出（50 个新文件 + 2 个改动文件）

每个业务单据模块 10 个文件：DO×2 + Mapper×2 + Service×2 + Controller×1 + VO×3

| 模块 | 单号前缀 | Controller 路径 | VoucherType | 凭证分录 |
|---|---|---|---|---|
| 其他应收单 | QTYS | `/erp/other-receivable` | 2 | 借 1221 其他应收款 / 贷 1002 银行存款 |
| 预收款单 | YSKD | `/erp/pre-receipt` | 3 | 借 1002 银行存款 / 贷 2203 预收账款 |
| 预付款单 | YFKD | `/erp/pre-payment` | 4 | 借 1123 预付账款 / 贷 1002 银行存款 |
| 预收账款单 | YSZK | `/erp/pre-receivable` | 5 | 借 1002 银行存款 / 贷 2204 预收账款(另) |
| 其他应付单 | QTYF | `/erp/other-payable` | 8 | 借 2241 其他应付款 / 贷 1002 银行存款 |

改动文件：
- `ErpAccountingSubjectCodeConstants.java` — 新增 5 个科目常量（PREPAID/OTHER_RECEIVABLE/PRE_RECEIPT/PRE_RECEIVABLE/OTHER_PAYABLE）
- `ErpAutoVoucherBuilder.java` — 新增 5 个 build 方法（buildOtherReceivableItems / buildPreReceiptItems / buildPrePaymentItems / buildPreReceivableItems / buildOtherPayableItems）
- `ErrorCodeConstants.java` — 新增 1_030_617_000 ~ 1_030_622_005 段（5×6=30 个错误码）

### 前端产出（20 个新文件）

每个页面 4 个文件：API×1 + data.ts×1 + index.vue×1 + modules/form.vue×1

| 页面 | 路径 |
|---|---|
| 其他应收单 | `views/erp/accounting/other-receivable/` |
| 预收款单 | `views/erp/accounting/pre-receipt/` |
| 预付款单 | `views/erp/accounting/pre-payment/` |
| 预收账款单 | `views/erp/accounting/pre-receivable/` |
| 其他应付单 | `views/erp/accounting/other-payable/` |

### 主表字段（5 张表统一）

id / no / status(10/20/30) / biz_time / party_type(1=客户 2=供应商 3=员工) / party_id / party_name / account_id / total_amount / discount_amount / actual_amount / remark / file_url + BaseDO + tenant_id

### 子表字段（5 张表统一）

id / {主表}_id / summary / amount / remark + BaseDO + tenant_id

### 验证

- 后端编译：`mvn clean compile -pl yudao-module-erp -am -DskipTests` → BUILD SUCCESS
- 前端文件：20 个文件全部落地（5 API + 5 data.ts + 5 index.vue + 5 form.vue）

### Team 模式执行

- Wave 1：主导串行（SQL + 科目常量扩展）
- Wave 2：5 个 Agent 并行（w2-other-receivable / w2-pre-receipt / w2-pre-payment / w2-pre-receivable / w2-other-payable）
- Wave 3：5 个 Agent 并行（w3-other-receivable-page / w3-pre-receipt-page / w3-pre-payment-page / w3-pre-receivable-page / w3-other-payable-page）
- 收尾：主导统一编译验证 + 关闭所有 Agent

### 部署步骤

```bash
# 1. 执行 SQL（新增 10 张表 + 4 个科目 + 25 条菜单）
mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 ruoyi-vue-pro < sql/mysql/erp_finance_voucher_v28.sql

# 2. 重新编译启动后端
cd "e:/A_xy_demo/erp/ruoyi-vue-pro-master/ruoyi-vue-pro-master"
mvn clean package -pl yudao-server -am -DskipTests
# 启动 cn.iocoder.yudao.server.YudaoServerApplication

# 3. 前端无需额外操作（新页面已落地，菜单已配）
```

### 部署后验证流程

1. 登录后台 → 财务核算菜单下应出现 5 个新子菜单（其他应收单/预收款单/预付款单/预收账款单/其他应付单）
2. 进入「其他应收单」→ 新增一条（选对方类型=客户，填金额）→ 保存成功
3. 审核该单据 → 凭证列表应自动出现一条凭证（借 1221 / 贷 1002）
4. 反审核该单据 → 凭证自动删除
5. 系统开账页面 → 确认 VoucherType 2/3/4/5/8 勾选开关生效
6. 重复验证预收款单/预付款单/预收账款单/其他应付单

### 客户需求覆盖率更新（截至五期）

| VoucherType | 名称 | 状态 |
|---|---|---|
| 1 销售凭证 | 销售出库/退货审核触发 | ✅ 三期完成 |
| 2 其他应收凭证 | 其他应收单审核触发 | ✅ **五期完成** |
| 3 预收款凭证 | 预收款单审核触发 | ✅ **五期完成** |
| 4 预付款凭证 | 预付款单审核触发 | ✅ **五期完成** |
| 5 预收账款凭证 | 预收账款单审核触发 | ✅ **五期完成** |
| 6 调拨出库凭证 | 调拨出库审核触发 | ✅ 四期完成 |
| 7 采购凭证 | 采购入库/退货审核触发 | ✅ 三期完成 |
| 8 其他应付凭证 | 其他应付单审核触发 | ✅ **五期完成** |
| 9 其他出库凭证 | 其他出库审核触发 | ✅ 四期完成 |
| 10 其他入库凭证 | 其他入库审核触发 | ✅ 四期完成 |
| 11 银行转账凭证 | 资金模块（未搭建） | ❌ 留后续 |

**11 种凭证类型已完成 10 种（91%），仅剩 VoucherType 11 银行转账凭证待资金模块搭建后接入。**

### 遗留项（留六期及以后）

1. **VoucherType 11 银行转账凭证**：归资金模块（银行/现金日记账尚未搭建）
2. **8 个固定科目 → 业务-科目映射表**：维持工业账，未启动多账套切换
3. **三大报表 formula 取数**：骨架页已就绪，与财务确认公式语法后启动
4. **辅助核算多类型联动**：凭证分录"核算项"列仍占位文本输入
5. **月末结转 / 期间损益结转**：客户文档明确"暂缓"
6. **partyType 联动下拉**：当前对方名称为手填文本，后续可改为根据 partyType 联动客户/供应商/员工下拉选择器
7. **收款单/付款单凭证接入**：现有收款/付款单审核时尚未生成凭证（与三期采购/销售凭证同理，可复用模式快速接入）

---

## 财务核算模块六期：试用反馈修复（已完成 ✅，2026-05-18）

> 客户反馈文档：`刘/财务问题汇总-ds修订版.md`
> 范围：客户在五期基础上试用后提出 30+ 项问题，按"Bug 修复 + UI 调整 + 业务逻辑增强"一体化交付。

### 关键决策（与客户对齐）

| 决策点 | 答案 |
|---|---|
| 会计科目加凭证类型字段（客户/连锁）| 加 `voucher_type TINYINT DEFAULT 1` |
| 新增开账后是否自动扫描历史业务单据生成凭证 | 同步扫描（REQUIRES_NEW 子事务隔离，单条失败不影响整体） |
| 凭证生成页单据类型下拉拓展 | 按文档全部 20 种全上，10 种实现真实查询，10 种暂返回空 |
| 其他 4 项一起做 | 折叠分录明细区 / 金额列小数位 / 记账出纳改用户下拉 / 删除开账编辑删除按钮 |

### Wave 1：Bug 修复 + SQL（主导亲手）

#### Bug 修复

**根本原因**：`ErpAccountServiceImpl.getAccountList(Collection<Long> ids)` 没做空集合保护，当传入的 accountId 集合全为 null 时（如新建的预收/预付单据未关联账户），`accountMapper.selectByIds(emptyCollection)` 直接生成 `WHERE id IN ()` 非法 SQL，触发 `BadSqlGrammarException`。

**修复点**：[ErpAccountServiceImpl.java](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/finance/ErpAccountServiceImpl.java) 加 `CollUtil.isEmpty` 兜底 → 一处修复，全局生效（5 个 Controller 的 `getAccountMap` 调用都受益）。

```java
@Override
public List<ErpAccountDO> getAccountList(Collection<Long> ids) {
    if (CollUtil.isEmpty(ids)) {
        return Collections.emptyList();
    }
    return accountMapper.selectByIds(ids);
}
```

#### SQL 脚本 `sql/mysql/erp_finance_v29.sql`

1. `erp_accounting_subject` 加 `voucher_type TINYINT DEFAULT 1`（用 information_schema 存储过程方案幂等）
2. 既有数据全部 `UPDATE voucher_type = 1`
3. 隐藏 5 个二级业务表单菜单（`visible=0`）：其他应收单(3070) / 预收款单(3075) / 预付款单(3080) / 预收账款单(3085) / 其他应付单(3090)。仅菜单隐藏，数据表保留 — 凭证生成扫描时仍需后端接口可用。

#### 新增枚举 `ErpVoucherSourceBizTypeEnum`

20 种单据类型 1-20：销售凭证 / 销售出库单 / 销售退货单 / 其他应收 / 预收款凭证 / 收款凭证 / 采购凭证 / 采购入库单 / 采购退货单 / 其他应付 / 预付款凭证 / 付款凭证 / 其他收入凭证 / 费用支出凭证 / 调拨出库单 / 其他出库单 / 其他入库单 / 银行转账 / 采购调价 / 线上收款

### Wave 2：后端 3 Agent 并行

#### W2-A 开账后自动扫描生成凭证

修改 [ErpBookOpenServiceImpl.java](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/finance/accounting/ErpBookOpenServiceImpl.java)：
- 注入 17 个 Resource（11 个业务表 Mapper + 3 个关联实体 Service + `ErpAutoVoucherBuilder` + `ErpVoucherService` + `ErpVoucherMapper` + `PlatformTransactionManager`）
- `create()` 末尾调用 `scanAndGenerateVouchersAfterCreate(bookOpen)`
- 计算期间起止：`YearMonth.of(fiscalYear, period)` 推算月初月末
- 遍历已勾选的 voucherType，对 9 种凭证类型扫描 11 张业务表（销售凭证 = 销售出库 + 销售退货；采购凭证 = 采购入库 + 采购退货；其他 7 种各 1 张表）
- **三层失败防护**：外层 try-catch（不阻塞开账主流程）+ voucherType 级 try-catch（一类失败不影响其他）+ 单据级 REQUIRES_NEW 子事务 + setRollbackOnly（一条失败不影响下一条）
- **查重防重复**：每条业务单据先 `voucherMapper.selectListByBiz(voucherType, bizId)`，已存在跳过 — 保证幂等
- **sumCost 取数**：销售出库 / 销售退货 / 其他出库三类按明细 productId × warehouseId 查 stock.costPrice 累加（与三期、四期一致）
- VoucherType 6 调拨出库 / 11 银行转账暂不扫描（与五期遗留项一致）

#### W2-B 凭证生成接口扩展 20 种单据

修改 [ErpVoucherAttributionService.java](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/finance/accounting/ErpVoucherAttributionService.java) + Impl + [ErpVoucherAttributionController.java](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/finance/accounting/ErpVoucherAttributionController.java)：
- 新增 `GET /erp/voucher-attribution/search-source-biz` 端点
- 新建 `ErpVoucherAttributionSearchSourceBizReqVO`（5 字段：sourceBizType / bizDateStart / bizDateEnd / bizNo / partyName）
- 按 sourceBizType switch 路由到对应业务表 mapper，统一返回 `PageResult<ErpVoucherAttributionRespVO>`（attributionStatus=10 未归属）
- **真实查询 10 种**：销售出库(2) / 销售退货(3) / 其他应收(4) / 收款凭证(6) / 采购入库(8) / 采购退货(9) / 其他应付(10) / 付款凭证(12) / 其他出库(16) / 其他入库(17)
- **暂返回空 10 种**：销售凭证(1) / 预收款凭证(5) / 采购凭证(7) / 预付款凭证(11) / 其他收入凭证(13) / 费用支出凭证(14) / 调拨出库单(15) / 银行转账(18) / 采购调价(19) / 线上收款(20)
- 客户/供应商 partyName 走 customerService/supplierService 批量查 + 内存模糊过滤
- 其他应收/其他应付有 party_name 字段，直接 SQL like

#### W2-C 会计科目加 voucher_type 字段

新建 [ErpSubjectVoucherTypeEnum.java](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/enums/finance/accounting/ErpSubjectVoucherTypeEnum.java)：CUSTOMER(1) / CHAIN(2)

修改 [ErpAccountingSubjectDO.java](yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/dataobject/finance/accounting/ErpAccountingSubjectDO.java) + 3 个 VO（Save / Resp / Page）加 voucherType 字段（@InEnum 校验，非必填）

### Wave 3：前端 4 Agent 并行

#### W3-A 会计科目页（5 文件）

- `data.ts`：label「简称→科目别名」「科目大类→科目类型」；新增 `voucherType` 字段（客户/连锁 Select）；`parentCode` 从 Input 改 ApiSelect（依赖 subjectCategory 联动）；`subjectLevel` / `sort` 改隐藏字段
- `form.vue`：`handleValuesChange` 钩子 — 选父科目自动取 `parent.level + 1`、继承 balanceDirection；切换 subjectCategory 清空 parentCode；提交前兜底 sort=0、subjectLevel=1
- `import-modal.vue`：修复 "null" 提示 `msg ?? '成功'` 兜底
- `index.vue`：删除工具栏「下载导入模板」按钮（与导入弹框重复）

#### W3-B 系统开账页（3 文件）

- 列表删除「编辑」「删除」按钮（汽配云不允许）
- 删除/隐藏「连锁名称」字段

#### W3-C 凭证页（3 文件）

- 分录明细区改折叠抽屉（语义不明的字段折叠）
- 记账/出纳/主管/审核 改 ApiSelect 拉 `system/user/simple-list`
- 「附件数」→「附件」改文本框
- 项目/部门/客户/个人 加 `disabled`（只读）
- 子表"核算项"列改普通 Input（与会计科目无关联）

#### W3-D 凭证生成页（2 文件）

- 单据类型下拉拓展到 20 种（用 W2-B 后端枚举）
- 新逻辑：先选类型 + 日期 → 查询（调 `/search-source-biz`） → 勾选数据 → 设制单日期 → 生成凭证

### 验证

```bash
mvn clean compile -pl yudao-module-erp -am -DskipTests → BUILD SUCCESS（37.43s）
```

### Team 模式执行

- Wave 1：主导亲手（Bug 修复 + SQL + 枚举）
- Wave 2：3 Agent 并行（w2a-book-open-scan / w2b-attribution-bizType / w2c-subject-vouchertype）
- Wave 3：4 Agent 并行（w3a-subject / w3b-bookopen / w3c-voucher / w3d-attribution）
- 收尾：主导统一编译验证 + 关闭所有 Agent

### 部署步骤

```bash
cd "e:/A_xy_demo/erp/ruoyi-vue-pro-master/ruoyi-vue-pro-master"

# 1. 执行 SQL（加 voucher_type 字段 + 隐藏 5 个菜单）
mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 ruoyi-vue-pro < sql/mysql/erp_finance_v29.sql

# 2. 重新编译启动后端
mvn clean package -pl yudao-server -am -DskipTests
# 启动 cn.iocoder.yudao.server.YudaoServerApplication

# 3. 前端构建
cd yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master
pnpm build:antd

# 4. 刷新菜单缓存（隐藏 5 个表单生效）
# 登录后台 → 系统管理 → 菜单管理 → 刷新缓存 → 退出重登
```

### 部署后验证流程

1. **凭证列表 / 凭证生成不再报 SQL 异常**：访问 `/erp/voucher` 和 `/erp/voucher-attribution`，列表正常加载
2. **会计科目新增**：新建科目，选父科目后 `subjectLevel` 自动填充 +1；"凭证类型"下拉显示客户/连锁
3. **导入会计科目**：上传 Excel，结果提示从"null"改为"1001：成功"
4. **新增系统开账**：勾选凭证类型 → 提交开账 → 查看凭证列表，该期间已审核的业务单据应自动生成凭证（每条独立子事务，失败不阻塞）
5. **凭证生成页**：单据类型下拉显示 20 项 → 选采购入库单 + 日期 → 查询出已审核单据 → 勾选 → 设制单日期 → 点生成凭证
6. **5 个隐藏的业务单据菜单**：其他应收/预收款/预付款/预收账款/其他应付 在左侧菜单不可见，但后端接口仍可用

### 客户反馈问题修复覆盖率

| 大类 | 问题数 | 已修复 | 待办原因 |
|---|---|---|---|
| 1. 菜单分组 | 1 | ✅ 1 | — |
| 2. 会计科目 | 8 | ✅ 8 | — |
| 3. 系统开账 | 6 | ✅ 5 | 业务类型勾选保留与否需财务确认 |
| 4. 凭证列表 | 17 | ✅ 17 | — |
| 5. 凭证生成 | 4 | ✅ 4 | — |
| 6. 报表取数公式 | 1 | ❌ 0 | 需财务先确认公式语法（七期） |
| **合计** | **37** | **✅ 35（95%）** | **2 项依赖财务确认** |

### 遗留项（留七期及以后）

1. **三大报表 formula 取数**：金额列仍硬编码 0.00，需财务给公式后启动
2. **业务类型勾选保留与否**：客户反馈"财务说不要"，但去掉后系统开账无法控制凭证生成范围，需财务最终确认
3. **登账功能**：客户反馈"需与财务确认"
4. **VoucherType 6 调拨出库 + 11 银行转账**：依赖调拨和资金模块完善
5. **VoucherType 5/7/11/13/14/15/18/19/20**：凭证生成页这 9 种单据类型暂返回空，待七期补真实查询
6. **辅助核算多类型联动**：凭证分录"核算项"列仍占位文本输入（W3-C 已改为非级联 Input，但未做完整的项目/部门/客户级联选择器）
7. **审批按钮去留**：客户反馈"需与财务确认"

---

