# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

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

**所有 SQL 迁移脚本（7 个，需按顺序执行）**：
1. `sql/mysql/erp_stock_cost_v1.sql`
2. `sql/mysql/erp_purchase_in_v2.sql`
3. `sql/mysql/erp_purchase_items_v2.sql`
4. `sql/mysql/erp_purchase_return_v3.sql`
5. `sql/mysql/erp_price_system_v4.sql`
6. `sql/mysql/erp_menu_v4_v5.sql` — 菜单注册（价格体系 + 升级流水账菜单名 + 授权超管 role_id=1）
7. `sql/mysql/erp_purchase_order_gift_v6.sql` — 采购订单赠品字段

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
