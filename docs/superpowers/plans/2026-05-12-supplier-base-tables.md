# Supplier Base Tables Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ERP 供应商资料搭建文档《供应商基础表-修改版.md》要求的全部供应商基础子表、后端 CRUD 接口、前端 API 类型，为后续供应商 Tab 页面接入提供稳定数据底座。

**Architecture:** 供应商主表 `erp_supplier` 保持现状，本期新增供应商专属子表，不抽象为客户/供应商共用表，避免影响销售客户侧现有实现。后端按 RuoYi Vue Pro 现有 ERP 模块模式落地：DO + Mapper + VO + Service + Controller + SQL 迁移；前端本期只补 API 层，供应商 Tab UI 后续单独开发。

**Tech Stack:** Spring Boot 2.7.18、JDK 8、MyBatis Plus、BaseDO、BaseMapperX、CommonResult、PageResult、Vben Admin、TypeScript。

---

## 1. 范围边界

本期只做“基础表搭建”和“基础 CRUD 能力”，不做供应商详情 Tab 页面。

本期包含：

- 新增 SQL 迁移脚本：`sql/mysql/erp_supplier_base_tables_v20.sql`
- 新增供应商子表：联系人、合同、结构化拓展、动态拓展、企业图片、供应商账户、票据登记、任务量、工商信息占位
- 新增后端 DO / Mapper / VO / Service / Controller
- 新增错误码
- 新增前端 API 类型和请求函数
- 编译验证

本期不包含：

- 供应商详情 Tab UI
- 图片真实上传组件接入
- 文件真实上传组件接入
- 天眼查、企查查等第三方工商 API 对接
- 菜单 SQL 注册
- 客户侧代码重构
- 客户/供应商通用抽象重构

---

## 2. 数据表总览

| 表名 | 说明 | 关系 | 主要接口形态 |
|---|---|---|---|
| `erp_supplier_contact` | 供应商联系人档案 | 供应商 1:N 联系人 | 按 `supplierId` 列表 + CRUD |
| `erp_supplier_contract` | 供应商合同 | 供应商 1:N 合同 | 按 `supplierId` 列表 + CRUD |
| `erp_supplier_extend_info` | 供应商结构化拓展信息 | 供应商 1:1 拓展配置 | 按 `supplierId` get/save |
| `erp_supplier_extend` | 供应商动态拓展字段值 | 供应商 1:N KV 字段 | 按 `supplierId` 列表 + CRUD |
| `erp_supplier_image` | 供应商企业图片 | 供应商 1:N 图片 | 按 `supplierId` 列表 + CRUD |
| `erp_supplier_account` | 供应商银行账户 | 供应商 1:N 账户 | 按 `supplierId` 列表 + CRUD |
| `erp_supplier_bill` | 供应商票据登记 | 供应商 1:N 票据 | 按 `supplierId` 列表 + CRUD |
| `erp_supplier_task` | 供应商任务量管理 | 供应商 1:N 月度任务 | 按 `supplierId` 列表 + CRUD |
| `erp_supplier_business_info` | 供应商工商信息占位 | 供应商 1:N 工商记录 | 按 `supplierId` 列表 + CRUD |

所有表统一继承 BaseDO 字段：

| 字段 | 数据库类型 | Java 类型 | 必填 | 说明 |
|---|---|---|---|---|
| `creator` | `varchar(64)` | BaseDO | 否 | 创建者，框架自动填充 |
| `create_time` | `datetime` | BaseDO | 是 | 创建时间，框架自动填充 |
| `updater` | `varchar(64)` | BaseDO | 否 | 更新者，框架自动填充 |
| `update_time` | `datetime` | BaseDO | 是 | 更新时间，框架自动填充 |
| `deleted` | `bit(1)` | BaseDO | 是 | 逻辑删除 |
| `tenant_id` | `bigint` | BaseDO | 是 | 租户编号 |

---

## 3. 每张基础表字段设计

### 3.1 `erp_supplier_contact` 供应商联系人档案

对应文档 Tab 2：联系人档案。

#### 字段清单

| 字段 | 数据库类型 | Java/TS 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|---|
| `id` | `bigint` | `Long / number` | 是 | 自增 | 主键 |
| `supplier_id` | `bigint` | `Long / number` | 是 | 无 | 供应商编号，关联 `erp_supplier.id` |
| `name` | `varchar(64)` | `String / string` | 是 | 无 | 姓名 |
| `salesperson` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 导购员 |
| `telephone` | `varchar(32)` | `String / string` | 否 | `NULL` | 联系电话、固定电话 |
| `mobile` | `varchar(32)` | `String / string` | 是 | 无 | 移动电话、手机号 |
| `address` | `varchar(255)` | `String / string` | 否 | `NULL` | 联系地址 |
| `email` | `varchar(128)` | `String / string` | 否 | `NULL` | 电子邮箱 |
| `gender` | `tinyint` | `Integer / number` | 否 | `NULL` | 性别：1 男，2 女 |
| `position` | `varchar(64)` | `String / string` | 否 | `NULL` | 职务 |
| `company_id` | `bigint` | `Long / number` | 是 | 无 | 所属公司；供应商场景默认当前 `supplier_id`，保留自关联能力 |
| `fax` | `varchar(32)` | `String / string` | 否 | `NULL` | 传真 |
| `dept_id` | `bigint` | `Long / number` | 否 | `NULL` | 部门编号，关联系统部门 |
| `importance` | `tinyint` | `Integer / number` | 否 | `1` | 重要性：1 普通，2 重要，3 决策人 |
| `commission_rate` | `decimal(10,2)` | `BigDecimal / number` | 否 | `NULL` | 提成率，百分比值，如 5 表示 5% |
| `fixed_commission` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 固定提成 |
| `post_code` | `varchar(20)` | `String / string` | 否 | `NULL` | 邮编 |
| `qq` | `varchar(32)` | `String / string` | 否 | `NULL` | QQ 号 |
| `birthday` | `date` | `LocalDate / string` | 否 | `NULL` | 生日 |
| `primary_contact` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 主要联系人 |
| `receiver_contact` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 收货主要联系人 |
| `settle_contact` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 结算主要联系人 |
| `message_contact` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 消息主要联系人 |
| `wechat` | `varchar(64)` | `String / string` | 否 | `NULL` | 微信 |
| `wechat_official_status` | `tinyint` | `Integer / number` | 否 | `0` | 关注公众号状态：0 未关注，1 已邀请，2 已关注 |
| `order_access` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 订单访问权限 |
| `ecommerce_access` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 电商访问 |
| `business_card_front_url` | `varchar(512)` | `String / string` | 否 | `NULL` | 名片正面图片 URL |
| `business_card_back_url` | `varchar(512)` | `String / string` | 否 | `NULL` | 名片背面图片 URL |
| `last_contact_time` | `datetime` | `LocalDateTime / string` | 否 | `NULL` | 最后一次联系时间；创建/更新接口允许传值，后续业务可自动维护 |
| `status` | `tinyint` | `Integer / number` | 否 | `0` | 状态，预留 |
| `remark` | `varchar(500)` | `String / string` | 否 | `NULL` | 备注 |

