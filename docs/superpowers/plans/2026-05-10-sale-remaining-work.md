# 销售管理剩余工作 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐销售管理模块当前未完成项，让销售模块从“主链路可跑”推进到“可部署验收”的状态。

**Architecture:** 继续采用渐进改造策略，不删除旧销售订单、旧销售出库、旧销售退货能力。优先补齐权限/菜单一致性、销售调价前端、客户批量能力、配置联动、打印入口和部署脚本可靠性，避免重写已跑通的销售主链路。

**Tech Stack:** Spring Boot 2.7.18、JDK 8、MyBatis Plus、MapStruct、EasyExcel、Vben Admin、Vue 3、Ant Design Vue、vxe-table。

---

## 当前已完成基线

- 客户基础档案、多 Tab 后端和前端主体已完成。
- 报价订单、销售手推车、销售单生成、库存扣减主链路已完成。
- 报价订单与销售手推车互转已完成。
- 销售退货双模式已完成。
- 销售调价后端“旧单调价追溯 + 新销售单生成 + 不重复扣库存”已完成。
- 销售配置表、销售配置页、销售菜单基础 SQL 已完成。
- 报价订单、销售手推车、销售订单、销售单、销售退货导出已完成。
- 销售子表横向滚动保护已覆盖报价订单、销售手推车、销售退货、旧销售订单、销售单。

---

## 剩余工作总览

| 优先级 | 模块 | 状态 | 目标 |
|---|---|---|---|
| P0 | 销售手推车删除 | 未完成 | 补齐后端删除接口、前端删除按钮、权限一致性 |
| P0 | 菜单 SQL 幂等化 | 未完成 | 避免重复执行 `erp_sale_menu_v14.sql` 主键冲突 |
| P1 | 销售调价前端完整化 | 部分完成 | 前端按“选择原销售单、改价格、审批生成新单”呈现 |
| P1 | 客户批量编辑/导入/导出升级 | 部分完成 | 客户导出已有，需补导入、批量编辑、新字段覆盖 |
| P1 | 销售配置深度联动 | 未完成 | 配置表接入客户合同、客户任务、物流/线路等下拉 |
| P2 | 打印入口预留 | 未完成 | 各销售单据列表/详情页统一放打印入口，占位提示 |
| P2 | 工商信息真实能力 | 占位 | 继续保留占位，不接第三方；补接口/页面说明 |
| P2 | 全量前端 typecheck 清理 | 非销售阻塞 | 另开专项处理 AI/BPM/库存等既有类型错误 |
| P3 | 真实环境业务验收 | 未完成 | 启动前后端，按主路径点验 |

---

## Task 1: 销售手推车删除能力

**Files:**
- Modify: `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/sale/ErpSaleCartController.java`
- Verify existing: `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sale/ErpSaleCartService.java`
- Verify existing: `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sale/ErpSaleCartServiceImpl.java`
- Modify: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/api/erp/sale/cart/index.ts`
- Modify: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/sale/cart/index.vue`
- Test: `yudao-module-erp/src/test/java/cn/iocoder/yudao/module/erp/service/sale/ErpSaleCartServiceImplTest.java`

- [ ] **Step 1: 检查 Service 是否已有删除方法**

Run:

```bash
Select-String -Path yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sale/ErpSaleCartService*.java -Pattern "deleteSaleCart"
```

Expected: Service 和 Impl 已存在 `deleteSaleCart(List<Long> ids)`；如果不存在，则在 Service/Impl 中按报价订单删除模式补齐。

- [ ] **Step 2: 写删除状态约束测试**

在 `ErpSaleCartServiceImplTest.java` 增加测试：草稿可删除，已终审不可删除。

```java
@Test
public void testDeleteSaleCart_success_whenDraft() {
    ErpSaleCartDO cart = randomPojo(ErpSaleCartDO.class,
            o -> o.setId(1L).setStatus(ErpSaleCartStatusEnum.DRAFT.getStatus()));
    when(saleCartMapper.selectBatchIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(cart));

    saleCartService.deleteSaleCart(Collections.singletonList(1L));

    verify(saleCartMapper).deleteBatchIds(Collections.singletonList(1L));
}

@Test
public void testDeleteSaleCart_fail_whenFinalApproved() {
    ErpSaleCartDO cart = randomPojo(ErpSaleCartDO.class,
            o -> o.setId(1L).setNo("SCT0001").setStatus(ErpSaleCartStatusEnum.FINAL_APPROVED.getStatus()));
    when(saleCartMapper.selectBatchIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(cart));

    assertServiceException(() -> saleCartService.deleteSaleCart(Collections.singletonList(1L)),
            SALE_CART_DELETE_FAIL_FINAL_APPROVED);
}
```