#### SaveReqVO 必填校验

- `supplierId`：`@NotNull(message = "供应商不能为空")`
- `name`：`@NotEmpty(message = "姓名不能为空")`
- `mobile`：`@NotEmpty(message = "移动电话不能为空")`
- `companyId`：后端允许为空；如果为空，Service 保存前自动设为 `supplierId`。这样满足文档“必填”，同时降低前端负担。

#### 索引

- `idx_supplier_contact_supplier_id (supplier_id)`
- `idx_supplier_contact_mobile (mobile)`

---

### 3.2 `erp_supplier_contract` 供应商合同

对应文档 Tab 3：合同。

#### 字段清单

| 字段 | 数据库类型 | Java/TS 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|---|
| `id` | `bigint` | `Long / number` | 是 | 自增 | 主键 |
| `supplier_id` | `bigint` | `Long / number` | 是 | 无 | 供应商编号 |
| `contract_no` | `varchar(64)` | `String / string` | 是 | 无 | 合同编号 |
| `contract_date` | `datetime` | `LocalDateTime / string` | 是 | 无 | 合同日期 |
| `contract_type` | `varchar(64)` | `String / string` | 否 | `NULL` | 合同类型，关联配置表，默认普通合同 |
| `settle_method` | `varchar(64)` | `String / string` | 否 | `NULL` | 结算方式 |
| `start_time` | `datetime` | `LocalDateTime / string` | 否 | `NULL` | 生效日期 |
| `transport_method` | `varchar(64)` | `String / string` | 否 | `NULL` | 运输方式 |
| `end_time` | `datetime` | `LocalDateTime / string` | 否 | `NULL` | 终止日期 |
| `main_contract` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 主要合同 |
| `rebate_enabled` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 返点证集、是否启用返点 |
| `freight_settle_method` | `tinyint` | `Integer / number` | 否 | `NULL` | 运费结算方式：1 我方承担，2 客户承担，3 双方平摊，4 月结 |
| `base_amount` | `decimal(24,6)` | `BigDecimal / number` | 否 | `0` | 铺底金额 |
| `task_amount` | `decimal(24,6)` | `BigDecimal / number` | 否 | `0` | 目标任务量 |
| `summary` | `text` | `String / string` | 否 | `NULL` | 摘要 |
| `attachment_url` | `varchar(512)` | `String / string` | 否 | `NULL` | 合同附件 URL；本期不做上传，只存 URL |
| `status` | `tinyint` | `Integer / number` | 否 | `0` | 状态，预留 |
| `remark` | `varchar(500)` | `String / string` | 否 | `NULL` | 备注 |

#### SaveReqVO 必填校验

- `supplierId`：`@NotNull(message = "供应商不能为空")`
- `contractNo`：`@NotEmpty(message = "合同编号不能为空")`
- `contractDate`：`@NotNull(message = "合同日期不能为空")`

#### 索引

- `idx_supplier_contract_supplier_id (supplier_id)`
- `idx_supplier_contract_no (contract_no)`

---

### 3.3 `erp_supplier_extend_info` 供应商结构化拓展信息

对应文档 Tab 4 的固定字段：业务配置字段、自动补货配置、自动计算上下限配置。

该表为一对一：同一租户、同一供应商只保留一条未删除记录。

#### 字段清单：业务配置字段

| 字段 | 数据库类型 | Java/TS 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|---|
| `id` | `bigint` | `Long / number` | 是 | 自增 | 主键 |
| `supplier_id` | `bigint` | `Long / number` | 是 | 无 | 供应商编号 |
| `chain_group_id` | `bigint` | `Long / number` | 否 | `NULL` | 对应连锁，关联连锁店/集团配置 |
| `bank_name2` | `varchar(128)` | `String / string` | 否 | `NULL` | 开户行 2 |
| `bank_account2` | `varchar(64)` | `String / string` | 否 | `NULL` | 账户 2 |
| `ext_email` | `varchar(128)` | `String / string` | 否 | `NULL` | 电子邮件 |
| `id_card_no` | `varchar(32)` | `String / string` | 否 | `NULL` | 身份证号码 |
| `alternate_code` | `varchar(64)` | `String / string` | 否 | `NULL` | 替代编码 |
| `base_amount` | `decimal(18,2)` | `BigDecimal / number` | 否 | `0` | 铺底金额 |
| `legal_person` | `varchar(64)` | `String / string` | 否 | `NULL` | 企业法人 |
| `legal_person_phone` | `varchar(32)` | `String / string` | 否 | `NULL` | 法人电话 |
| `boss` | `varchar(64)` | `String / string` | 否 | `NULL` | 老板 |
| `boss_phone` | `varchar(32)` | `String / string` | 否 | `NULL` | 老板电话 |
| `price_markup_rate` | `decimal(10,4)` | `BigDecimal / number` | 否 | `NULL` | 浮动率、售价上浮比例 |
| `key_supplier` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 重点供应商 |
| `alliance_replenishment` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 联盟补货 |
| `purchase_return_contact` | `varchar(64)` | `String / string` | 否 | `NULL` | 采购退货联系人 |
| `purchase_return_address` | `varchar(500)` | `String / string` | 否 | `NULL` | 采购退货地址 |
| `purchase_return_phone` | `varchar(32)` | `String / string` | 否 | `NULL` | 采购退货电话 |
| `settle_day` | `int` | `Integer / number` | 否 | `NULL` | 结账日期，建议存 1-31 的日号 |
| `generate_in_bill` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 是否产生入仓单 |
| `inbound_method` | `varchar(64)` | `String / string` | 否 | `NULL` | 入库方式，如采购入库 |
| `generate_label_task` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 是否产生贴码任务 |
| `alliance_order_match_rule` | `text` | `String / string` | 否 | `NULL` | 联盟商开单匹配规则 |
| `alliance_bill_extract_rule` | `text` | `String / string` | 否 | `NULL` | 联盟商单据提取规则 |
| `settle_account_id` | `bigint` | `Long / number` | 否 | `NULL` | 结算账户，关联 `erp_account.id` |
| `scrap_return_cycle_days` | `int` | `Integer / number` | 否 | `NULL` | 废品返厂周期，单位天 |
| `scrap_return_next_date` | `date` | `LocalDate / string` | 否 | `NULL` | 返厂下次计算日期 |
| `scrap_return_threshold_amount` | `decimal(18,2)` | `BigDecimal / number` | 否 | `NULL` | 废品返厂阈值金额 |

#### 字段清单：自动补货配置

| 字段 | 数据库类型 | Java/TS 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|---|
| `auto_replenishment_enabled` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 启用自动补货 |
| `auto_replenishment_formula` | `varchar(64)` | `String / string` | 否 | `NULL` | 自动补货公式，如按销备货 |
| `auto_replenishment_cycle` | `varchar(128)` | `String / string` | 否 | `NULL` | 自动补货周期，如到货周期+采购提前期 |
| `auto_replenishment_time` | `time` | `LocalTime / string` | 否 | `NULL` | 自动补货时间，如 00:00 |
| `next_order_date` | `date` | `LocalDate / string` | 否 | `NULL` | 下次订货日期 |

#### 字段清单：自动计算上下限配置

| 字段 | 数据库类型 | Java/TS 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|---|
| `stock_upper_days` | `int` | `Integer / number` | 否 | `NULL` | 库存上限天数 |
| `stock_standard_days` | `int` | `Integer / number` | 否 | `NULL` | 标准库存天数 |
| `stock_lower_days` | `int` | `Integer / number` | 否 | `NULL` | 库存下限天数 |
| `daily_sale_cycle` | `varchar(64)` | `String / string` | 否 | `NULL` | 日均销售周期，如按天数算 |
| `daily_sale_recent_days` | `int` | `Integer / number` | 否 | `NULL` | 日均销量距今，单位天 |
| `limit_adjust_recent_months` | `int` | `Integer / number` | 否 | `NULL` | 上下限调整条件：最近 X 个月 |
| `limit_adjust_monthly_sales` | `decimal(18,2)` | `BigDecimal / number` | 否 | `NULL` | 上下限调整条件：月均销量 |

#### SaveReqVO 必填校验

- `supplierId`：`@NotNull(message = "供应商不能为空")`
- 其他字段均非必填。

#### 约束和索引

- 唯一键：`uk_supplier_extend_info_supplier_id (supplier_id, tenant_id, deleted)`

---

### 3.4 `erp_supplier_extend` 供应商动态拓展字段值

对应文档 Tab 4 的“扩展属性配置（元数据/动态字段）”中供应商实际字段值。本表先做字段值承载；动态字段模板配置后续可以复用现有 `erp_field_config` 或单独扩展，不在本期新增复杂元数据表。

#### 字段清单

| 字段 | 数据库类型 | Java/TS 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|---|
| `id` | `bigint` | `Long / number` | 是 | 自增 | 主键 |
| `supplier_id` | `bigint` | `Long / number` | 是 | 无 | 供应商编号 |
| `extend_key` | `varchar(64)` | `String / string` | 是 | 无 | 字段标识 |
| `extend_name` | `varchar(128)` | `String / string` | 否 | `NULL` | 字段名称 |
| `extend_value` | `text` | `String / string` | 否 | `NULL` | 字段值 |
| `extend_type` | `varchar(64)` | `String / string` | 否 | `NULL` | 字段类型：文本框、下拉框等 |
| `required` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 是否必填，记录当时配置 |
| `option_values` | `varchar(1000)` | `String / string` | 否 | `NULL` | 下拉选项值，英文分号分隔 |
| `sort` | `int` | `Integer / number` | 否 | `0` | 排序 |
| `remark` | `varchar(500)` | `String / string` | 否 | `NULL` | 备注 |

#### SaveReqVO 必填校验

- `supplierId`：`@NotNull(message = "供应商不能为空")`
- `extendKey`：`@NotEmpty(message = "字段标识不能为空")`
- 如果 `extendType = SELECT` 或 `extendType = 下拉框`，前端后续应要求 `optionValues`；后端本期暂不强约束，避免和现有字段配置类型不一致。

#### 索引

- `idx_supplier_extend_supplier_id (supplier_id)`
- `idx_supplier_extend_key (extend_key)`

---

### 3.5 `erp_supplier_image` 供应商企业图片

对应文档 Tab 5：企业信息。

#### 字段清单