- [ ] **Step 3: 跑测试确认失败**

Run:

```bash
mvn test -pl yudao-module-erp "-Dtest=ErpSaleCartServiceImplTest#testDeleteSaleCart_success_whenDraft,ErpSaleCartServiceImplTest#testDeleteSaleCart_fail_whenFinalApproved"
```

Expected: 如果错误码/删除约束未实现，应失败。

- [ ] **Step 4: 实现 Controller 删除接口**

在 `ErpSaleCartController.java` 增加：

```java
@DeleteMapping("/delete")
@Operation(summary = "删除销售手推车")
@Parameter(name = "ids", description = "编号数组", required = true)
@PreAuthorize("@ss.hasPermission('erp:sale-cart:delete')")
public CommonResult<Boolean> deleteSaleCart(@RequestParam("ids") List<Long> ids) {
    saleCartService.deleteSaleCart(ids);
    return success(true);
}
```

- [ ] **Step 5: 实现前端 API**

在 `api/erp/sale/cart/index.ts` 增加：

```ts
export function deleteSaleCart(ids: number[]) {
  return requestClient.delete('/erp/sale-cart/delete', {
    params: { ids: ids.join(',') },
  });
}
```

- [ ] **Step 6: 实现前端按钮**

在 `views/erp/sale/cart/index.vue`：

```ts
import { deleteSaleCart } from '#/api/erp/sale/cart';

async function handleDelete(ids: number[]) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting'),
    duration: 0,
  });
  try {
    await deleteSaleCart(ids);
    message.success($t('ui.actionMessage.deleteSuccess'));
    checkedIds.value = [];
    handleRefresh();
  } finally {
    hideLoading();
  }
}
```

按钮规则：

```ts
{
  label: $t('common.delete'),
  type: 'link',
  danger: true,
  icon: ACTION_ICON.DELETE,
  auth: ['erp:sale-cart:delete'],
  ifShow: () => row.status === 10,
  popConfirm: {
    title: $t('ui.actionMessage.deleteConfirm', [row.no]),
    confirm: handleDelete.bind(null, [row.id!]),
  },
}
```

- [ ] **Step 7: 验证**

Run:

```bash
mvn test -pl yudao-module-erp "-Dtest=ErpSaleCartServiceImplTest"
mvn compile -pl yudao-module-erp -am -DskipTests
```

Expected: BUILD SUCCESS，销售手推车测试通过。

---

## Task 2: 销售菜单 SQL 幂等化

**Files:**
- Modify: `sql/mysql/erp_sale_menu_v14.sql`

- [ ] **Step 1: 备份现有脚本内容**

Run:

```bash
Copy-Item sql/mysql/erp_sale_menu_v14.sql sql/mysql/erp_sale_menu_v14.sql.bak
```

- [ ] **Step 2: 将 `system_menu` 插入改为幂等写法**

每条菜单改为 `INSERT ... ON DUPLICATE KEY UPDATE`，格式：

```sql
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2970, '报价订单', '', 2, 5, 2617, 'quote', 'fa:file-text-o',
 'erp/sale/quote/index', 'ErpSaleQuote',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';
```

- [ ] **Step 3: 将 `system_role_menu` 授权改为防重复**

使用 `INSERT IGNORE`：

```sql
INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 2970, '1', NOW(), '1', NOW(), b'0', 1);
```

- [ ] **Step 4: 静态检查 SQL ID 覆盖**

Run:

```bash
Select-String -Path sql/mysql/erp_sale_menu_v14.sql -Pattern "2977|2989|ON DUPLICATE KEY UPDATE|INSERT IGNORE"
```

Expected: 能看到导出权限 ID `2977`、`2989`，菜单 upsert 和授权 `INSERT IGNORE`。

---

## Task 3: 销售调价前端完整化