| 字段 | 数据库类型 | Java/TS 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|---|
| `id` | `bigint` | `Long / number` | 是 | 自增 | 主键 |
| `supplier_id` | `bigint` | `Long / number` | 是 | 无 | 供应商编号 |
| `image_type` | `varchar(64)` | `String / string` | 是 | 无 | 图片类型：营业执照、门头照片、公司证书、名片、内部照片 |
| `image_name` | `varchar(128)` | `String / string` | 否 | `NULL` | 图片名称 |
| `image_url` | `varchar(512)` | `String / string` | 是 | 无 | 图片 URL |
| `defaulted` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 是否默认图 |
| `sort` | `int` | `Integer / number` | 否 | `0` | 排序 |
| `remark` | `varchar(500)` | `String / string` | 否 | `NULL` | 备注 |

#### SaveReqVO 必填校验

- `supplierId`：`@NotNull(message = "供应商不能为空")`
- `imageType`：`@NotEmpty(message = "图片类型不能为空")`
- `imageUrl`：`@NotEmpty(message = "图片地址不能为空")`

#### 索引

- `idx_supplier_image_supplier_id (supplier_id)`
- `idx_supplier_image_type (image_type)`

---

### 3.6 `erp_supplier_account` 供应商账户

对应文档 Tab 6：供应商账户。

#### 字段清单

| 字段 | 数据库类型 | Java/TS 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|---|
| `id` | `bigint` | `Long / number` | 是 | 自增 | 主键 |
| `supplier_id` | `bigint` | `Long / number` | 是 | 无 | 供应商编号 |
| `account_name` | `varchar(128)` | `String / string` | 是 | 无 | 账户名，银行账户名称 |
| `card_no` | `varchar(64)` | `String / string` | 是 | 无 | 卡号、银行账号 |
| `bank_name` | `varchar(128)` | `String / string` | 否 | `NULL` | 开户行 |
| `defaulted` | `bit(1)` | `Boolean / boolean` | 否 | `0` | 是否默认账户 |
| `sort` | `int` | `Integer / number` | 否 | `0` | 排序 |
| `remark` | `varchar(500)` | `String / string` | 否 | `NULL` | 备注 |

#### SaveReqVO 必填校验

- `supplierId`：`@NotNull(message = "供应商不能为空")`
- `accountName`：`@NotEmpty(message = "账户名不能为空")`
- `cardNo`：`@NotEmpty(message = "卡号不能为空")`

#### 业务规则

- 如果保存 `defaulted = true`，Service 需要先把同一供应商其他账户的 `defaulted` 置为 `false`，再保存当前记录。
- 该规则本期在 `create` 和 `update` 两个入口都执行。

#### 索引

- `idx_supplier_account_supplier_id (supplier_id)`
- `idx_supplier_account_card_no (card_no)`

---

### 3.7 `erp_supplier_bill` 供应商票据登记

对应文档 Tab 7：票据登记。

#### 字段清单

| 字段 | 数据库类型 | Java/TS 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|---|
| `id` | `bigint` | `Long / number` | 是 | 自增 | 主键 |
| `supplier_id` | `bigint` | `Long / number` | 是 | 无 | 供应商编号 |
| `bill_date` | `date` | `LocalDate / string` | 是 | 无 | 日期，票据登记日期 |
| `bill_no` | `varchar(64)` | `String / string` | 是 | 无 | 票据号、发票号码 |
| `amount` | `decimal(18,2)` | `BigDecimal / number` | 是 | 无 | 金额 |
| `remark` | `varchar(500)` | `String / string` | 否 | `NULL` | 备注 |

#### SaveReqVO 必填校验

- `supplierId`：`@NotNull(message = "供应商不能为空")`
- `billDate`：`@NotNull(message = "票据日期不能为空")`
- `billNo`：`@NotEmpty(message = "票据号不能为空")`
- `amount`：`@NotNull(message = "金额不能为空")`

#### 索引

- `idx_supplier_bill_supplier_id (supplier_id)`
- `idx_supplier_bill_no (bill_no)`

---

### 3.8 `erp_supplier_task` 供应商任务量管理

对应文档 Tab 8：任务量管理。

#### 字段清单

| 字段 | 数据库类型 | Java/TS 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|---|
| `id` | `bigint` | `Long / number` | 是 | 自增 | 主键 |
| `supplier_id` | `bigint` | `Long / number` | 是 | 无 | 供应商编号 |
| `year` | `int` | `Integer / number` | 是 | 无 | 年份 |
| `month` | `int` | `Integer / number` | 是 | 无 | 月份，1-12 |
| `task_level` | `varchar(64)` | `String / string` | 否 | `NULL` | 级别，关联任务级别配置 |
| `task_amount` | `decimal(24,6)` | `BigDecimal / number` | 是 | 无 | 任务量 |
| `remark` | `varchar(500)` | `String / string` | 否 | `NULL` | 备注 |

#### SaveReqVO 必填校验

- `supplierId`：`@NotNull(message = "供应商不能为空")`
- `year`：`@NotNull(message = "年份不能为空")`
- `month`：`@NotNull(message = "月份不能为空")`
- `taskAmount`：`@NotNull(message = "任务量不能为空")`

#### 业务规则

- `month` 必须在 1 到 12 之间。
- 建议唯一键：同一供应商、同一年月、同一任务级别只允许一条未删除记录。
- 唯一键字段：`supplier_id, year, month, task_level, tenant_id, deleted`。注意 MySQL 唯一键中 `task_level` 为 `NULL` 时可能允许多条，Service 层也要补校验。

#### 索引

- `idx_supplier_task_supplier_id (supplier_id)`
- `idx_supplier_task_period (year, month)`

---

### 3.9 `erp_supplier_business_info` 供应商工商信息占位

对应文档 Tab 9：工商信息（后期实现）。

#### 字段清单

| 字段 | 数据库类型 | Java/TS 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|---|
| `id` | `bigint` | `Long / number` | 是 | 自增 | 主键 |
| `supplier_id` | `bigint` | `Long / number` | 是 | 无 | 供应商编号 |
| `company_name` | `varchar(128)` | `String / string` | 否 | `NULL` | 企业名称 |
| `credit_code` | `varchar(64)` | `String / string` | 否 | `NULL` | 统一信用代码 |
| `legal_person` | `varchar(64)` | `String / string` | 否 | `NULL` | 法定代表人 |
| `registered_address` | `varchar(500)` | `String / string` | 否 | `NULL` | 注册地址 |
| `business_scope` | `text` | `String / string` | 否 | `NULL` | 经营范围 |
| `registered_capital` | `varchar(64)` | `String / string` | 否 | `NULL` | 注册资本 |
| `establish_date` | `varchar(64)` | `String / string` | 否 | `NULL` | 成立日期，先用字符串兼容第三方数据 |
| `business_status` | `varchar(64)` | `String / string` | 否 | `NULL` | 经营状态 |
| `raw_data` | `longtext` | `String / string` | 否 | `NULL` | 第三方接口原始数据，后期预留 |
| `remark` | `varchar(500)` | `String / string` | 否 | `NULL` | 备注 |

#### SaveReqVO 必填校验

- `supplierId`：`@NotNull(message = "供应商不能为空")`
- 其他字段均非必填。因为文档明确后期实现，本期只做占位。

#### 索引

- `idx_supplier_business_info_supplier_id (supplier_id)`
- `idx_supplier_business_info_credit_code (credit_code)`

---

## 4. 后端文件规划

### 4.1 SQL

- Create: `sql/mysql/erp_supplier_base_tables_v20.sql`
  - 创建上述 9 张供应商基础表。
  - 使用 `CREATE TABLE IF NOT EXISTS`。
  - 表注释统一以 `ERP 供应商...` 开头。
  - 不写菜单数据。

### 4.2 DO

Create:

- `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/dataobject/purchase/ErpSupplierContactDO.java`
- `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/dataobject/purchase/ErpSupplierContractDO.java`
- `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/dataobject/purchase/ErpSupplierExtendInfoDO.java`
- `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/dataobject/purchase/ErpSupplierExtendDO.java`
- `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/dataobject/purchase/ErpSupplierImageDO.java`
- `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/dataobject/purchase/ErpSupplierAccountDO.java`
- `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/dataobject/purchase/ErpSupplierBillDO.java`
- `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/dataobject/purchase/ErpSupplierTaskDO.java`
- `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/dataobject/purchase/ErpSupplierBusinessInfoDO.java`

要求：

- 全部继承 `BaseDO`。
- 全部使用 `@TableName`。
- 全部使用 `@KeySequence`，与现有项目兼容 Oracle、PostgreSQL、KingBase、DB2、H2。
- 金额、比例、数量使用 `BigDecimal`。
- 日期使用 `LocalDate`，日期时间使用 `LocalDateTime`，时间使用 `LocalTime`。

### 4.3 Mapper

Create:

- `dal/mysql/purchase/ErpSupplierContactMapper.java`
- `dal/mysql/purchase/ErpSupplierContractMapper.java`
- `dal/mysql/purchase/ErpSupplierExtendInfoMapper.java`
- `dal/mysql/purchase/ErpSupplierExtendMapper.java`
- `dal/mysql/purchase/ErpSupplierImageMapper.java`
- `dal/mysql/purchase/ErpSupplierAccountMapper.java`
- `dal/mysql/purchase/ErpSupplierBillMapper.java`
- `dal/mysql/purchase/ErpSupplierTaskMapper.java`
- `dal/mysql/purchase/ErpSupplierBusinessInfoMapper.java`

要求：

- 全部继承 `BaseMapperX<T>`。
- 1:N 子表提供 `selectListBySupplierId(Long supplierId)`。
- 1:1 `ErpSupplierExtendInfoMapper` 提供 `selectBySupplierId(Long supplierId)`。
- 账户 Mapper 提供 `clearDefaultBySupplierId(Long supplierId, Long excludeId)`，用于默认账户互斥。
- 任务 Mapper 提供按供应商、年月、级别查重方法。

### 4.4 VO

Create packages:

- `controller/admin/purchase/vo/suppliercontact/`
- `controller/admin/purchase/vo/suppliercontract/`
- `controller/admin/purchase/vo/supplierextendinfo/`
- `controller/admin/purchase/vo/supplierextend/`
- `controller/admin/purchase/vo/supplierimage/`
- `controller/admin/purchase/vo/supplieraccount/`
- `controller/admin/purchase/vo/supplierbill/`
- `controller/admin/purchase/vo/suppliertask/`
- `controller/admin/purchase/vo/supplierbusinessinfo/`

每个 1:N 表创建：

- `ErpSupplierXxxRespVO`
- `ErpSupplierXxxSaveReqVO`
- 如后续需要独立分页，再补 `PageReqVO`；本期供应商详情 Tab 预计按供应商查询列表，因此先做 list，不做 page。

`erp_supplier_extend_info` 创建：

- `ErpSupplierExtendInfoRespVO`
- `ErpSupplierExtendInfoSaveReqVO`

### 4.5 Service

Create:

- `service/purchase/ErpSupplierContactService.java`
- `service/purchase/ErpSupplierContactServiceImpl.java`
- `service/purchase/ErpSupplierContractService.java`
- `service/purchase/ErpSupplierContractServiceImpl.java`
- `service/purchase/ErpSupplierExtendInfoService.java`
- `service/purchase/ErpSupplierExtendInfoServiceImpl.java`
- `service/purchase/ErpSupplierExtendService.java`
- `service/purchase/ErpSupplierExtendServiceImpl.java`
- `service/purchase/ErpSupplierImageService.java`
- `service/purchase/ErpSupplierImageServiceImpl.java`
- `service/purchase/ErpSupplierAccountService.java`
- `service/purchase/ErpSupplierAccountServiceImpl.java`
- `service/purchase/ErpSupplierBillService.java`
- `service/purchase/ErpSupplierBillServiceImpl.java`
- `service/purchase/ErpSupplierTaskService.java`
- `service/purchase/ErpSupplierTaskServiceImpl.java`
- `service/purchase/ErpSupplierBusinessInfoService.java`
- `service/purchase/ErpSupplierBusinessInfoServiceImpl.java`