**Files:**
- Verify/Modify: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/api/erp/sale/priceadjust/index.ts`
- Verify/Modify: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/sale/priceadjust/index.vue`
- Verify/Modify: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/sale/priceadjust/data.ts`
- Verify/Modify: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/sale/priceadjust/modules/form.vue`
- Create if missing: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/sale/priceadjust/modules/sale-out-select.vue`

- [ ] **Step 1: 对齐 API 类型**

确认 `PriceAdjust` 类型包含：

```ts
originalSaleOutId?: number;
originalSaleOutNo?: string;
newSaleOutId?: number;
newSaleOutNo?: string;
adjustReason?: string;
items?: PriceAdjustItem[];
```

确认 `PriceAdjustItem` 类型包含：

```ts
saleOutItemId?: number;
saleOutNo?: string;
productId?: number;
productName?: string;
count?: number;
oldProductPrice?: number;
productPrice?: number;
totalPrice?: number;
```

- [ ] **Step 2: 新建销售单选择弹窗**

弹窗必须使用数据列表型规范：

```vue
<Modal class="!w-[85vw]" title="选择销售单">
  <div class="h-[70vh]">
    <Table
      :scroll="{ x: 'max-content', y: 'calc(70vh - 180px)' }"
    />
  </div>
</Modal>
```

查询条件默认只查已审核销售单：

```ts
await getSaleOutPage({
  pageNo,
  pageSize,
  status: 20,
  ...searchValues,
});
```

- [ ] **Step 3: 表单接入选择销售单**

选择销售单后：

```ts
formApi.setValues({
  originalSaleOutId: saleOut.id,
  originalSaleOutNo: saleOut.no,
  customerId: saleOut.customerId,
  accountId: saleOut.accountId,
});
items.value = (detail.items || []).map((item) => ({
  saleOutItemId: item.id,
  saleOutNo: saleOut.no,
  productId: item.productId,
  productName: item.productName,
  count: item.count,
  oldProductPrice: item.productPrice,
  productPrice: item.productPrice,
  totalPrice: item.totalPrice,
}));
```

- [ ] **Step 4: 子表限制只允许改价格和备注**

子表列：
- 商品：只读
- 数量：只读
- 原价格：只读
- 新价格：可编辑
- 新金额：自动计算
- 备注：可编辑

价格变化：

```ts
function handlePriceChange(row: PriceAdjustItem) {
  row.totalPrice = erpPriceMultiply(row.productPrice || 0, row.count || 0) || 0;
  emitItemsUpdate(true);
}
```

- [ ] **Step 5: 列表展示追溯关系**

列表列增加：
- 原销售单号 `originalSaleOutNo`
- 新销售单号 `newSaleOutNo`
- 调价原因 `adjustReason`

- [ ] **Step 6: 验证**

Run:

```bash
pnpm -F @vben/web-antd run typecheck
```

Expected: 全量可能仍因既有无关错误失败；需过滤：

```bash
Select-String -Path typecheck.log -Pattern "src/views/erp/sale/priceadjust|src/api/erp/sale/priceadjust"
```

Expected: 销售调价目录无新增错误。

---

## Task 4: 客户批量编辑、导入、导出升级

**Files:**
- Modify: `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/sale/ErpCustomerController.java`
- Modify: `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/sale/vo/customer/ErpCustomerRespVO.java`
- Create: `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/sale/vo/customer/ErpCustomerImportExcelVO.java`
- Create: `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/sale/vo/customer/ErpCustomerBatchUpdateReqVO.java`
- Modify: `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sale/ErpCustomerService.java`
- Modify: `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sale/ErpCustomerServiceImpl.java`
- Modify: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/api/erp/sale/customer/index.ts`
- Modify: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/sale/customer/index.vue`
- Create: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/sale/customer/modules/import-form.vue`
- Create: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/sale/customer/modules/batch-edit-form.vue`

- [ ] **Step 1: 定义客户导入 Excel VO**

字段覆盖客户基础主表，不覆盖联系人/合同/图片等多 Tab 子资料：

```java
@Data
public class ErpCustomerImportExcelVO {
    @ExcelProperty("客户名称")
    private String name;
    @ExcelProperty("客户编码")
    private String no;
    @ExcelProperty("联系人")
    private String contact;
    @ExcelProperty("联系电话")
    private String mobile;
    @ExcelProperty("客户等级")
    private Integer level;
    @ExcelProperty("地区编号")
    private Long areaId;
    @ExcelProperty("详细地址")
    private String address;
    @ExcelProperty("税号")
    private String taxNo;
    @ExcelProperty("开户行")
    private String bankName;
    @ExcelProperty("银行账号")
    private String bankAccount;
    @ExcelProperty("备注")
    private String remark;
}
```

- [ ] **Step 2: 新增导入模板接口**

Controller:

```java
@GetMapping("/get-import-template")
@Operation(summary = "获得客户导入模板")
@PreAuthorize("@ss.hasPermission('erp:customer:import')")
public void importTemplate(HttpServletResponse response) throws IOException {
    List<ErpCustomerImportExcelVO> list = Collections.singletonList(new ErpCustomerImportExcelVO());
    ExcelUtils.write(response, "客户导入模板.xls", "客户", ErpCustomerImportExcelVO.class, list);
}
```

- [ ] **Step 3: 新增客户导入接口**

Controller:

```java
@PostMapping("/import")
@Operation(summary = "导入客户")
@PreAuthorize("@ss.hasPermission('erp:customer:import')")
public CommonResult<Boolean> importCustomer(@RequestParam("file") MultipartFile file) throws Exception {
    List<ErpCustomerImportExcelVO> list = ExcelUtils.read(file, ErpCustomerImportExcelVO.class);
    customerService.importCustomerList(list);
    return success(true);
}
```

- [ ] **Step 4: 批量编辑只允许改低风险字段**

`ErpCustomerBatchUpdateReqVO`：

```java
@Data
public class ErpCustomerBatchUpdateReqVO {
    @NotEmpty(message = "客户编号不能为空")
    private List<Long> ids;
    private Long saleUserId;
    private Long deptId;
    private Long priceSystemId;
    private Integer status;
    private String routeCode;
    private String remark;
}
```

Service 只更新非空字段，禁止批量改客户名称、税号、银行账号等高风险字段。

- [ ] **Step 5: 前端新增导入和批量编辑弹窗**

客户列表工具栏增加：
- 导入
- 下载模板
- 批量编辑
- 导出

批量编辑按钮仅在已勾选客户时可用。

- [ ] **Step 6: 验证**

Run:

```bash
mvn compile -pl yudao-module-erp -am -DskipTests
pnpm -F @vben/web-antd run typecheck
```

Expected: 后端编译通过；前端过滤 `src/views/erp/sale/customer|src/api/erp/sale/customer` 无新增错误。

---

## Task 5: 销售配置深度联动

**Files:**
- Modify: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/api/erp/sale/config/index.ts`
- Modify: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/sale/customer/modules/contract-tab.vue`
- Modify: `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/views/erp/sale/customer/modules/task-tab.vue`
- Modify if exists: customer logistics/basic form files under `views/erp/sale/customer/modules/`

- [ ] **Step 1: API 增加按类型 simple-list**

```ts
export function getSaleConfigSimpleList(configType: string) {
  return requestClient.get<ErpSaleConfigApi.SaleConfig[]>(
    '/erp/sale-config/simple-list',
    { params: { configType, status: 0 } },
  );
}
```

后端如果没有 `/simple-list`，在 `ErpSaleConfigController` 增加：

```java
@GetMapping("/simple-list")
@Operation(summary = "获得销售配置精简列表")
@PreAuthorize("@ss.hasPermission('erp:sale-config:query')")
public CommonResult<List<ErpSaleConfigRespVO>> getSaleConfigSimpleList(@RequestParam("configType") String configType) {
    return success(BeanUtils.toBean(saleConfigService.getSaleConfigList(configType), ErpSaleConfigRespVO.class));
}
```

- [ ] **Step 2: 客户合同接入合同类型**

合同类型下拉从 `CONTRACT_TYPE` 获取：

```ts
const contractTypeOptions = await getSaleConfigSimpleList('CONTRACT_TYPE');
```

字段映射：

```ts
{
  labelField: 'name',
  valueField: 'code',
}
```

- [ ] **Step 3: 客户任务接入任务级别**

任务级别下拉从 `TASK_LEVEL` 获取。

- [ ] **Step 4: 物流/线路接入线路和运费说明**

线路从 `ROUTE` 获取；运费说明从 `FREIGHT_EXPLAIN` 获取。

- [ ] **Step 5: 验证**

Run:

```bash
pnpm -F @vben/web-antd run typecheck
```

Expected: 过滤客户和销售配置目录无新增错误。

---

## Task 6: 销售单据打印入口预留

**Files:**
- Modify: `views/erp/sale/quote/index.vue`
- Modify: `views/erp/sale/cart/index.vue`
- Modify: `views/erp/sale/out/index.vue`
- Modify: `views/erp/sale/return/index.vue`
- Modify: `views/erp/sale/priceadjust/index.vue`

- [ ] **Step 1: 增加统一打印占位函数**

每个页面增加：

```ts
function handlePrint(row: Record<string, any>) {
  message.info(`打印模板正在配置中，单号：${row.no || '-'}`);
}
```

- [ ] **Step 2: 行操作增加打印按钮**

```ts
{
  label: '打印',
  type: 'link',
  icon: ACTION_ICON.PRINT,
  auth: ['erp:sale-quote:query'],
  onClick: handlePrint.bind(null, row),
}
```

如果 `ACTION_ICON.PRINT` 不存在，则使用 `ACTION_ICON.VIEW` 或查找项目已有打印图标常量。

- [ ] **Step 3: 验证**

Run:

```bash
pnpm -F @vben/web-antd run typecheck
```

Expected: 过滤 `src/views/erp/sale` 无新增错误。

---

## Task 7: 工商信息占位确认与文案收口

**Files:**
- Verify/Modify: `views/erp/sale/customer/modules/business-info-tab.vue`
- Verify/Modify: `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/sale/ErpCustomerBusinessInfoController.java`

- [ ] **Step 1: 确认后端接口只保存占位资料**

接口只保存本地字段，不调用第三方：

```java
// 当前阶段不接第三方工商 API，仅保留人工维护字段和后续对接入口。
```

- [ ] **Step 2: 前端 Tab 显示占位状态**

页面仅显示资料字段和“暂未接入第三方工商 API”的轻提示，不出现不可用按钮。

- [ ] **Step 3: 验证**

Run:

```bash
mvn compile -pl yudao-module-erp -am -DskipTests
```

Expected: BUILD SUCCESS。

---

## Task 8: 真实环境业务验收

**Files:**
- No code files unless defects are found.

- [ ] **Step 1: 启动后端**

Run:

```bash
mvn clean package -pl yudao-server -am -DskipTests
```

启动 `cn.iocoder.yudao.server.YudaoServerApplication`，Profile 使用 `local`。

- [ ] **Step 2: 启动前端**

Run:

```bash
cd yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master
pnpm -F @vben/web-antd run dev
```

- [ ] **Step 3: 执行 SQL**

按顺序执行：

```bash
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_sale_quote_cart_v8.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_sale_customer_tabs_v9.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_sale_customer_extend_v10.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_sale_return_v11.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_sale_price_adjust_v12.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_sale_config_v13.sql
mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_sale_menu_v14.sql
```

- [ ] **Step 4: 刷新菜单缓存**

后台：系统管理 → 菜单管理 → 刷新缓存。然后退出重登。

- [ ] **Step 5: 点验主路径**

验收路径：

```text
客户档案 → 报价订单 → 转销售手推车 → 提交 → 初审 → 终审 → 销售单 → 库存扣减
销售手推车 → 提交 → 初审 → 终审 → 销售单 → 库存扣减
报价订单 → 审核生成销售单 → 库存扣减
销售单 → 按销售单退货 → 审批 → 入库存
销售单 → 销售调价 → 审批 → 旧单追溯 + 新单生成 + 不重复扣库存
```

- [ ] **Step 6: 验收记录**

在 `AGENTS.md` / `CLAUDE.md` 追加：

```markdown
### 销售模块真实环境验收（YYYY-MM-DD）
- 报价订单主路径：通过/失败，问题：
- 销售手推车主路径：通过/失败，问题：
- 销售退货：通过/失败，问题：
- 销售调价：通过/失败，问题：
- 菜单权限：通过/失败，问题：
```

---

## 不建议并入本轮的工作

1. **真实天眼查/企查查 API 对接**：涉及供应商、费用、限流、授权和字段映射，建议单独立项。
2. **复杂打印模板设计器**：当前只预留入口；模板排版、套打、权限和打印历史建议单独开发。
3. **复杂应收账款/佣金核算**：会影响财务模块，不能混在销售基础闭环收尾里。
4. **前端全量 typecheck 清理**：当前错误分布在 AI/BPM/库存/IoT/Mall 等多个模块，建议单独专项处理。

---

## 推荐执行顺序

1. Task 1：销售手推车删除能力
2. Task 2：菜单 SQL 幂等化
3. Task 3：销售调价前端完整化
4. Task 5：销售配置深度联动
5. Task 6：打印入口预留
6. Task 4：客户批量编辑、导入、导出升级
7. Task 7：工商信息占位确认
8. Task 8：真实环境业务验收

推荐先做 1-3，因为它们直接影响当前销售主链路闭环和部署稳定性；客户导入/批量编辑可以作为下一批。