通用 Service 方法：

```java
Long createXxx(ErpSupplierXxxSaveReqVO createReqVO);
void updateXxx(ErpSupplierXxxSaveReqVO updateReqVO);
void deleteXxx(Long id);
ErpSupplierXxxDO getXxx(Long id);
List<ErpSupplierXxxDO> getXxxList(Long supplierId);
```

结构化拓展信息 Service 方法：

```java
ErpSupplierExtendInfoDO getExtendInfo(Long supplierId);
Long saveExtendInfo(ErpSupplierExtendInfoSaveReqVO saveReqVO);
```

通用校验：

- `validateSupplierExists(supplierId)`：调用现有 `ErpSupplierService.getSupplier(supplierId)` 或 `validateSupplier(supplierId)`。
- 更新、删除时校验记录存在。
- 更新、删除时校验记录的 `supplierId` 不跨供应商；如果接口只传 `id`，可暂不暴露跨供应商参数，但 Service 内根据 `id` 查记录后操作。

特殊规则：

- 联系人：`companyId` 为空时自动设置为 `supplierId`。
- 账户：默认账户互斥。
- 任务量：校验月份 1-12；同供应商、年份、月份、级别不重复。
- 结构化拓展：保存时如果不存在则创建，存在则更新。

### 4.6 Controller

Create:

- `controller/admin/purchase/ErpSupplierContactController.java`
- `controller/admin/purchase/ErpSupplierContractController.java`
- `controller/admin/purchase/ErpSupplierExtendInfoController.java`
- `controller/admin/purchase/ErpSupplierExtendController.java`
- `controller/admin/purchase/ErpSupplierImageController.java`
- `controller/admin/purchase/ErpSupplierAccountController.java`
- `controller/admin/purchase/ErpSupplierBillController.java`
- `controller/admin/purchase/ErpSupplierTaskController.java`
- `controller/admin/purchase/ErpSupplierBusinessInfoController.java`

接口路径：

| Controller | Base Path | 主要接口 |
|---|---|---|
| 联系人 | `/erp/supplier-contact` | `create/update/delete/get/list` |
| 合同 | `/erp/supplier-contract` | `create/update/delete/get/list` |
| 结构化拓展 | `/erp/supplier-extend-info` | `get-by-supplier-id/save` |
| 动态拓展 | `/erp/supplier-extend` | `create/update/delete/get/list` |
| 图片 | `/erp/supplier-image` | `create/update/delete/get/list` |
| 账户 | `/erp/supplier-account` | `create/update/delete/get/list` |
| 票据 | `/erp/supplier-bill` | `create/update/delete/get/list` |
| 任务量 | `/erp/supplier-task` | `create/update/delete/get/list` |
| 工商信息 | `/erp/supplier-business-info` | `create/update/delete/get/list` |

权限标识：

- `erp:supplier:update`：create/update/delete/save 类操作，先复用供应商更新权限。
- `erp:supplier:query`：get/list 类操作。

这样本期不用新增菜单权限数据，后续 Tab UI 也能随供应商权限自然控制。

### 4.7 错误码

Modify:

- `yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/enums/ErrorCodeConstants.java`

新增建议错误码段：

| 常量 | 编码 | 文案 |
|---|---:|---|
| `SUPPLIER_CONTACT_NOT_EXISTS` | `1_030_110_000` | 供应商联系人不存在 |
| `SUPPLIER_CONTRACT_NOT_EXISTS` | `1_030_110_001` | 供应商合同不存在 |
| `SUPPLIER_EXTEND_INFO_NOT_EXISTS` | `1_030_110_002` | 供应商拓展信息不存在 |
| `SUPPLIER_EXTEND_NOT_EXISTS` | `1_030_110_003` | 供应商动态拓展字段不存在 |
| `SUPPLIER_IMAGE_NOT_EXISTS` | `1_030_110_004` | 供应商图片不存在 |
| `SUPPLIER_ACCOUNT_NOT_EXISTS` | `1_030_110_005` | 供应商账户不存在 |
| `SUPPLIER_BILL_NOT_EXISTS` | `1_030_110_006` | 供应商票据不存在 |
| `SUPPLIER_TASK_NOT_EXISTS` | `1_030_110_007` | 供应商任务量不存在 |
| `SUPPLIER_BUSINESS_INFO_NOT_EXISTS` | `1_030_110_008` | 供应商工商信息不存在 |
| `SUPPLIER_TASK_MONTH_INVALID` | `1_030_110_009` | 供应商任务月份必须在 1 到 12 之间 |
| `SUPPLIER_TASK_DUPLICATE` | `1_030_110_010` | 同一供应商同一年月同一级别任务量已存在 |

---

## 5. 前端 API 文件规划

Create:

- `yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master/apps/web-antd/src/api/erp/purchase/supplier/contact.ts`
- `.../supplier/contract.ts`
- `.../supplier/extend-info.ts`
- `.../supplier/extend.ts`
- `.../supplier/image.ts`
- `.../supplier/account.ts`
- `.../supplier/bill.ts`
- `.../supplier/task.ts`
- `.../supplier/business-info.ts`

或者为了目录更清晰，创建目录：

- `src/api/erp/purchase/supplier/contact/index.ts`
- `src/api/erp/purchase/supplier/contract/index.ts`
- `src/api/erp/purchase/supplier/extend-info/index.ts`
- `src/api/erp/purchase/supplier/extend/index.ts`
- `src/api/erp/purchase/supplier/image/index.ts`
- `src/api/erp/purchase/supplier/account/index.ts`
- `src/api/erp/purchase/supplier/bill/index.ts`
- `src/api/erp/purchase/supplier/task/index.ts`
- `src/api/erp/purchase/supplier/business-info/index.ts`

推荐第二种目录式组织，和未来 Tab 组件拆分更匹配。

每个 API 文件提供：

```ts
export namespace ErpSupplierContactApi {
  export interface SupplierContact {
    id?: number;
    supplierId: number;
    // 完整字段
  }
}

export function getSupplierContactList(supplierId: number) {}
export function getSupplierContact(id: number) {}
export function createSupplierContact(data: ErpSupplierContactApi.SupplierContact) {}
export function updateSupplierContact(data: ErpSupplierContactApi.SupplierContact) {}
export function deleteSupplierContact(id: number) {}
```

结构化拓展 API：

```ts
export function getSupplierExtendInfo(supplierId: number) {}
export function saveSupplierExtendInfo(data: ErpSupplierExtendInfoApi.SupplierExtendInfo) {}
```

---

## 6. 实施任务

### Task 1: 创建 SQL 迁移脚本

**Files:**

- Create: `sql/mysql/erp_supplier_base_tables_v20.sql`

- [ ] **Step 1: 编写 9 张表的 `CREATE TABLE IF NOT EXISTS`**

按第 3 节字段清单创建表。所有表必须包含 BaseDO 标准字段，所有 1:N 表必须包含 `supplier_id` 索引。

- [ ] **Step 2: 自查 SQL 字段和计划字段一致**

检查字段名、类型、必填、默认值、注释。重点检查：

- `mobile`、`contract_no`、`contract_date`、`account_name`、`card_no`、`bill_date`、`bill_no`、`amount`、`year`、`month`、`task_amount` 是否 NOT NULL。
- `erp_supplier_extend_info` 是否存在唯一键 `supplier_id, tenant_id, deleted`。
- 所有表是否有 `tenant_id`。

- [ ] **Step 3: 保持脚本只做结构，不插菜单**

该脚本不写 `system_menu`，不插权限。权限先复用 `erp:supplier:*`。

---

### Task 2: 新增 DO 和 Mapper

**Files:**

- Create: 第 4.2 节所有 DO
- Create: 第 4.3 节所有 Mapper

- [ ] **Step 1: 编写 9 个 DO**

每个 DO 按字段清单定义属性，类名与表名一一对应。

- [ ] **Step 2: 编写 9 个 Mapper**

Mapper 使用 `LambdaQueryWrapperX` 实现基础查询方法。

示例模式：

```java
default List<ErpSupplierContactDO> selectListBySupplierId(Long supplierId) {
    return selectList(new LambdaQueryWrapperX<ErpSupplierContactDO>()
            .eq(ErpSupplierContactDO::getSupplierId, supplierId)
            .orderByDesc(ErpSupplierContactDO::getId));
}
```

- [ ] **Step 3: 为账户和任务量补特殊 Mapper 方法**

账户默认值清理：

```java
default void clearDefaultBySupplierId(Long supplierId, Long excludeId) {
    LambdaUpdateWrapper<ErpSupplierAccountDO> updateWrapper = new LambdaUpdateWrapper<ErpSupplierAccountDO>()
            .eq(ErpSupplierAccountDO::getSupplierId, supplierId)
            .set(ErpSupplierAccountDO::getDefaulted, false);
    if (excludeId != null) {
        updateWrapper.ne(ErpSupplierAccountDO::getId, excludeId);
    }
    update(null, updateWrapper);
}
```

任务查重：

```java
default ErpSupplierTaskDO selectByUniqueKey(Long supplierId, Integer year, Integer month, String taskLevel) {
    return selectOne(new LambdaQueryWrapperX<ErpSupplierTaskDO>()
            .eq(ErpSupplierTaskDO::getSupplierId, supplierId)
            .eq(ErpSupplierTaskDO::getYear, year)
            .eq(ErpSupplierTaskDO::getMonth, month)
            .eqIfPresent(ErpSupplierTaskDO::getTaskLevel, taskLevel));
}
```

---

### Task 3: 新增 VO

**Files:**

- Create: 第 4.4 节所有 VO 包和类

- [ ] **Step 1: 为 1:N 表创建 SaveReqVO**

严格落实第 3 节必填项。

示例：

```java
@Schema(description = "管理后台 - ERP 供应商联系人新增/修改 Request VO")
@Data
public class ErpSupplierContactSaveReqVO {
    private Long id;

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotEmpty(message = "姓名不能为空")
    private String name;

    @NotEmpty(message = "移动电话不能为空")
    private String mobile;

    private Long companyId;
}
```

- [ ] **Step 2: 为 1:N 表创建 RespVO**

RespVO 包含全部字段和 `createTime`。

- [ ] **Step 3: 为结构化拓展创建 SaveReqVO / RespVO**

`ErpSupplierExtendInfoSaveReqVO` 只强制 `supplierId`，其他字段非必填。

---

### Task 4: 新增 Service 和 ServiceImpl

**Files:**

- Create: 第 4.5 节所有 Service 和 ServiceImpl
- Modify: `ErrorCodeConstants.java`

- [ ] **Step 1: 添加错误码**

按第 4.7 节新增错误码。

- [ ] **Step 2: 实现通用 CRUD Service**

每个 1:N 子表实现：

- `create`
- `update`
- `delete`
- `get`
- `listBySupplierId`

保存前调用供应商存在校验。

- [ ] **Step 3: 实现结构化拓展 save-upsert**

逻辑：

```java
ErpSupplierExtendInfoDO old = mapper.selectBySupplierId(saveReqVO.getSupplierId());
if (old == null) {
    mapper.insert(BeanUtils.toBean(saveReqVO, ErpSupplierExtendInfoDO.class));
} else {
    ErpSupplierExtendInfoDO updateObj = BeanUtils.toBean(saveReqVO, ErpSupplierExtendInfoDO.class);
    updateObj.setId(old.getId());
    mapper.updateById(updateObj);
}
```

- [ ] **Step 4: 实现默认账户互斥**

在账户 `create` 和 `update` 中：

```java
if (Boolean.TRUE.equals(reqVO.getDefaulted())) {
    supplierAccountMapper.clearDefaultBySupplierId(reqVO.getSupplierId(), reqVO.getId());
}
```

- [ ] **Step 5: 实现任务量月份和唯一性校验**

规则：

- `month < 1 || month > 12` 抛 `SUPPLIER_TASK_MONTH_INVALID`
- 创建时查到同 key 记录抛 `SUPPLIER_TASK_DUPLICATE`
- 更新时查到同 key 记录且 id 不等于当前 id，抛 `SUPPLIER_TASK_DUPLICATE`

---

### Task 5: 新增 Controller

**Files:**

- Create: 第 4.6 节所有 Controller

- [ ] **Step 1: 编写 1:N Controller**

每个 Controller 提供：

```java
@PostMapping("/create")
@PutMapping("/update")
@DeleteMapping("/delete")
@GetMapping("/get")
@GetMapping("/list")
```

`list` 入参：

```java
@RequestParam("supplierId") Long supplierId
```

- [ ] **Step 2: 编写结构化拓展 Controller**

提供：

```java
@GetMapping("/get-by-supplier-id")
@PostMapping("/save")
```

- [ ] **Step 3: 统一权限**

查询：

```java
@PreAuthorize("@ss.hasPermission('erp:supplier:query')")
```

写入：

```java
@PreAuthorize("@ss.hasPermission('erp:supplier:update')")
```

---

### Task 6: 新增前端 API 类型

**Files:**

- Create: 第 5 节所有前端 API 文件

- [ ] **Step 1: 创建目录结构**

推荐创建：

```text
src/api/erp/purchase/supplier/contact/index.ts
src/api/erp/purchase/supplier/contract/index.ts
src/api/erp/purchase/supplier/extend-info/index.ts
src/api/erp/purchase/supplier/extend/index.ts
src/api/erp/purchase/supplier/image/index.ts
src/api/erp/purchase/supplier/account/index.ts
src/api/erp/purchase/supplier/bill/index.ts
src/api/erp/purchase/supplier/task/index.ts
src/api/erp/purchase/supplier/business-info/index.ts
```

- [ ] **Step 2: 每个文件定义 namespace 和 interface**

字段名使用后端 VO 的驼峰命名。

- [ ] **Step 3: 每个文件定义请求函数**

1:N 表：

```ts
getXxxList(supplierId: number)
getXxx(id: number)
createXxx(data)
updateXxx(data)
deleteXxx(id: number)
```

结构化拓展：

```ts
getSupplierExtendInfo(supplierId: number)
saveSupplierExtendInfo(data)
```

---

### Task 7: 后端编译验证

**Files:**

- No file changes

- [ ] **Step 1: 编译 ERP 模块**

Run:

```bash
mvn compile -pl yudao-module-erp -am -DskipTests
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 2: 如出现 MapStruct/Lombok/VO 字段错误，按报错修正**

常见问题：

- DO 字段与 VO 字段拼写不一致
- `LocalTime` import 缺失
- `LambdaUpdateWrapper` import 缺失
- 错误码重复

---

### Task 8: 前端类型检查或轻量验证

**Files:**

- No file changes

- [ ] **Step 1: 检查前端包管理器**

进入：

```bash
cd yudao-ui/yudao-ui-admin-vben/yudao-ui-admin-vben-master
```

检查 `package.json` 脚本，优先选择已有的类型检查或 lint 命令。

- [ ] **Step 2: 运行可用的前端验证**

如果项目支持：

```bash
pnpm lint
```

或：

```bash
pnpm typecheck
```

Expected:

```text
无新增 API 文件导致的 TypeScript 错误
```

如果本地依赖未安装，记录无法运行，不要临时改依赖。

---

## 7. 后续 Tab UI 接入建议

本计划完成后，供应商前端 Tab 页面可以按以下方式接入：

- 入口：供应商列表新增“详情”按钮，而不是继续塞进新增/编辑弹窗。
- 弹窗宽度：供应商详情含多表格，应使用 `class="!w-[85vw]"`。
- 表格高度：每个数据列表 Tab 使用 `:scroll="{ x: 'max-content', y: 'calc(70vh - 220px)' }"`。
- Tab：
  - 基础信息：读取 `getSupplier`
  - 联系人：调用 `supplier/contact`
  - 合同：调用 `supplier/contract`
  - 拓展：调用 `supplier/extend-info` + `supplier/extend`
  - 企业信息：调用 `supplier/image`
  - 供应商账户：调用 `supplier/account`
  - 票据登记：调用 `supplier/bill`
  - 任务量：调用 `supplier/task`
  - 工商信息：调用 `supplier/business-info`

---

## 8. 验收标准

- SQL 脚本包含 9 张供应商基础表，字段、必填项与本计划一致。
- 后端 ERP 模块 `mvn compile -pl yudao-module-erp -am -DskipTests` 成功。
- 每个基础表都有后端 CRUD 或 get/save 接口。
- 所有写接口都会校验供应商存在。
- 供应商账户默认账户互斥生效。
- 供应商任务量月份校验和重复校验生效。
- 前端 API 类型文件完整，后续 Tab 页面无需再补接口路径。

---

## 9. 自查结果

- 文档 Tab 2 至 Tab 9 均有对应数据表或占位表。
- 所有文档标注必填项均已列入 SaveReqVO 校验或 Service 自动补齐规则。
- 工商信息按“后期实现”处理，只做人工维护和接口预留。
- 电子合同按文档说明不实现。
- 本计划没有要求改动客户侧现有代码。
