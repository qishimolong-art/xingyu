package cn.iocoder.yudao.module.erp.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * ERP 错误码枚举类
 * <p>
 * erp 系统，使用 1-030-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== ERP 通用（1-030-000-000） ==========
    ErrorCode ERP_IMPORT_ROW_LIMIT_EXCEEDED = new ErrorCode(1_030_000_002,
            "导入数据超过最大行数限制，当前 {} 行，最多允许 {} 行");
    ErrorCode ATTACHMENT_SCAN_SESSION_NOT_EXISTS = new ErrorCode(1_030_000_100, "扫码上传附件会话不存在或已失效");
    ErrorCode ATTACHMENT_SCAN_EXPIRED = new ErrorCode(1_030_000_101, "扫码上传附件二维码已过期");
    ErrorCode ATTACHMENT_SCAN_CANCELED = new ErrorCode(1_030_000_102, "扫码上传附件会话已取消");
    ErrorCode ATTACHMENT_SCAN_FILE_COUNT_EXCEEDED = new ErrorCode(1_030_000_103,
            "扫码上传附件数量已达到上限，最多允许 {} 个附件");
    ErrorCode ATTACHMENT_SCAN_FILE_TYPE_INVALID = new ErrorCode(1_030_000_104,
            "扫码上传附件仅支持图片、PDF、Word、Excel 文件");
    ErrorCode ATTACHMENT_SCAN_FILE_SIZE_EXCEEDED = new ErrorCode(1_030_000_105,
            "扫码上传附件大小不能超过 {}");
    ErrorCode ATTACHMENT_SCAN_NOT_OWNER = new ErrorCode(1_030_000_106, "只能查看或取消自己创建的扫码上传附件会话");

    // ========== ERP 供应商（1-030-100-000） ==========
    ErrorCode SUPPLIER_NOT_EXISTS = new ErrorCode(1_030_100_000, "供应商不存在");
    ErrorCode SUPPLIER_NOT_ENABLE = new ErrorCode(1_030_100_000, "供应商({})未启用");
    ErrorCode SUPPLIER_DELETE_FAIL_REFERENCED = new ErrorCode(1_030_100_001, "该供应商已被{}引用，无法删除");
    ErrorCode SUPPLIER_CATEGORY_INVALID = new ErrorCode(1_030_100_002, "供应商类别必须是：供应商、既是客户又是供应商");
    ErrorCode SUPPLIER_DISABLE_FAIL_PAYABLE_NOT_CLEAR = new ErrorCode(1_030_100_003, "供应商【{}】仍有未结清欠款【{}】，无法停用");
    ErrorCode SUPPLIER_CODE_DUPLICATE = new ErrorCode(1_030_100_004, "供应商编码({})已存在");
    ErrorCode SUPPLIER_MERGED = new ErrorCode(1_030_100_005, "供应商({})已被合并，不可操作");
    ErrorCode SUPPLIER_NAME_DUPLICATE = new ErrorCode(1_030_100_006, "供应商名称({})已存在");

    // ========== ERP 采购订单（1-030-101-000） ==========
    ErrorCode PURCHASE_ORDER_NOT_EXISTS = new ErrorCode(1_030_101_000, "采购订单不存在");
    ErrorCode PURCHASE_ORDER_DELETE_FAIL_APPROVE = new ErrorCode(1_030_101_001, "采购订单({})已下订，无法删除");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL = new ErrorCode(1_030_101_002, "反下订失败，只有已下订的采购订单才能反下订");
    ErrorCode PURCHASE_ORDER_APPROVE_FAIL = new ErrorCode(1_030_101_003, "下订失败，只有未下订的采购订单才能下订");
    ErrorCode PURCHASE_ORDER_NO_EXISTS = new ErrorCode(1_030_101_004, "生成采购单号失败，请重新提交");
    ErrorCode PURCHASE_ORDER_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_101_005, "采购订单({})已下订，无法修改");
    ErrorCode PURCHASE_ORDER_NOT_APPROVE = new ErrorCode(1_030_101_006, "采购订单未下订，无法操作");
    ErrorCode PURCHASE_ORDER_ITEM_IN_FAIL_PRODUCT_EXCEED = new ErrorCode(1_030_101_007, "采购订单项({})超过最大允许入库数量({})");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN = new ErrorCode(1_030_101_008, "反下订失败，采购订单已进行入库操作");
ErrorCode PURCHASE_ORDER_ITEM_RETURN_FAIL_IN_EXCEED = new ErrorCode(1_030_101_009, "采购订单项({})超过最大允许退货数量({})");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN = new ErrorCode(1_030_101_010, "反下订失败，已存在对应的采购退货单");
    ErrorCode PURCHASE_ORDER_ITEM_GIFT_MODIFY_FAIL_HAS_IN = new ErrorCode(1_030_101_011, "采购订单项({})已有入库记录，不允许修改赠品标记");
    ErrorCode PURCHASE_ORDER_IN_EXCEED_INABLE = new ErrorCode(1_030_101_012, "入库数量超过可入库数量，商品[{}] 可入[{}] 实入[{}]");
    ErrorCode PURCHASE_ORDER_ITEM_COUNT_POSITIVE = new ErrorCode(1_030_101_013, "订货数量不得小于等于 0");
    ErrorCode PURCHASE_ORDER_ITEM_PRICE_POSITIVE = new ErrorCode(1_030_101_014, "订货价格不得小于等于 0");
    ErrorCode PURCHASE_ORDER_ITEM_DUPLICATE = new ErrorCode(1_030_101_015, "同一采购订单明细中产品、仓库、赠品标识重复：{}");
    ErrorCode PURCHASE_ORDER_SUBMIT_SUPPLIER_REQUIRED = new ErrorCode(1_030_101_016, "提交采购订单时供应商不能为空");
    ErrorCode PURCHASE_ORDER_SUBMIT_TIME_REQUIRED = new ErrorCode(1_030_101_017, "提交采购订单时采购时间不能为空");
    ErrorCode PURCHASE_ORDER_SUBMIT_ITEMS_REQUIRED = new ErrorCode(1_030_101_018, "提交采购订单时至少需要一条有效明细");
    ErrorCode PURCHASE_ORDER_UPDATE_FAIL_NOT_DRAFT = new ErrorCode(1_030_101_019, "采购订单({})不是草稿，无法保存草稿修改");
    ErrorCode PURCHASE_ORDER_SUBMIT_FAIL = new ErrorCode(1_030_101_020, "采购订单草稿提交失败，请刷新后重试");
    ErrorCode PURCHASE_ORDER_SUPPLIER_DEPT_NOT_ALLOWED = new ErrorCode(1_030_101_021,
            "\u5f53\u524d\u91c7\u8d2d\u8ba2\u5355\u90e8\u95e8\u4e0d\u5728\u8be5\u4f9b\u5e94\u5546\u53ef\u7528\u90e8\u95e8\u8303\u56f4\u5185");
    ErrorCode PURCHASE_ORDER_ITEM_BATCH_UPDATE_FIELD_REQUIRED = new ErrorCode(1_030_101_022,
            "批量修改采购订单明细时，仓库和部门至少填写一个");
    ErrorCode PURCHASE_ORDER_ITEM_BATCH_UPDATE_ITEM_NOT_EXISTS = new ErrorCode(1_030_101_023,
            "采购订单明细不存在或不属于当前采购订单");
    ErrorCode PURCHASE_ORDER_ITEM_BATCH_UPDATE_FAIL_HAS_IN = new ErrorCode(1_030_101_024,
            "采购订单明细已有入库记录，不能批量修改仓库或部门");
    ErrorCode PURCHASE_ORDER_ITEM_BATCH_UPDATE_FAIL_HAS_RETURN = new ErrorCode(1_030_101_025,
            "采购订单明细已有退货记录，不能批量修改仓库或部门");
    ErrorCode PURCHASE_ORDER_ITEM_BATCH_UPDATE_DEPT_REQUIRED = new ErrorCode(1_030_101_026,
            "目标仓库可用部门不唯一，请同时选择部门");
    ErrorCode PURCHASE_ORDER_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED = new ErrorCode(1_030_101_027,
            "目标仓库不允许使用所选部门");
    ErrorCode PURCHASE_ORDER_ITEM_OPERATION_INVALID = new ErrorCode(1_030_101_028,
            "采购订单明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode PURCHASE_ORDER_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_101_029,
            "采购订单明细不存在或不属于当前采购订单");

    // ========== ERP 采购入库（1-030-102-000） ==========
    ErrorCode PURCHASE_IN_NOT_EXISTS = new ErrorCode(1_030_102_000, "采购入库单不存在");
    ErrorCode PURCHASE_IN_DELETE_FAIL_APPROVE = new ErrorCode(1_030_102_001, "采购入库单({})已审核，无法删除");
    ErrorCode PURCHASE_IN_PROCESS_FAIL = new ErrorCode(1_030_102_002, "反审核失败，只有已审核的入库单才能反审核");
    ErrorCode PURCHASE_IN_APPROVE_FAIL = new ErrorCode(1_030_102_003, "审核失败，只有未审核的入库单才能审核");
    ErrorCode PURCHASE_IN_NO_EXISTS = new ErrorCode(1_030_102_004, "生成入库单失败，请重新提交");
    ErrorCode PURCHASE_IN_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_102_005, "采购入库单({})已审核，无法修改");
    ErrorCode PURCHASE_IN_NOT_APPROVE = new ErrorCode(1_030_102_006, "采购入库单未审核，无法操作");
    ErrorCode PURCHASE_IN_FAIL_PAYMENT_PRICE_EXCEED = new ErrorCode(1_030_102_007, "付款金额({})超过采购入库单总金额({})");
    ErrorCode PURCHASE_IN_PROCESS_FAIL_EXISTS_PAYMENT = new ErrorCode(1_030_102_008, "反审核失败，已存在对应的付款单");
    ErrorCode PURCHASE_IN_ITEM_COUNT_POSITIVE = new ErrorCode(1_030_102_015, "入库数量不得小于等于 0");
    ErrorCode PURCHASE_IN_ITEM_PRICE_POSITIVE = new ErrorCode(1_030_102_016,
            "非赠品入库价格必须大于 0，赠品入库价格必须为 0");
    ErrorCode PURCHASE_IN_ITEM_DUPLICATE = new ErrorCode(1_030_102_017, "同一采购入库明细中产品、仓库、赠品标识重复：{}");
    ErrorCode PURCHASE_IN_TRANSFER_OUT_ITEMS_EMPTY = new ErrorCode(1_030_102_018, "当前采购入库单无可调拨明细");
    ErrorCode PURCHASE_IN_TRANSFER_OUT_SOURCE_ITEM_NOT_EXISTS = new ErrorCode(1_030_102_019, "来源采购入库明细不存在或不属于当前入库单");
    ErrorCode PURCHASE_IN_TRANSFER_OUT_COUNT_POSITIVE = new ErrorCode(1_030_102_020, "调拨数量必须大于 0");
    ErrorCode PURCHASE_IN_TRANSFER_OUT_EXCEED_AVAILABLE = new ErrorCode(1_030_102_021, "入库明细[{}]本次调拨数量({})超过可调拨数量({})");
    ErrorCode PURCHASE_IN_UPDATE_FAIL_NOT_DRAFT = new ErrorCode(1_030_102_022, "采购入库单({})不是草稿，无法保存草稿修改");
    ErrorCode PURCHASE_IN_SUBMIT_FAIL = new ErrorCode(1_030_102_023, "采购入库草稿提交失败，请刷新后重试");
    ErrorCode PURCHASE_IN_SUBMIT_ITEMS_REQUIRED = new ErrorCode(1_030_102_024, "提交采购入库草稿时至少需要一条有效明细");
    ErrorCode PURCHASE_IN_SUBMIT_SUPPLIER_REQUIRED = new ErrorCode(1_030_102_025, "提交采购入库草稿时供应商不能为空");
    ErrorCode PURCHASE_IN_SUBMIT_TIME_REQUIRED = new ErrorCode(1_030_102_026, "提交采购入库草稿时入库时间不能为空");
    ErrorCode PURCHASE_IN_SUPPLIER_DEPT_NOT_ALLOWED = new ErrorCode(1_030_102_027,
            "当前采购入库部门不在该供应商可用部门范围内");
    ErrorCode PURCHASE_IN_ITEM_BATCH_UPDATE_FIELD_REQUIRED = new ErrorCode(1_030_102_028,
            "批量修改采购入库明细时，仓库和部门至少填写一个");
    ErrorCode PURCHASE_IN_ITEM_BATCH_UPDATE_ITEM_NOT_EXISTS = new ErrorCode(1_030_102_029,
            "采购入库明细不存在或不属于当前采购入库单");
    ErrorCode PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_102_030,
            "采购入库单({})已审核，不能批量修改明细仓库或部门");
    ErrorCode PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_RETURN = new ErrorCode(1_030_102_031,
            "采购入库明细已有退货记录，不能批量修改仓库或部门");
    ErrorCode PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_TRANSFER_OUT = new ErrorCode(1_030_102_032,
            "采购入库明细已有调拨出库记录，不能批量修改仓库或部门");
    ErrorCode PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_SALE_CART = new ErrorCode(1_030_102_033,
            "采购入库明细已转销售手推车，不能批量修改仓库或部门");
    ErrorCode PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_INVOICE = new ErrorCode(1_030_102_034,
            "采购入库单已开票，不能批量修改明细仓库或部门");
    ErrorCode PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_STOCK_IN_BILL = new ErrorCode(1_030_102_035,
            "采购入库明细已生成入仓单，不能批量修改仓库或部门");
    ErrorCode PURCHASE_IN_ITEM_BATCH_UPDATE_FAIL_HAS_ADJUST = new ErrorCode(1_030_102_036,
            "采购入库明细已调价，不能批量修改仓库或部门");
    ErrorCode PURCHASE_IN_ITEM_BATCH_UPDATE_DEPT_REQUIRED = new ErrorCode(1_030_102_037,
            "目标仓库可用部门不唯一，请同时选择部门");
    ErrorCode PURCHASE_IN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED = new ErrorCode(1_030_102_038,
            "目标仓库不允许使用所选部门");
    ErrorCode PURCHASE_IN_DATA_PERMISSION_DENIED = new ErrorCode(1_030_102_039, "没有该数据权限");
    ErrorCode PURCHASE_IN_ITEM_OPERATION_INVALID = new ErrorCode(1_030_102_040,
            "采购入库明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode PURCHASE_IN_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_102_041,
            "采购入库明细不存在或不属于当前采购入库单");
    ErrorCode PURCHASE_IN_FREIGHT_TYPE_INVALID = new ErrorCode(1_030_102_042,
            "采购入库运费类型只能是：代厂家付、我方自付");
    ErrorCode PURCHASE_IN_FREIGHT_TYPE_REQUIRED = new ErrorCode(1_030_102_043,
            "填写运费时必须选择运费类型");
    ErrorCode PURCHASE_IN_FREIGHT_AMOUNT_NEGATIVE = new ErrorCode(1_030_102_044,
            "采购入库运费不能小于 0");

    // ========== ERP 采购退货（1-030-103-000） ==========
    ErrorCode PURCHASE_RETURN_NOT_EXISTS = new ErrorCode(1_030_103_000, "采购退货单不存在");
    ErrorCode PURCHASE_RETURN_DELETE_FAIL_APPROVE = new ErrorCode(1_030_103_001, "采购退货单({})已审核，无法删除");
    ErrorCode PURCHASE_RETURN_PROCESS_FAIL = new ErrorCode(1_030_103_002, "反审核失败，只有已审核的退货单才能反审核");
    ErrorCode PURCHASE_RETURN_APPROVE_FAIL = new ErrorCode(1_030_103_003, "审核失败，只有未审核的退货单才能审核");
    ErrorCode PURCHASE_RETURN_NO_EXISTS = new ErrorCode(1_030_103_004, "生成退货单失败，请重新提交");
    ErrorCode PURCHASE_RETURN_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_103_005, "采购退货单({})已审核，无法修改");
    ErrorCode PURCHASE_RETURN_NOT_APPROVE = new ErrorCode(1_030_103_006, "采购退货单未审核，无法操作");
    ErrorCode PURCHASE_RETURN_FAIL_REFUND_PRICE_EXCEED = new ErrorCode(1_030_103_007, "退款金额({})超过采购退货单总金额({})");
    ErrorCode PURCHASE_RETURN_PROCESS_FAIL_EXISTS_REFUND = new ErrorCode(1_030_103_008, "反审核失败，已存在对应的退款单");
    ErrorCode PURCHASE_RETURN_MODE_INVALID = new ErrorCode(1_030_103_009, "采购退货模式不合法");
    ErrorCode PURCHASE_RETURN_BY_ORDER_SOURCE_REQUIRED = new ErrorCode(1_030_103_010, "按单退货时必须指定原入库项");
    ErrorCode PURCHASE_RETURN_SOURCE_IN_ITEM_NOT_EXISTS = new ErrorCode(1_030_103_011, "原采购入库项不存在");
    ErrorCode PURCHASE_RETURN_EXCEED_RETURNABLE = new ErrorCode(1_030_103_012, "退货数量超过可退数量，入库项[{}] 可退[{}] 但尝试退[{}]");
    ErrorCode PURCHASE_RETURN_COUNT_POSITIVE = new ErrorCode(1_030_103_013, "退货数量必须大于 0");
    ErrorCode PURCHASE_RETURN_SUPPLIER_REQUIRED = new ErrorCode(1_030_103_014, "供应商不能为空");
    ErrorCode PURCHASE_RETURN_ITEM_COUNT_POSITIVE = new ErrorCode(1_030_103_015, "退货数量不得小于等于 0");
    ErrorCode PURCHASE_RETURN_ITEM_PRICE_POSITIVE = new ErrorCode(1_030_103_016, "退货价格不得小于等于 0");
    ErrorCode PURCHASE_RETURN_ITEM_DUPLICATE = new ErrorCode(1_030_103_017, "同一采购退货明细中产品、仓库、赠品标识重复：{}");
    ErrorCode PURCHASE_RETURN_ITEMS_REQUIRED = new ErrorCode(1_030_103_029, "采购退货明细不能为空");
    ErrorCode PURCHASE_RETURN_UPDATE_FAIL_NOT_DRAFT = new ErrorCode(1_030_103_018, "采购退货单({})不是草稿，无法保存草稿修改");
    ErrorCode PURCHASE_RETURN_SUBMIT_FAIL = new ErrorCode(1_030_103_019, "采购退货草稿提交失败，请刷新后重试");
    ErrorCode PURCHASE_RETURN_SUBMIT_ITEMS_REQUIRED = new ErrorCode(1_030_103_020, "提交采购退货草稿时至少需要一条有效明细");
    ErrorCode PURCHASE_RETURN_SUBMIT_SUPPLIER_REQUIRED = new ErrorCode(1_030_103_021, "提交采购退货草稿时供应商不能为空");
    ErrorCode PURCHASE_RETURN_SUBMIT_TIME_REQUIRED = new ErrorCode(1_030_103_022, "提交采购退货草稿时退货时间不能为空");
    ErrorCode PURCHASE_RETURN_ITEM_BATCH_UPDATE_FIELD_REQUIRED = new ErrorCode(1_030_103_023, "请至少选择仓库或部门");
    ErrorCode PURCHASE_RETURN_ITEM_BATCH_UPDATE_ITEM_NOT_EXISTS = new ErrorCode(1_030_103_024, "所选采购退货明细不存在");
    ErrorCode PURCHASE_RETURN_ITEM_BATCH_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_103_025, "采购退货单({})已审核，无法批量修改明细仓库/部门");
    ErrorCode PURCHASE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_NOT_ALLOWED_BY_ORDER = new ErrorCode(1_030_103_026, "按入库单退货的明细已关联原入库项，不能批量修改仓库");
    ErrorCode PURCHASE_RETURN_ITEM_BATCH_UPDATE_DEPT_REQUIRED = new ErrorCode(1_030_103_027, "目标仓库存在多个可用部门，请选择部门");
    ErrorCode PURCHASE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED = new ErrorCode(1_030_103_028, "所选部门不属于目标仓库可用部门");
    ErrorCode PURCHASE_RETURN_ITEM_OPERATION_INVALID = new ErrorCode(1_030_103_029,
            "采购退货明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode PURCHASE_RETURN_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_103_030,
            "采购退货明细不存在或不属于当前采购退货单");

    // ========== ERP 采购票据（1-030-104-000） ==========
    ErrorCode PURCHASE_INVOICE_NOT_EXISTS = new ErrorCode(1_030_104_000, "采购票据不存在");
    ErrorCode PURCHASE_INVOICE_DELETE_FAIL_APPROVE = new ErrorCode(1_030_104_001, "采购票据({})已审核，无法删除");
    ErrorCode PURCHASE_INVOICE_PROCESS_FAIL = new ErrorCode(1_030_104_002, "反审核失败，只有已审核的采购票据才能反审核");
    ErrorCode PURCHASE_INVOICE_APPROVE_FAIL = new ErrorCode(1_030_104_003, "审核失败，只有未审核的采购票据才能审核");
    ErrorCode PURCHASE_INVOICE_NO_EXISTS = new ErrorCode(1_030_104_004, "生成采购票据单号失败，请重新提交");
    ErrorCode PURCHASE_INVOICE_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_104_005, "采购票据({})已审核，无法修改");
    ErrorCode PURCHASE_INVOICE_NOT_APPROVE = new ErrorCode(1_030_104_006, "采购票据未审核，无法操作");
    ErrorCode PURCHASE_INVOICE_ITEM_EMPTY = new ErrorCode(1_030_104_007, "采购票据明细不能为空");
    ErrorCode PURCHASE_INVOICE_ITEM_COUNT_POSITIVE = new ErrorCode(1_030_104_008, "票据明细数量必须大于 0");
    ErrorCode PURCHASE_INVOICE_ITEM_PRICE_NEGATIVE = new ErrorCode(1_030_104_009, "票据明细单价不能小于 0");
    ErrorCode PURCHASE_INVOICE_SUPPLIER_REQUIRED = new ErrorCode(1_030_104_010, "供应商不能为空");
    ErrorCode PURCHASE_INVOICE_PROCESS_NOT_SUPPORT = new ErrorCode(1_030_104_011, "采购票据不支持反审");
    ErrorCode PURCHASE_INVOICE_SOURCE_IN_INVOICED = new ErrorCode(1_030_104_012, "入库单({})已开票，无法重复开票");
    ErrorCode PURCHASE_INVOICE_UPDATE_FAIL_NOT_DRAFT = new ErrorCode(1_030_104_013,
            "采购票据({})不是草稿，无法保存草稿修改");
    ErrorCode PURCHASE_INVOICE_SUBMIT_FAIL = new ErrorCode(1_030_104_014,
            "采购票据草稿提交失败，请刷新后重试");
    ErrorCode PURCHASE_INVOICE_SUBMIT_DATE_REQUIRED = new ErrorCode(1_030_104_015,
            "提交采购票据草稿时开票日期不能为空");
    ErrorCode PURCHASE_INVOICE_SUBMIT_TYPE_REQUIRED = new ErrorCode(1_030_104_016,
            "提交采购票据草稿时票据类型不能为空");
    ErrorCode PURCHASE_INVOICE_SUBMIT_NO_REQUIRED = new ErrorCode(1_030_104_017,
            "提交采购票据草稿时发票号不能为空");
    ErrorCode PURCHASE_INVOICE_ITEM_OPERATION_INVALID = new ErrorCode(1_030_104_018,
            "采购票据明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode PURCHASE_INVOICE_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_104_019,
            "采购票据明细不存在或不属于当前采购票据");
    ErrorCode PURCHASE_INVOICE_OCR_BATCH_NOT_EXISTS = new ErrorCode(1_030_104_100,
            "采购发票识别批次不存在");
    ErrorCode PURCHASE_INVOICE_OCR_APP_CODE_NOT_CONFIGURED = new ErrorCode(1_030_104_101,
            "采购发票识别未配置 AppCode 或 AppKey/AppSecret，请配置 yudao.erp.purchase-invoice-ocr.app-code 或 app-key/app-secret");
    ErrorCode PURCHASE_INVOICE_OCR_RECOGNIZE_STATUS_NOT_SUPPORT = new ErrorCode(1_030_104_102,
            "采购发票识别批次当前状态不支持重新识别");
    ErrorCode PURCHASE_INVOICE_OCR_MATCH_STATUS_NOT_SUPPORT = new ErrorCode(1_030_104_103,
            "采购发票识别批次当前状态不支持匹配");
    ErrorCode PURCHASE_INVOICE_OCR_NO_MATCHABLE_ITEMS = new ErrorCode(1_030_104_104,
            "采购发票识别批次没有可匹配的发票明细");
    ErrorCode PURCHASE_INVOICE_OCR_CONFIRM_STATUS_NOT_SUPPORT = new ErrorCode(1_030_104_105,
            "采购发票识别批次当前状态不支持确认生成采购票据");
    ErrorCode PURCHASE_INVOICE_OCR_NO_CONFIRMED_ITEMS = new ErrorCode(1_030_104_106,
            "采购发票识别批次没有可确认生成采购票据的明细");
    ErrorCode PURCHASE_INVOICE_OCR_CONFIRM_PURCHASE_IN_CHANGED = new ErrorCode(1_030_104_107,
            "厂家单号({})匹配的采购入库单已变化或不可开票，请重新匹配");
    ErrorCode PURCHASE_INVOICE_OCR_CONFIRM_SUPPLIER_NOT_UNIQUE = new ErrorCode(1_030_104_108,
            "厂家单号({})匹配到多个供应商，无法自动生成采购票据");
    ErrorCode PURCHASE_INVOICE_OCR_CONFIRM_INVOICE_FIELD_REQUIRED = new ErrorCode(1_030_104_109,
            "厂家单号({})缺少发票日期、发票类型或发票号，无法自动生成采购票据");
    ErrorCode PURCHASE_INVOICE_OCR_ITEM_NOT_EXISTS = new ErrorCode(1_030_104_110,
            "采购发票识别明细不存在");
    ErrorCode PURCHASE_INVOICE_OCR_UPDATE_FACTORY_ORDER_NO_NOT_SUPPORT = new ErrorCode(1_030_104_111,
            "采购发票识别明细当前状态不支持修改厂家单号");
    ErrorCode PURCHASE_INVOICE_OCR_FACTORY_ORDER_NO_INVALID = new ErrorCode(1_030_104_112,
            "厂家单号必须以 S 开头");

    // ========== ERP 客户（1-030-200-000）==========
    ErrorCode CUSTOMER_NOT_EXISTS = new ErrorCode(1_020_200_000, "客户不存在");
    ErrorCode CUSTOMER_NOT_ENABLE = new ErrorCode(1_020_200_001, "客户({})未启用");
    ErrorCode CUSTOMER_DELETE_FAIL_REFERENCED = new ErrorCode(1_020_200_002, "该客户已被{}引用，无法删除");
    ErrorCode CUSTOMER_DISABLE_FAIL_RECEIVABLE_NOT_CLEAR = new ErrorCode(1_020_200_003, "客户【{}】仍有未结清应收账款【{}】，无法停用");
    ErrorCode CUSTOMER_CODE_DUPLICATE = new ErrorCode(1_020_200_004, "客户编码({})已存在");
    ErrorCode CUSTOMER_MERGED = new ErrorCode(1_020_200_005, "客户({})已被合并，不可操作");
    ErrorCode CUSTOMER_NAME_DUPLICATE = new ErrorCode(1_020_200_022, "客户名称({})已存在");
    ErrorCode CUSTOMER_CREDIT_CONFIG_REQUIRED = new ErrorCode(1_020_200_006, "开启白条授信后，授信金额和授信期限至少填写一项");
    ErrorCode CUSTOMER_CREDIT_VALUE_INVALID = new ErrorCode(1_020_200_007, "白条授信金额和授信期限不能小于 0");
    ErrorCode CUSTOMER_CREDIT_BLOCKED = new ErrorCode(1_020_200_008, "客户({})已超过白条授信限制，不能继续选择：{}");
    ErrorCode CUSTOMER_SALE_DEPT_NOT_ALLOWED = new ErrorCode(1_020_200_009, "客户未分配给当前报价部门，不能保存报价订单");
    ErrorCode CUSTOMER_DEPT_CREDIT_DEPT_NOT_ALLOWED = new ErrorCode(1_020_200_010, "部门不在该客户可用部门范围内，不能配置授信");
    ErrorCode CUSTOMER_DEPT_CREDIT_DUPLICATE_DEPT = new ErrorCode(1_020_200_011, "客户分部门授信中存在重复部门");
    ErrorCode CUSTOMER_MEMBER_NOT_EXISTS = new ErrorCode(1_020_200_012, "客户小程序授权关系不存在");
    ErrorCode CUSTOMER_MEMBER_DUPLICATE = new ErrorCode(1_020_200_013, "该客户已绑定该小程序账号");
    ErrorCode CUSTOMER_MEMBER_USER_BOUND = new ErrorCode(1_020_200_014, "该小程序账号已授权给客户【{}】");
    ErrorCode CUSTOMER_MEMBER_CUSTOMER_NOT_ENABLE = new ErrorCode(1_020_200_015, "客户已停用，不能启用小程序授权");
    ErrorCode CUSTOMER_MEMBER_USER_NOT_EXISTS = new ErrorCode(1_020_200_016, "小程序会员用户不存在");
    ErrorCode CUSTOMER_MEMBER_USER_NOT_ENABLE = new ErrorCode(1_020_200_017, "小程序会员用户已停用，不能启用客户授权");
    ErrorCode CUSTOMER_MEMBER_AUTH_REQUIRED = new ErrorCode(1_020_200_018, "当前账号暂未开通客户授权，请联系业务人员开通后再操作");
    ErrorCode CUSTOMER_MEMBER_DEPT_REQUIRED = new ErrorCode(1_020_200_019, "请选择部门");
    ErrorCode CUSTOMER_MEMBER_DEPT_NOT_ALLOWED = new ErrorCode(1_020_200_020, "当前账号无权使用所选部门");
    ErrorCode SALE_PRICE_LEVEL_PERMISSION_DENIED = new ErrorCode(1_020_200_021, "当前用户没有使用该价格体系的权限");
    ErrorCode CUSTOMER_CONTACT_NOT_EXISTS = new ErrorCode(1_030_207_000, "客户联系人不存在");
    ErrorCode CUSTOMER_CONTRACT_NOT_EXISTS = new ErrorCode(1_030_207_001, "客户合同不存在");
    ErrorCode CUSTOMER_IMAGE_NOT_EXISTS = new ErrorCode(1_030_207_002, "客户图片不存在");
    ErrorCode CUSTOMER_TASK_NOT_EXISTS = new ErrorCode(1_030_207_003, "客户任务量不存在");
    ErrorCode CUSTOMER_AREA_NOT_EXISTS = new ErrorCode(1_030_207_004, "客户企业地区不存在");
    ErrorCode CUSTOMER_EXTEND_NOT_EXISTS = new ErrorCode(1_030_207_005, "客户拓展信息不存在");
    ErrorCode CUSTOMER_BUSINESS_INFO_NOT_EXISTS = new ErrorCode(1_030_207_006, "客户工商信息不存在");

    // ========== ERP 销售订单（1-030-201-000） ==========
    ErrorCode SALE_ORDER_NOT_EXISTS = new ErrorCode(1_020_201_000, "销售订单不存在");
    ErrorCode SALE_ORDER_DELETE_FAIL_APPROVE = new ErrorCode(1_020_201_001, "销售订单({})已审核，无法删除");
    ErrorCode SALE_ORDER_PROCESS_FAIL = new ErrorCode(1_020_201_002, "反审核失败，只有已审核的销售订单才能反审核");
    ErrorCode SALE_ORDER_APPROVE_FAIL = new ErrorCode(1_020_201_003, "审核失败，只有未审核的销售订单才能审核");
    ErrorCode SALE_ORDER_NO_EXISTS = new ErrorCode(1_020_201_004, "生成销售单号失败，请重新提交");
    ErrorCode SALE_ORDER_UPDATE_FAIL_APPROVE = new ErrorCode(1_020_201_005, "销售订单({})已审核，无法修改");
    ErrorCode SALE_ORDER_NOT_APPROVE = new ErrorCode(1_020_201_006, "销售订单未审核，无法操作");
    ErrorCode SALE_ORDER_ITEM_OUT_FAIL_PRODUCT_EXCEED = new ErrorCode(1_020_201_007, "销售订单项({})超过最大允许出库数量({})");
    ErrorCode SALE_ORDER_PROCESS_FAIL_EXISTS_OUT = new ErrorCode(1_020_201_008, "反审核失败，已存在对应的销售出库单");
    ErrorCode SALE_ORDER_ITEM_RETURN_FAIL_OUT_EXCEED = new ErrorCode(1_020_201_009, "销售订单项({})超过最大允许退货数量({})");
    ErrorCode SALE_ORDER_PROCESS_FAIL_EXISTS_RETURN = new ErrorCode(1_020_201_010, "反审核失败，已存在对应的销售退货单");
    ErrorCode SALE_ORDER_ITEM_OPERATION_INVALID = new ErrorCode(1_020_201_011,
            "销售订单明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode SALE_ORDER_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_020_201_012,
            "销售订单明细不存在或不属于当前销售订单");
    ErrorCode SALE_ORDER_ITEM_EMPTY = new ErrorCode(1_020_201_013, "销售订单明细不能为空");

    // ========== ERP 销售出库（1-030-202-000） ==========
    ErrorCode SALE_OUT_NOT_EXISTS = new ErrorCode(1_020_202_000, "销售出库单不存在");
    ErrorCode SALE_OUT_DELETE_FAIL_APPROVE = new ErrorCode(1_020_202_001, "销售出库单({})已审核，无法删除");
    ErrorCode SALE_OUT_PROCESS_FAIL = new ErrorCode(1_020_202_002, "反审核失败，只有已审核的出库单才能反审核");
    ErrorCode SALE_OUT_APPROVE_FAIL = new ErrorCode(1_020_202_003, "审核失败，只有未审核的出库单才能审核");
    ErrorCode SALE_OUT_NO_EXISTS = new ErrorCode(1_020_202_004, "生成出库单失败，请重新提交");
    ErrorCode SALE_OUT_UPDATE_FAIL_APPROVE = new ErrorCode(1_020_202_005, "销售出库单({})已审核，无法修改");
    ErrorCode SALE_OUT_NOT_APPROVE = new ErrorCode(1_020_202_006, "销售出库单未审核，无法操作");
    ErrorCode SALE_OUT_FAIL_RECEIPT_PRICE_EXCEED = new ErrorCode(1_020_202_007, "收款金额({})超过销售出库单总金额({})");
    ErrorCode SALE_OUT_PROCESS_FAIL_EXISTS_RECEIPT = new ErrorCode(1_020_202_008, "反审核失败，已存在对应的收款单");
    ErrorCode SALE_OUT_EXPRESS_FILE_EMPTY = new ErrorCode(1_020_202_009, "快递单图片不能为空");
    ErrorCode SALE_OUT_EXPRESS_FILE_TYPE_INVALID = new ErrorCode(1_020_202_010, "快递单仅支持 jpg、jpeg、png 格式");
    ErrorCode SALE_OUT_EXPRESS_FILE_SIZE_EXCEEDED = new ErrorCode(1_020_202_011, "快递单图片不能超过 5MB");
    ErrorCode SALE_OUT_ITEM_OPERATION_INVALID = new ErrorCode(1_020_202_012,
            "销售出库明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode SALE_OUT_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_020_202_013,
            "销售出库明细不存在或不属于当前销售出库单");
    ErrorCode SALE_PICK_DELIVERY_ORDER_NOT_EXISTS = new ErrorCode(1_030_208_000, "销售拣货送货单不存在");
    ErrorCode SALE_PICK_TASK_NOT_EXISTS = new ErrorCode(1_030_208_001, "销售拣货单不存在");
    ErrorCode SALE_PICK_DELIVERY_ITEM_REQUIRED = new ErrorCode(1_030_208_002, "请至少选择一条本次提交明细");
    ErrorCode SALE_PICK_DELIVERY_FILE_REQUIRED = new ErrorCode(1_030_208_003, "请至少上传一张本次提交凭证");
    ErrorCode SALE_PICK_TASK_WAREHOUSE_PERMISSION_DENIED = new ErrorCode(1_030_208_004, "当前拣货员未负责该仓库，不能操作该拣货单");
    ErrorCode SALE_PICK_TASK_STATUS_INVALID = new ErrorCode(1_030_208_005, "当前拣货单状态不允许继续拣货");
    ErrorCode SALE_PICK_ITEM_INVALID = new ErrorCode(1_030_208_006, "所选拣货明细不存在、已拣货或不属于当前拣货单");
    ErrorCode SALE_DELIVERY_NOT_READY = new ErrorCode(1_030_208_007, "该销售单尚未完成拣货，不能送货");
    ErrorCode SALE_DELIVERY_STATUS_INVALID = new ErrorCode(1_030_208_008, "当前送货单状态不允许继续送货");
    ErrorCode SALE_DELIVERY_ITEM_INVALID = new ErrorCode(1_030_208_009, "所选送货明细不存在、未完成拣货、已送货或不属于当前送货单");
    ErrorCode SALE_PICK_DELIVERY_WAREHOUSE_REQUIRED = new ErrorCode(1_030_208_010, "销售单明细缺少仓库，无法生成拣货单");
    ErrorCode SALE_PICK_DELIVERY_VOUCHER_FILE_EMPTY = new ErrorCode(1_030_208_011, "凭证图片不能为空");
    ErrorCode SALE_PICK_DELIVERY_VOUCHER_FILE_TYPE_INVALID = new ErrorCode(1_030_208_012, "凭证图片仅支持 jpg、jpeg、png 格式");
    ErrorCode SALE_PICK_DELIVERY_VOUCHER_FILE_SIZE_EXCEEDED = new ErrorCode(1_030_208_013, "凭证图片不能超过 5MB");

    // ========== ERP 销售退货（1-030-203-000） ==========
    ErrorCode SALE_RETURN_NOT_EXISTS = new ErrorCode(1_020_203_000, "销售退货单不存在");
    ErrorCode SALE_RETURN_DELETE_FAIL_APPROVE = new ErrorCode(1_020_203_001, "销售退货单({})已审核，无法删除");
    ErrorCode SALE_RETURN_PROCESS_FAIL = new ErrorCode(1_020_203_002, "反审核失败，只有已审核的退货单才能反审核");
    ErrorCode SALE_RETURN_APPROVE_FAIL = new ErrorCode(1_020_203_003, "审核失败，只有未审核的退货单才能审核");
    ErrorCode SALE_RETURN_NO_EXISTS = new ErrorCode(1_020_203_004, "生成退货单失败，请重新提交");
    ErrorCode SALE_RETURN_UPDATE_FAIL_APPROVE = new ErrorCode(1_020_203_005, "销售退货单({})已审核，无法修改");
    ErrorCode SALE_RETURN_NOT_APPROVE = new ErrorCode(1_020_203_006, "销售退货单未审核，无法操作");
    ErrorCode SALE_RETURN_FAIL_REFUND_PRICE_EXCEED = new ErrorCode(1_020_203_007, "退款金额({})超过销售退货单总金额({})");
    ErrorCode SALE_RETURN_PROCESS_FAIL_EXISTS_REFUND = new ErrorCode(1_020_203_008, "反审核失败，已存在对应的退款单");
    ErrorCode SALE_RETURN_MODE_INVALID = new ErrorCode(1_020_203_009, "销售退货模式不正确");
    ErrorCode SALE_RETURN_BY_SALE_OUT_SOURCE_REQUIRED = new ErrorCode(1_020_203_010, "按销售单退货时，销售单和销售单明细不能为空");
    ErrorCode SALE_RETURN_SOURCE_OUT_ITEM_NOT_EXISTS = new ErrorCode(1_020_203_011, "销售单明细不存在：{}");
    ErrorCode SALE_RETURN_EXCEED_RETURNABLE = new ErrorCode(1_020_203_012, "销售单明细({})本次退货数量({})超过可退数量({})");
    ErrorCode SALE_RETURN_COUNT_POSITIVE = new ErrorCode(1_020_203_013, "销售退货数量必须大于 0");
    ErrorCode SALE_RETURN_BY_STOCK_CUSTOMER_REQUIRED = new ErrorCode(1_020_203_014, "按库存退货时，客户不能为空");
    ErrorCode SALE_RETURN_ITEMS_EMPTY = new ErrorCode(1_020_203_015, "销售退货明细不能为空");
    ErrorCode SALE_RETURN_ITEM_PRODUCT_REQUIRED = new ErrorCode(1_020_203_016, "销售退货明细产品不能为空");
    ErrorCode SALE_RETURN_ITEM_PRICE_REQUIRED = new ErrorCode(1_020_203_017, "销售退货明细产品单价不能为空且不能小于 0");
    ErrorCode SALE_RETURN_UPDATE_FAIL_NOT_DRAFT = new ErrorCode(1_020_203_018, "销售退货单({})不是草稿，无法保存草稿修改");
    ErrorCode SALE_RETURN_SUBMIT_FAIL = new ErrorCode(1_020_203_019, "销售退货草稿提交失败，请刷新后重试");
    ErrorCode SALE_RETURN_SUBMIT_ITEMS_REQUIRED = new ErrorCode(1_020_203_020, "销售退货草稿至少需要一条有效明细才能提交");
    ErrorCode SALE_RETURN_DRAFT_ITEMS_REQUIRED = new ErrorCode(1_020_203_021, "生成销售退货草稿时至少需要一条有效明细");
    ErrorCode SALE_RETURN_ITEM_BATCH_UPDATE_EMPTY = new ErrorCode(1_020_203_022, "请至少选择仓库或部门");
    ErrorCode SALE_RETURN_ITEM_BATCH_UPDATE_NOT_EXISTS = new ErrorCode(1_020_203_023, "销售退货明细不存在或不属于当前单据：{}");
    ErrorCode SALE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED = new ErrorCode(1_020_203_024, "所选部门不属于目标仓库可用销售部门");
    ErrorCode SALE_RETURN_ITEM_BATCH_UPDATE_FIELD_DENIED = new ErrorCode(1_020_203_025, "当前用户没有批量修改该明细字段的权限");
    ErrorCode SALE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_READONLY = new ErrorCode(1_020_203_026, "来源绑定销售退货不允许批量修改明细仓库");
    ErrorCode SALE_RETURN_ITEM_DUPLICATE = new ErrorCode(1_020_203_027, "同一销售退货明细中产品、仓库重复：{}");
    ErrorCode SALE_RETURN_TRANSFER_ITEMS_EMPTY = new ErrorCode(1_020_203_028, "当前销售退货单无可转明细");
    ErrorCode SALE_RETURN_TRANSFER_SOURCE_ITEM_NOT_EXISTS = new ErrorCode(1_020_203_029, "来源销售退货明细不存在或不属于当前单据");
    ErrorCode SALE_RETURN_TRANSFER_COUNT_POSITIVE = new ErrorCode(1_020_203_030, "转单数量必须大于 0");
    ErrorCode SALE_RETURN_TRANSFER_EXCEED_AVAILABLE = new ErrorCode(1_020_203_031, "销售退货明细[{}]本次转单数量({})超过可转数量({})");
    ErrorCode SALE_RETURN_TRANSFER_WAREHOUSE_SAME = new ErrorCode(1_020_203_032, "调出仓库和调入仓库不能相同");
    ErrorCode SALE_RETURN_TRANSFER_SUPPLIER_REQUIRED = new ErrorCode(1_020_203_033, "转采购退货时供应商不能为空");
    ErrorCode SALE_RETURN_TRANSFER_DEPT_REQUIRED = new ErrorCode(1_020_203_034, "转采购退货时部门不能为空");
    ErrorCode SALE_RETURN_TRANSFER_SOURCE_CHANGED = new ErrorCode(1_020_203_035, "来源销售退货明细已绑定，不能修改来源商品身份");
    ErrorCode SALE_RETURN_ITEM_OPERATION_INVALID = new ErrorCode(1_020_203_036,
            "销售退货明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode SALE_RETURN_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_020_203_037,
            "销售退货明细不存在或不属于当前销售退货单");

    // ========== ERP 仓库 1-030-400-000 ==========
    ErrorCode WAREHOUSE_NOT_EXISTS = new ErrorCode(1_030_400_000, "仓库不存在");
    ErrorCode WAREHOUSE_NOT_ENABLE = new ErrorCode(1_030_400_001, "仓库({})未启用");
    ErrorCode WAREHOUSE_DELETE_FAIL_REFERENCED = new ErrorCode(1_030_400_002, "该仓库已被{}引用，无法删除");
    ErrorCode WAREHOUSE_DISABLE_FAIL_STOCK_NOT_ZERO = new ErrorCode(1_030_400_003, "仓库【{}】仍存在非零库存，无法停用");
    ErrorCode WAREHOUSE_SALE_DISABLE_FAIL_STOCK_NOT_ZERO = new ErrorCode(1_030_400_004, "仓库【{}】仍存在非零库存，无法关闭销售启用");
    ErrorCode WAREHOUSE_PURCHASE_NOT_ENABLE = new ErrorCode(1_030_400_005, "仓库【{}】未启用采购，不能在采购模块选择");
    ErrorCode WAREHOUSE_SALE_NOT_ENABLE = new ErrorCode(1_030_400_006, "仓库【{}】未启用销售，不能在销售模块选择");
    ErrorCode WAREHOUSE_CODE_EXISTS = new ErrorCode(1_030_400_007, "仓库编码({})已存在");
    ErrorCode WAREHOUSE_SALE_DEPT_PERMISSION_DENIED = new ErrorCode(1_030_400_008, "当前部门无权在销售中使用仓库【{}】");
    ErrorCode WAREHOUSE_DIRECT_NOT_CONFIGURED = new ErrorCode(1_030_400_009,
            "未配置启用的直发仓，无法自动生成调拨出仓单");
    ErrorCode WAREHOUSE_DIRECT_MULTIPLE = new ErrorCode(1_030_400_010,
            "存在多个可匹配的直发仓，请按销售部门保留唯一的启用直发仓");

    // ========== ERP 其它入库单 1-030-401-000 ==========
    ErrorCode STOCK_IN_NOT_EXISTS = new ErrorCode(1_030_401_000, "其它入库单不存在");
    ErrorCode STOCK_IN_DELETE_FAIL_APPROVE = new ErrorCode(1_030_401_001, "其它入库单({})已审核，无法删除");
    ErrorCode STOCK_IN_PROCESS_FAIL = new ErrorCode(1_030_401_002, "反审核失败，只有已审核的入库单才能反审核");
    ErrorCode STOCK_IN_APPROVE_FAIL = new ErrorCode(1_030_401_003, "审核失败，只有未审核的入库单才能审核");
    ErrorCode STOCK_IN_NO_EXISTS = new ErrorCode(1_030_401_004, "生成入库单失败，请重新提交");
    ErrorCode STOCK_IN_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_401_005, "其它入库单({})已审核，无法修改");
    ErrorCode STOCK_IN_ITEM_DUPLICATE = new ErrorCode(1_030_401_006, "同一入库单明细中产品和仓库重复：{}");
    ErrorCode STOCK_IN_ITEM_BATCH_UPDATE_FIELD_REQUIRED = new ErrorCode(1_030_401_007, "请选择目标仓库");
    ErrorCode STOCK_IN_ITEM_BATCH_UPDATE_ITEM_NOT_EXISTS = new ErrorCode(1_030_401_008, "所选其它入库明细不存在");
    ErrorCode STOCK_IN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED = new ErrorCode(1_030_401_009, "目标仓库不属于当前单据部门的可用仓库");
    ErrorCode STOCK_IN_ITEM_OPERATION_INVALID = new ErrorCode(1_030_401_010,
            "其它入库明细操作类型无效");
    ErrorCode STOCK_IN_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_401_011,
            "其它入库明细不存在或不属于当前单据");
    ErrorCode STOCK_IN_ITEM_EMPTY = new ErrorCode(1_030_401_012, "其它入库明细不能为空");

    // ========== ERP 入仓单 1-030-406-000 ==========
    ErrorCode STOCK_IN_BILL_NOT_EXISTS = new ErrorCode(1_030_406_000, "入仓单不存在");
    ErrorCode STOCK_IN_BILL_PICKUP_FAIL_COMPLETED = new ErrorCode(1_030_406_001, "入仓单({})已完成提货");
    ErrorCode STOCK_IN_BILL_PICKUP_ITEM_NOT_EXISTS = new ErrorCode(1_030_406_002, "入仓单明细不存在：{}");
    ErrorCode STOCK_IN_BILL_PICKUP_COUNT_EXCEED = new ErrorCode(1_030_406_003, "入仓单明细({})本次提货数量({})超过待提数量({})");
    ErrorCode STOCK_IN_BILL_NO_EXISTS = new ErrorCode(1_030_406_004, "生成入仓单失败，请重新提交");

    // ========== ERP 出仓单 1-030-407-000 ==========
    ErrorCode STOCK_OUT_BILL_NOT_EXISTS = new ErrorCode(1_030_407_000, "出仓单不存在");
    ErrorCode STOCK_OUT_BILL_PICK_FAIL_COMPLETED = new ErrorCode(1_030_407_001, "出仓单({})已完成拣货");
    ErrorCode STOCK_OUT_BILL_PICK_ITEM_NOT_EXISTS = new ErrorCode(1_030_407_002, "出仓单明细不存在：{}");
    ErrorCode STOCK_OUT_BILL_PICK_COUNT_EXCEED = new ErrorCode(1_030_407_003, "出仓单明细({})本次拣货数量({})超过待拣数量({})");
    ErrorCode STOCK_OUT_BILL_NO_EXISTS = new ErrorCode(1_030_407_004, "生成出仓单失败，请重新提交");

    // ========== ERP 其它出库单 1-030-402-000 ==========
    ErrorCode STOCK_OUT_NOT_EXISTS = new ErrorCode(1_030_402_000, "其它出库单不存在");
    ErrorCode STOCK_OUT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_402_001, "其它出库单({})已审核，无法删除");
    ErrorCode STOCK_OUT_PROCESS_FAIL = new ErrorCode(1_030_402_002, "反审核失败，只有已审核的出库单才能反审核");
    ErrorCode STOCK_OUT_APPROVE_FAIL = new ErrorCode(1_030_402_003, "审核失败，只有未审核的出库单才能审核");
    ErrorCode STOCK_OUT_NO_EXISTS = new ErrorCode(1_030_402_004, "生成出库单失败，请重新提交");
    ErrorCode STOCK_OUT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_402_005, "其它出库单({})已审核，无法修改");
    ErrorCode STOCK_OUT_ITEM_DUPLICATE = new ErrorCode(1_030_402_006, "同一出库单明细中产品和仓库重复：{}");
    ErrorCode STOCK_OUT_ITEM_BATCH_UPDATE_FIELD_REQUIRED = new ErrorCode(1_030_402_007, "请选择目标仓库");
    ErrorCode STOCK_OUT_ITEM_BATCH_UPDATE_ITEM_NOT_EXISTS = new ErrorCode(1_030_402_008, "所选其它出库明细不存在");
    ErrorCode STOCK_OUT_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED = new ErrorCode(1_030_402_009, "目标仓库不属于当前单据部门的可用仓库");
    ErrorCode STOCK_OUT_ITEM_OPERATION_INVALID = new ErrorCode(1_030_402_010,
            "其它出库明细操作类型无效");
    ErrorCode STOCK_OUT_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_402_033,
            "其它出库明细不存在或不属于当前单据");
    ErrorCode STOCK_OUT_ITEM_EMPTY = new ErrorCode(1_030_402_036, "其它出库明细不能为空");

    // ========== ERP 调拨出库单 1-030-403-000 ==========
    ErrorCode STOCK_MOVE_NOT_EXISTS = new ErrorCode(1_030_402_000, "调拨出库单不存在");
    ErrorCode STOCK_MOVE_DELETE_FAIL_APPROVE = new ErrorCode(1_030_402_001, "调拨出库单({})已审核，无法删除");
    ErrorCode STOCK_MOVE_PROCESS_FAIL = new ErrorCode(1_030_402_002, "反审核失败，只有已审核的调拨出库单才能反审核");
    ErrorCode STOCK_MOVE_APPROVE_FAIL = new ErrorCode(1_030_402_003, "审核失败，只有待审批的调拨出库单才能审核");
    ErrorCode STOCK_MOVE_NO_EXISTS = new ErrorCode(1_030_402_004, "生成调拨号失败，请重新提交");
    ErrorCode STOCK_MOVE_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_402_005, "调拨出库单({})已审核，无法修改");
    ErrorCode STOCK_MOVE_ITEM_DUPLICATE = new ErrorCode(1_030_402_006, "同一调拨出库单明细中产品、调出仓库和调入仓库重复：{}");
    ErrorCode STOCK_MOVE_WAREHOUSE_REQUIRED = new ErrorCode(1_030_402_007, "调拨出库单明细的调出仓库和调入仓库不能为空");
    ErrorCode STOCK_MOVE_WAREHOUSE_SAME = new ErrorCode(1_030_402_008, "调拨出库单明细的调出仓库和调入仓库不能相同");
    ErrorCode STOCK_MOVE_SHARE_PRICE_REQUIRED = new ErrorCode(1_030_402_009, "产品({})跨部门调拨需要维护股份价");
    ErrorCode STOCK_MOVE_DELETE_CROSS_DEPT_DENIED = new ErrorCode(1_030_402_011, "销售手推车跨部门调拨出库单只能由总公司删除");
    ErrorCode STOCK_MOVE_CREATE_PERMISSION_DENIED = new ErrorCode(1_030_402_012, "当前用户没有创建调拨出库单的权限");
    ErrorCode STOCK_MOVE_ITEM_EMPTY = new ErrorCode(1_030_402_013, "调拨出库单明细不能为空");
    ErrorCode STOCK_MOVE_ITEM_COUNT_POSITIVE = new ErrorCode(1_030_402_014, "调拨数量必须大于 0");
    ErrorCode STOCK_MOVE_ITEM_PRICE_POSITIVE = new ErrorCode(1_030_402_015, "调拨单价必须大于 0");
    ErrorCode STOCK_MOVE_TRANSFER_IN_EXISTS = new ErrorCode(1_030_402_016, "该调拨出库单已生成调拨入库单，请勿重复审批");
    ErrorCode STOCK_MOVE_LEGACY_READ_ONLY = new ErrorCode(1_030_402_017,
            "原库存调拨功能已停用，仅支持历史查询；新业务请使用调拨出库单");
    ErrorCode STOCK_MOVE_DELETE_CART_SOURCE_DENIED = new ErrorCode(1_030_402_018,
            "销售手推车来源调拨出库单请使用“解锁手推车”操作");
    ErrorCode STOCK_MOVE_UNLOCK_NOT_TRANSFER_OUT = new ErrorCode(1_030_402_019,
            "当前单据不是调拨出库单，不能解锁手推车");
    ErrorCode STOCK_MOVE_UNLOCK_NOT_CART_SOURCE = new ErrorCode(1_030_402_020,
            "当前调拨出库单不是由销售手推车生成，不能解锁");
    ErrorCode STOCK_MOVE_UNLOCK_SOURCE_ID_MISSING = new ErrorCode(1_030_402_021,
            "调拨出库单缺少来源手推车信息，不能解锁");
    ErrorCode STOCK_MOVE_UNLOCK_APPROVED = new ErrorCode(1_030_402_022,
            "调拨出库单已审核，不能解锁手推车");
    ErrorCode STOCK_MOVE_UNLOCK_CROSS_DEPT_DENIED = new ErrorCode(1_030_402_023,
            "销售手推车跨部门调拨出库单只能由总公司解锁");
    ErrorCode STOCK_MOVE_UNLOCK_SOURCE_APPROVED_EXISTS = new ErrorCode(1_030_402_024,
            "来源手推车已存在审核后的调拨出库单，不能解锁");
    ErrorCode STOCK_MOVE_APPROVE_DEPT_PERMISSION_DENIED = new ErrorCode(1_030_402_025,
            "当前账号的数据权限不包含全部调出部门，不能审批该调拨出库单");
    ErrorCode STOCK_MOVE_UPDATE_FAIL_NOT_DRAFT = new ErrorCode(1_030_402_026,
            "调拨出库单({})不是草稿，无法保存草稿修改");
    ErrorCode STOCK_MOVE_SUBMIT_FAIL = new ErrorCode(1_030_402_027,
            "调拨出库单草稿提交失败，请刷新后重试");
    ErrorCode STOCK_MOVE_SUBMIT_TIME_REQUIRED = new ErrorCode(1_030_402_028,
            "提交调拨出库单草稿时调拨时间不能为空");
    ErrorCode STOCK_MOVE_SUBMIT_ITEMS_REQUIRED = new ErrorCode(1_030_402_029,
            "提交调拨出库单草稿时至少需要一条有效明细");
    ErrorCode STOCK_MOVE_FORMAL_UPDATE_FAIL_DRAFT = new ErrorCode(1_030_402_030,
            "调拨出库单({})仍是草稿，请使用草稿提交操作");
    ErrorCode STOCK_MOVE_DRAFT_ITEMS_REQUIRED = new ErrorCode(1_030_402_031,
            "生成调拨出库草稿时至少需要一条有效明细");
    ErrorCode STOCK_MOVE_SOURCE_SALE_RETURN_CHANGED = new ErrorCode(1_030_402_032,
            "来源销售退货明细已绑定，不能修改来源商品身份");
    ErrorCode STOCK_MOVE_UPDATE_CART_SOURCE_DENIED = new ErrorCode(1_030_402_033,
            "销售手推车来源调拨出库单不允许编辑，请返回销售手推车修改");
    ErrorCode STOCK_MOVE_ITEM_OPERATION_INVALID = new ErrorCode(1_030_402_034,
            "调拨出库明细操作类型无效");
    ErrorCode STOCK_MOVE_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_402_035,
            "调拨出库明细不存在或不属于当前单据");
    ErrorCode STOCK_MOVE_APPROVE_CART_SOURCE_PICK_DELIVERY_REQUIRED = new ErrorCode(1_030_402_036,
            "销售手推车来源调拨出库单需完成拣货送货后由系统自动审核");

    // ========== ERP 仓库移货单 1-030-408-000 ==========
    ErrorCode WAREHOUSE_MOVE_NOT_EXISTS = new ErrorCode(1_030_408_000, "仓库移货单不存在");
    ErrorCode WAREHOUSE_MOVE_DELETE_FAIL_APPROVE = new ErrorCode(1_030_408_001, "仓库移货单({})已审核，无法删除");
    ErrorCode WAREHOUSE_MOVE_PROCESS_FAIL = new ErrorCode(1_030_408_002, "反审核失败，只有已审核的仓库移货单才能反审核");
    ErrorCode WAREHOUSE_MOVE_APPROVE_FAIL = new ErrorCode(1_030_408_003, "审核失败，只有未审核的仓库移货单才能审核");
    ErrorCode WAREHOUSE_MOVE_NO_EXISTS = new ErrorCode(1_030_408_004, "生成移货单号失败，请重新提交");
    ErrorCode WAREHOUSE_MOVE_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_408_005, "仓库移货单({})已审核，无法修改");
    ErrorCode WAREHOUSE_MOVE_ITEM_DUPLICATE = new ErrorCode(1_030_408_006, "同一移货单明细中产品、移出仓库、移入仓库和移出货架重复：{}");
    ErrorCode WAREHOUSE_MOVE_WAREHOUSE_REQUIRED = new ErrorCode(1_030_408_007, "仓库移货单的移出仓库和移入仓库不能为空");
    ErrorCode WAREHOUSE_MOVE_WAREHOUSE_SAME = new ErrorCode(1_030_408_008, "移出仓库和移入仓库不能相同");
    ErrorCode WAREHOUSE_MOVE_ITEM_COUNT_POSITIVE = new ErrorCode(1_030_408_009, "移货数量必须大于 0");
    ErrorCode WAREHOUSE_MOVE_STOCK_NOT_ENOUGH = new ErrorCode(1_030_408_010, "产品({})移出仓库库存不足，可用数量({})，移货数量({})");
    ErrorCode WAREHOUSE_MOVE_UPDATE_FAIL_NOT_DRAFT = new ErrorCode(1_030_408_011, "仓库移货单({})不是草稿，无法保存草稿修改");
    ErrorCode WAREHOUSE_MOVE_SUBMIT_FAIL = new ErrorCode(1_030_408_012, "仓库移货单草稿提交失败，请刷新后重试");
    ErrorCode WAREHOUSE_MOVE_SUBMIT_TIME_REQUIRED = new ErrorCode(1_030_408_013, "提交仓库移货单草稿时移货日期不能为空");
    ErrorCode WAREHOUSE_MOVE_SUBMIT_ITEMS_REQUIRED = new ErrorCode(1_030_408_014, "提交仓库移货单草稿时至少需要一条有效明细");
    ErrorCode WAREHOUSE_MOVE_DRAFT_ITEMS_REQUIRED = new ErrorCode(1_030_408_015, "生成仓库移货草稿时至少需要一条有效明细");
    ErrorCode WAREHOUSE_MOVE_ITEM_OPERATION_INVALID = new ErrorCode(1_030_408_016,
            "仓库移货明细操作类型无效");
    ErrorCode WAREHOUSE_MOVE_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_408_017,
            "仓库移货明细不存在或不属于当前单据");

    // ========== ERP 库存盘点单 1-030-403-000 ==========
    ErrorCode STOCK_CHECK_NOT_EXISTS = new ErrorCode(1_030_403_000, "库存盘点单不存在");
    ErrorCode STOCK_CHECK_DELETE_FAIL_APPROVE = new ErrorCode(1_030_403_001, "库存盘点单({})已审核，无法删除");
    ErrorCode STOCK_CHECK_PROCESS_FAIL = new ErrorCode(1_030_403_002, "反审核失败，只有已审核的盘点单才能反审核");
    ErrorCode STOCK_CHECK_APPROVE_FAIL = new ErrorCode(1_030_403_003, "审核失败，只有未审核的盘点单才能审核");
    ErrorCode STOCK_CHECK_NO_EXISTS = new ErrorCode(1_030_403_004, "生成盘点号失败，请重新提交");
    ErrorCode STOCK_CHECK_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_403_005, "库存盘点单({})已审核，无法修改");
    ErrorCode STOCK_CHECK_ITEM_DUPLICATE = new ErrorCode(1_030_403_006, "同一盘点单明细中产品和仓库重复：{}");
    ErrorCode STOCK_CHECK_UPDATE_FAIL_NOT_DRAFT = new ErrorCode(1_030_403_007, "库存盘点单({})不是草稿，无法保存草稿修改");
    ErrorCode STOCK_CHECK_SUBMIT_FAIL = new ErrorCode(1_030_403_008, "库存盘点草稿提交失败，请刷新后重试");
    ErrorCode STOCK_CHECK_SUBMIT_TIME_REQUIRED = new ErrorCode(1_030_403_009, "库存盘点草稿提交失败，盘点时间不能为空");
    ErrorCode STOCK_CHECK_SUBMIT_ITEMS_REQUIRED = new ErrorCode(1_030_403_010, "库存盘点草稿提交失败，盘点明细不能为空");
    ErrorCode STOCK_CHECK_DRAFT_ITEMS_REQUIRED = new ErrorCode(1_030_403_011, "生成库存盘点草稿时至少需要一条有效明细");
    ErrorCode STOCK_CHECK_ITEM_BATCH_UPDATE_FIELD_REQUIRED = new ErrorCode(1_030_403_012, "请选择目标仓库");
    ErrorCode STOCK_CHECK_ITEM_BATCH_UPDATE_ITEM_NOT_EXISTS = new ErrorCode(1_030_403_013, "所选库存盘点明细不存在");
    ErrorCode STOCK_CHECK_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED = new ErrorCode(1_030_403_014, "目标仓库不属于当前单据部门的可用仓库");
    ErrorCode STOCK_CHECK_ITEM_OPERATION_INVALID = new ErrorCode(1_030_403_015,
            "库存盘点明细操作类型无效");
    ErrorCode STOCK_CHECK_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_403_016,
            "库存盘点明细不存在或不属于当前单据");

    // ========== ERP 产品库存 1-030-404-000 ==========
    ErrorCode STOCK_COUNT_NEGATIVE = new ErrorCode(1_030_404_000, "操作失败，产品({})所在仓库({})的库存：{}，小于变更数量：{}");
    ErrorCode STOCK_COUNT_NEGATIVE2 = new ErrorCode(1_030_404_001, "操作失败，产品({})所在仓库({})的库存不足");

    // ========== ERP 采购调价单 1-030-506-000 ==========
    ErrorCode PURCHASE_PRICE_ADJUST_NOT_EXISTS = new ErrorCode(1_030_506_000, "采购调价单不存在");
    ErrorCode PURCHASE_PRICE_ADJUST_DELETE_FAIL_APPROVE = new ErrorCode(1_030_506_001, "采购调价单已审批，无法删除");
    ErrorCode PURCHASE_PRICE_ADJUST_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_506_002, "采购调价单已审批，无法修改");
    ErrorCode PURCHASE_PRICE_ADJUST_APPROVE_FAIL = new ErrorCode(1_030_506_003, "采购调价单已审批，不可重复审批");
    ErrorCode PURCHASE_PRICE_ADJUST_PROCESS_FAIL = new ErrorCode(1_030_506_004, "采购调价单未审批，无法反审批");
    ErrorCode PURCHASE_PRICE_ADJUST_ITEM_NOT_EXISTS = new ErrorCode(1_030_506_005, "采购调价明细不存在");
    ErrorCode PURCHASE_PRICE_ADJUST_TYPE_INVALID = new ErrorCode(1_030_506_006, "采购调价类型不正确");
    ErrorCode PURCHASE_PRICE_ADJUST_ITEM_EMPTY = new ErrorCode(1_030_506_007, "采购调价明细不能为空");
    ErrorCode PURCHASE_PRICE_ADJUST_ITEM_ADJUSTED = new ErrorCode(1_030_506_008, "该入库明细已被调价，不可再次调价（添加明细方式）");
    ErrorCode PURCHASE_PRICE_ADJUST_NEW_PRICE_NEGATIVE = new ErrorCode(1_030_506_009, "调价后单价不能为负");
    ErrorCode PURCHASE_PRICE_ADJUST_NO_EXISTS = new ErrorCode(1_030_506_010, "生成调价单号失败，请重新提交");
    ErrorCode PURCHASE_PRICE_ADJUST_FAIL_PAYMENT_PRICE_EXCEED = new ErrorCode(1_030_506_011, "付款金额({})超过采购调价单可结算金额({})");
    ErrorCode PURCHASE_PRICE_ADJUST_ITEM_DUPLICATE = new ErrorCode(1_030_506_012, "同一采购调价明细中产品、仓库、赠品标识重复：{}");
    ErrorCode PURCHASE_PRICE_ADJUST_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_506_013, "采购调价单（{}）不是草稿，无法按草稿保存");
    ErrorCode PURCHASE_PRICE_ADJUST_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_506_014, "采购调价单不是草稿或已被提交，请刷新后重试");
    ErrorCode PURCHASE_PRICE_ADJUST_SUBMIT_SUPPLIER_REQUIRED = new ErrorCode(1_030_506_015, "供应商不能为空");
    ErrorCode PURCHASE_PRICE_ADJUST_IN_HAS_APPROVED_INVOICE = new ErrorCode(1_030_506_016,
            "入库单({})已完成采购票据开具，不能进行采购调价");
    ErrorCode PURCHASE_PRICE_ADJUST_ITEM_OPERATION_INVALID = new ErrorCode(1_030_506_017,
            "采购调价明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode PURCHASE_PRICE_ADJUST_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_506_018,
            "采购调价明细不存在或不属于当前采购调价单");

    // ========== ERP 库存占用 1-030-405-000 ==========
    ErrorCode STOCK_LOCK_NOT_EXISTS = new ErrorCode(1_030_405_000, "库存占用记录不存在");
    ErrorCode STOCK_LOCK_AVAILABLE_COUNT_NOT_ENOUGH = new ErrorCode(1_030_405_001, "产品({})在仓库({})的可用库存不足，可用：{}，需要：{}");
    ErrorCode MALL_STOCK_NOT_MATCH = new ErrorCode(1_030_405_002, "请选择有效库存");
    ErrorCode MALL_STOCK_NOT_AVAILABLE = new ErrorCode(1_030_405_003, "所选库存不足，请重新选择");
    ErrorCode MALL_PRODUCT_PRICE_MAPPING_NOT_EXISTS = new ErrorCode(1_030_405_004, "商品未关联 ERP 配件，无法修改小程序价格");
    ErrorCode MALL_PRODUCT_PRICE_MAPPING_MULTIPLE = new ErrorCode(1_030_405_005, "商品关联多个 ERP 配件，无法直接修改小程序价格");
    ErrorCode MALL_PRODUCT_PRICE_NEGATIVE = new ErrorCode(1_030_405_006, "小程序价格不能小于 0");

    // ========== ERP 连锁开单 1-030-700-000 ==========
    ErrorCode CHAIN_ORDER_NOT_EXISTS = new ErrorCode(1_030_700_000, "连锁开单不存在");
    ErrorCode CHAIN_ORDER_APPROVE_FAIL = new ErrorCode(1_030_700_001, "审核失败，只有待审核的连锁开单才能审核");
    ErrorCode CHAIN_ORDER_CANCEL_FAIL = new ErrorCode(1_030_700_002, "取消失败，只有待审核的连锁开单才能取消");
    ErrorCode CHAIN_ORDER_NO_EXISTS = new ErrorCode(1_030_700_003, "生成连锁开单号失败，请重新提交");

    // ========== ERP 销售调价单 1-030-204-000 ==========
    ErrorCode SALE_PRICE_ADJUST_NOT_EXISTS = new ErrorCode(1_030_204_000, "销售调价单不存在");
    ErrorCode SALE_PRICE_ADJUST_DELETE_FAIL_APPROVE = new ErrorCode(1_030_204_001, "销售调价单({})已审核，无法删除");
    ErrorCode SALE_PRICE_ADJUST_PROCESS_FAIL = new ErrorCode(1_030_204_002, "反审核失败，只有已审核的调价单才能反审核");
    ErrorCode SALE_PRICE_ADJUST_APPROVE_FAIL = new ErrorCode(1_030_204_003, "审核失败，只有未审核的调价单才能审核");
    ErrorCode SALE_PRICE_ADJUST_NO_EXISTS = new ErrorCode(1_030_204_004, "生成调价单号失败，请重新提交");
    ErrorCode SALE_PRICE_ADJUST_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_204_005, "销售调价单({})已审核，无法修改");
    ErrorCode SALE_PRICE_ADJUST_ITEM_DUPLICATE = new ErrorCode(1_030_204_006, "销售出库明细({})重复，不能在同一调价单内重复调价");
    ErrorCode SALE_PRICE_ADJUST_ITEM_ADJUSTED = new ErrorCode(1_030_204_007, "销售出库明细({})已被调价，不能再次调价");
    ErrorCode SALE_PRICE_ADJUST_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_204_008, "销售调价单({})不是草稿，无法按草稿保存");
    ErrorCode SALE_PRICE_ADJUST_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_204_009, "销售调价草稿提交失败，请刷新后重试");
    ErrorCode SALE_PRICE_ADJUST_SUBMIT_CUSTOMER_REQUIRED = new ErrorCode(1_030_204_010, "销售调价草稿缺少客户，无法提交");
    ErrorCode SALE_PRICE_ADJUST_SUBMIT_USER_REQUIRED = new ErrorCode(1_030_204_011, "销售调价草稿缺少调价人，无法提交");
    ErrorCode SALE_PRICE_ADJUST_SUBMIT_SETTLE_METHOD_REQUIRED = new ErrorCode(1_030_204_012, "销售调价草稿缺少结算方式，无法提交");
    ErrorCode SALE_PRICE_ADJUST_SUBMIT_DELIVERY_METHOD_REQUIRED = new ErrorCode(1_030_204_013, "销售调价草稿缺少送货方式，无法提交");
    ErrorCode SALE_PRICE_ADJUST_SUBMIT_ITEMS_REQUIRED = new ErrorCode(1_030_204_014, "销售调价草稿缺少有效调价明细，无法提交");
    ErrorCode SALE_PRICE_ADJUST_DRAFT_ITEMS_REQUIRED = new ErrorCode(1_030_204_015, "生成销售调价草稿时至少需要一条有效明细");
    ErrorCode SALE_PRICE_ADJUST_ITEM_OPERATION_INVALID = new ErrorCode(1_030_204_016,
            "销售调价明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode SALE_PRICE_ADJUST_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_204_017,
            "销售调价明细不存在或不属于当前销售调价单");

    // ========== ERP 报价订单 1-030-205-000 ==========
    ErrorCode SALE_QUOTE_NOT_EXISTS = new ErrorCode(1_030_205_000, "报价订单不存在");
    ErrorCode SALE_QUOTE_NO_EXISTS = new ErrorCode(1_030_205_001, "生成报价单号失败，请重新提交");
    ErrorCode SALE_QUOTE_UPDATE_FAIL_GENERATED = new ErrorCode(1_030_205_002, "报价订单({})已生成销售单，无法修改");
    ErrorCode SALE_QUOTE_DELETE_FAIL_GENERATED = new ErrorCode(1_030_205_003, "报价订单({})已生成销售单，无法删除");
    ErrorCode SALE_QUOTE_APPROVE_FAIL = new ErrorCode(1_030_205_004, "审核失败，只有待审核状态的报价订单才能审核");
    ErrorCode SALE_QUOTE_CONVERT_CART_FAIL = new ErrorCode(1_030_205_005, "报价订单转销售手推车失败，只有待审核、已审核或部分转换状态才能转换");
    ErrorCode SALE_QUOTE_CONVERT_COUNT_EXCEED = new ErrorCode(1_030_205_006, "报价订单项({})本次转换数量({})超过可转数量({})");
    ErrorCode SALE_QUOTE_CONVERT_COUNT_POSITIVE = new ErrorCode(1_030_205_007, "报价订单项({})本次转换数量必须大于 0");
    ErrorCode SALE_QUOTE_ITEM_NOT_EXISTS = new ErrorCode(1_030_205_008, "报价订单项不存在：{}");
    ErrorCode SALE_QUOTE_UPDATE_FAIL_NOT_DRAFT = new ErrorCode(1_030_205_009, "报价订单({})不是草稿状态，不允许编辑");
    ErrorCode SALE_QUOTE_ITEM_PRODUCT_PRICE_NOT_NULL = new ErrorCode(1_030_205_010, "报价订单项({})单价不能为空");
    ErrorCode SALE_QUOTE_ITEM_DUPLICATE = new ErrorCode(1_030_205_011, "同一报价订单明细中产品、仓库、赠品标识重复：{}");
    ErrorCode SALE_QUOTE_ITEM_PRODUCT_PRICE_POSITIVE = new ErrorCode(1_030_205_012, "报价订单项({})单价必须大于 0");
    ErrorCode SALE_QUOTE_ITEM_COUNT_POSITIVE = new ErrorCode(1_030_205_013, "报价订单项({})数量必须大于 0");
    ErrorCode SALE_QUOTE_SUBMIT_FAIL = new ErrorCode(1_030_205_014, "提交失败，只有草稿状态的报价订单才能提交");
    ErrorCode SALE_QUOTE_SUBMIT_CUSTOMER_REQUIRED = new ErrorCode(1_030_205_015, "提交报价订单前请选择客户");
    ErrorCode SALE_QUOTE_SUBMIT_ITEMS_REQUIRED = new ErrorCode(1_030_205_016, "提交报价订单前请添加报价产品");
    ErrorCode SALE_QUOTE_DRAFT_ITEMS_REQUIRED = new ErrorCode(1_030_205_017, "生成报价订单草稿时至少需要一条有效明细");
    ErrorCode SALE_QUOTE_ITEM_BATCH_UPDATE_EMPTY = new ErrorCode(1_030_205_018, "请至少选择仓库、部门或价格级别");
    ErrorCode SALE_QUOTE_ITEM_BATCH_UPDATE_FAIL_STATUS = new ErrorCode(1_030_205_019, "报价订单({})当前状态无法批量修改明细仓库/部门");
    ErrorCode SALE_QUOTE_ITEM_BATCH_UPDATE_NOT_EXISTS = new ErrorCode(1_030_205_020, "报价订单明细不存在或不属于当前单据：{}");
    ErrorCode SALE_QUOTE_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED = new ErrorCode(1_030_205_021, "所选部门不属于目标仓库可用销售部门");
    ErrorCode SALE_QUOTE_ITEM_BATCH_UPDATE_FIELD_DENIED = new ErrorCode(1_030_205_022, "当前用户没有批量修改该明细字段的权限");
    ErrorCode SALE_QUOTE_ITEM_OPERATION_INVALID = new ErrorCode(1_030_205_023,
            "报价订单明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode SALE_QUOTE_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_205_024,
            "报价订单明细不存在或不属于当前报价订单");

    // ========== ERP 销售手推车 1-030-206-000 ==========
    ErrorCode SALE_CART_NOT_EXISTS = new ErrorCode(1_030_206_000, "销售手推车不存在");
    ErrorCode SALE_CART_NO_EXISTS = new ErrorCode(1_030_206_001, "生成销售手推车单号失败，请重新提交");
    ErrorCode SALE_CART_SUBMIT_FAIL = new ErrorCode(1_030_206_002, "提交失败，只有草稿状态的销售手推车才能提交");
    ErrorCode SALE_CART_FIRST_APPROVE_FAIL = new ErrorCode(1_030_206_003, "初审失败，只有待初审的销售手推车才能初审");
    ErrorCode SALE_CART_FINAL_APPROVE_FAIL = new ErrorCode(1_030_206_004, "终审失败，只有初审通过的销售手推车才能终审");
    ErrorCode SALE_CART_UPDATE_FAIL_GENERATED = new ErrorCode(1_030_206_005, "销售手推车({})已生成销售单，无法修改");
    ErrorCode SALE_CART_CONVERT_QUOTE_FAIL = new ErrorCode(1_030_206_006, "销售手推车转报价订单失败，只有初审前才能转换");
    ErrorCode SALE_CART_DELETE_FAIL_FINAL_APPROVED = new ErrorCode(1_030_206_007, "销售手推车({})已终审生成销售单，无法删除");
    ErrorCode SALE_CART_DELETE_FAIL_CONVERTED = new ErrorCode(1_030_206_008, "销售手推车({})已转为报价订单，无法删除");
    ErrorCode SALE_CART_UPDATE_FAIL_NOT_PROCESS = new ErrorCode(1_030_206_009, "销售手推车({})已初审，无法修改");
    ErrorCode SALE_CART_REJECT_FAIL = new ErrorCode(1_030_206_010, "驳回失败，只有已提交或初审通过的销售手推车才能驳回");
    ErrorCode SALE_CART_ITEM_COUNT_POSITIVE = new ErrorCode(1_030_206_011, "产品数量必须大于 0");
    ErrorCode SALE_CART_ITEM_PRICE_POSITIVE = new ErrorCode(1_030_206_012, "产品单价必须大于 0");
    ErrorCode SALE_CART_CONVERT_QUOTE_ITEMS_EMPTY = new ErrorCode(1_030_206_013, "转出商品列表为空");
    ErrorCode SALE_CART_UPDATE_BASIC_FAIL_STATUS = new ErrorCode(1_030_206_014, "销售手推车({})不是终审通过或已生成销售单状态，无法修改基础信息");
    ErrorCode SALE_CART_DELETE_FAIL_NOT_DRAFT = new ErrorCode(1_030_206_015, "销售手推车({})已初审，无法删除");
    ErrorCode SALE_CART_FIRST_APPROVE_DISABLED = new ErrorCode(1_030_206_016, "销售手推车初审已关闭，无法执行初审");
    ErrorCode SALE_CART_FIRST_APPROVE_DEPT_UNAUTHORIZED = new ErrorCode(1_030_206_017, "当前用户所属部门未被授权执行销售手推车初审");
    ErrorCode SALE_CART_FIRST_APPROVE_DEPT_EMPTY = new ErrorCode(1_030_206_018, "启用部门授权时，授权部门不能为空");
    ErrorCode SALE_WAREHOUSE_DEPT_REQUIRED = new ErrorCode(1_030_206_019, "销售明细部门不能为空");
    ErrorCode SALE_WAREHOUSE_TRANSFER_APPROVED_EXISTS = new ErrorCode(1_030_206_020, "已存在审核后的来源调拨出库单，不能自动覆盖");
    ErrorCode SALE_WAREHOUSE_TRANSFER_NOT_APPROVED = new ErrorCode(1_030_206_021, "跨部门销售需要先审核调拨出库单，不能终审");
    ErrorCode SALE_CART_CANCEL_FIRST_APPROVE_FAIL = new ErrorCode(1_030_206_022,
            "撤销初审失败，只有待终审状态的销售手推车才能撤销初审");
    ErrorCode SALE_CART_STATUS_CHANGED = new ErrorCode(1_030_206_023, "销售手推车状态已变化，请刷新后重试");
    ErrorCode SALE_CART_UNLOCK_STATUS_INVALID = new ErrorCode(1_030_206_024,
            "销售手推车当前不是待终审状态，不能解锁");
    ErrorCode SALE_CART_DRAFT_ITEMS_REQUIRED = new ErrorCode(1_030_206_025, "生成销售手推车草稿时至少需要一条有效明细");
    ErrorCode SALE_CART_FREIGHT_AMOUNT_INVALID = new ErrorCode(1_030_206_025,
            "运费类型为“代客户付”或“我方自付”时，费用必须大于 0");
    ErrorCode SALE_CART_SELF_PAY_INFO_REQUIRED = new ErrorCode(1_030_206_026,
            "运费类型为“我方自付”时，结算方式、结算账户和销售人员不能为空");
    ErrorCode SALE_CART_ITEM_BATCH_UPDATE_EMPTY = new ErrorCode(1_030_206_027, "请至少选择仓库、部门或价格级别");
    ErrorCode SALE_CART_ITEM_BATCH_UPDATE_NOT_EXISTS = new ErrorCode(1_030_206_028, "销售手推车明细不存在或不属于当前单据：{}");
    ErrorCode SALE_CART_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED = new ErrorCode(1_030_206_029, "所选部门不属于目标仓库可用销售部门");
    ErrorCode SALE_CART_ITEM_BATCH_UPDATE_FIELD_DENIED = new ErrorCode(1_030_206_030, "当前用户没有批量修改该明细字段的权限");
    ErrorCode SALE_CART_ITEM_BATCH_UPDATE_TRANSFER_EXISTS = new ErrorCode(1_030_206_031, "销售手推车({})已存在调拨或库存锁定关系，无法批量修改明细仓库/部门");
    ErrorCode SALE_CART_ITEM_DUPLICATE = new ErrorCode(1_030_206_032, "同一销售手推车明细中产品、仓库、赠品标识、批次号重复：{}");
    ErrorCode SALE_CART_ITEM_OPERATION_INVALID = new ErrorCode(1_030_206_033,
            "销售手推车明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode SALE_CART_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_206_034,
            "销售手推车明细不存在或不属于当前单据");
    ErrorCode SALE_CART_ITEMS_EMPTY = new ErrorCode(1_030_206_035, "销售手推车明细不能为空");

    // ========== ERP 销售配置 1-030-207-000 ==========
    ErrorCode SALE_CONFIG_NOT_EXISTS = new ErrorCode(1_030_207_000, "销售配置不存在");
    ErrorCode SALE_CONFIG_CODE_DUPLICATE = new ErrorCode(1_030_207_001, "销售配置类型【{}】下已存在编码【{}】");
    ErrorCode SALE_DIRECT_DEPT_FORBIDDEN = new ErrorCode(1_030_207_010,
            "部门【{}】已禁止直接做销售单据，请由其他销售部门开单或走调货流程");

    // ========== ERP 供应商基础表 1-030-110-000 ==========
    ErrorCode SUPPLIER_CONTACT_NOT_EXISTS = new ErrorCode(1_030_110_000, "供应商联系人不存在");
    ErrorCode SUPPLIER_CONTRACT_NOT_EXISTS = new ErrorCode(1_030_110_001, "供应商合同不存在");
    ErrorCode SUPPLIER_EXTEND_INFO_NOT_EXISTS = new ErrorCode(1_030_110_002, "供应商拓展信息不存在");
    ErrorCode SUPPLIER_EXTEND_NOT_EXISTS = new ErrorCode(1_030_110_003, "供应商动态拓展字段不存在");
    ErrorCode SUPPLIER_IMAGE_NOT_EXISTS = new ErrorCode(1_030_110_004, "供应商图片不存在");
    ErrorCode SUPPLIER_ACCOUNT_NOT_EXISTS = new ErrorCode(1_030_110_005, "供应商账户不存在");
    ErrorCode SUPPLIER_BILL_NOT_EXISTS = new ErrorCode(1_030_110_006, "供应商票据不存在");
    ErrorCode SUPPLIER_TASK_NOT_EXISTS = new ErrorCode(1_030_110_007, "供应商任务量不存在");
    ErrorCode SUPPLIER_BUSINESS_INFO_NOT_EXISTS = new ErrorCode(1_030_110_008, "供应商工商信息不存在");
    ErrorCode SUPPLIER_TASK_MONTH_INVALID = new ErrorCode(1_030_110_009, "供应商任务月份必须在 1 到 12 之间");
    ErrorCode SUPPLIER_TASK_DUPLICATE = new ErrorCode(1_030_110_010, "同一供应商同一年月同一级别任务量已存在");

    // ========== ERP 车型适配 1-030-800-000 ==========
    ErrorCode VEHICLE_BRAND_NOT_EXISTS = new ErrorCode(1_030_800_000, "车型品牌不存在");
    ErrorCode VEHICLE_SERIES_NOT_EXISTS = new ErrorCode(1_030_800_001, "车系不存在");
    ErrorCode VEHICLE_MODEL_NOT_EXISTS = new ErrorCode(1_030_800_002, "车型不存在");
    ErrorCode VEHICLE_PRODUCT_FIT_NOT_EXISTS = new ErrorCode(1_030_800_003, "车型配件适配不存在");
    ErrorCode VEHICLE_PRODUCT_FIT_EXISTS = new ErrorCode(1_030_800_004, "车型配件适配已存在");

    // ========== ERP 自动订货 1-030-900-000 ==========
    ErrorCode AUTO_ORDER_RULE_NOT_EXISTS = new ErrorCode(1_030_900_000, "自动订货规则不存在");
    ErrorCode PURCHASE_SUGGESTION_NOT_EXISTS = new ErrorCode(1_030_900_001, "采购建议单不存在");
    ErrorCode PURCHASE_SUGGESTION_CONFIRM_FAIL = new ErrorCode(1_030_900_002, "确认失败，只有待确认的采购建议单才能确认");
    ErrorCode PURCHASE_SUGGESTION_GENERATE_FAIL = new ErrorCode(1_030_900_003, "生成采购单失败，只有已确认的采购建议单才能生成");

    // ========== ERP 产品 1-030-500-000 ==========
    ErrorCode PRODUCT_NOT_EXISTS = new ErrorCode(1_030_500_000, "产品不存在");
    ErrorCode PRODUCT_NOT_ENABLE = new ErrorCode(1_030_500_001, "产品({})未启用");
    ErrorCode PRODUCT_CODE_DUPLICATE = new ErrorCode(1_030_500_002, "配件编码({})已存在");
    ErrorCode PRODUCT_CODE_GENERATE_FAIL = new ErrorCode(1_030_500_003, "生成配件编码失败，请重试");
    ErrorCode PRODUCT_MERGED = new ErrorCode(1_030_500_004, "配件({})已被合并，不可操作");
    ErrorCode PRODUCT_NAME_DUPLICATE = new ErrorCode(1_030_500_021, "配件名称({})已存在");
    ErrorCode PRODUCT_WAREHOUSE_NOT_EXISTS = new ErrorCode(1_030_500_005, "默认仓库不存在");
    ErrorCode PRODUCT_UNIVERSAL_CODE_INVALID = new ErrorCode(1_030_500_006, "通用件编码({})不存在");
    ErrorCode PRODUCT_UNIVERSAL_CODE_SELF = new ErrorCode(1_030_500_007, "通用件编码不能是自身");
    ErrorCode PRODUCT_WAREHOUSE_REQUIRED = new ErrorCode(1_030_500_008, "默认仓库不能为空");
    ErrorCode PRODUCT_DELETE_FAIL_STOCK_EXISTS = new ErrorCode(1_030_500_009, "产品({})已有库存或库存历史记录，不允许删除，请停用或先清理后再操作");
    ErrorCode PRODUCT_DELETE_FAIL_REFERENCED = new ErrorCode(1_030_500_010, "该产品已被{}引用，无法删除");
    ErrorCode ERP_ITEM_BATCH_NO_REQUIRED = new ErrorCode(1_030_500_011, "第 {} 行：该配件已开启批次号管理，请填写批次号");
    ErrorCode ERP_ITEM_BATCH_NO_DISABLED = new ErrorCode(1_030_500_014, "第 {} 行：该配件未开启批次号管理，不能填写批次号");
    ErrorCode ARCHIVE_MERGE_SAME_ID = new ErrorCode(1_030_500_012, "被合并数据和保留数据不能相同");
    ErrorCode PRODUCT_MERGE_STOCK_CONFLICT = new ErrorCode(1_030_500_013, "被合并配件与保留配件存在相同仓库库存或锁定记录，请先处理库存后再合并");
    ErrorCode PRODUCT_STOCK_DISTRIBUTION_PRODUCT_DISABLED = new ErrorCode(1_030_500_015, "停用配件({})不能库存分发");
    ErrorCode PRODUCT_STOCK_DISTRIBUTION_REMOVE_DENIED = new ErrorCode(1_030_500_016, "仓库({})已有库存业务，不能取消分发");
    ErrorCode PRODUCT_STOCK_DISTRIBUTION_WAREHOUSE_DENIED = new ErrorCode(1_030_500_017, "无仓库权限，不能分发到仓库({})");
    ErrorCode PRODUCT_STOCK_DISTRIBUTION_WAREHOUSE_EMPTY = new ErrorCode(1_030_500_018, "分发仓库不能为空");
    ErrorCode PRODUCT_READONLY_BY_SALE_DISTRIBUTION = new ErrorCode(1_030_500_019,
            "该配件仅因销售仓库分配而可见，只允许查看");
    ErrorCode PRODUCT_FIELD_NO_PERMISSION = new ErrorCode(1_030_500_020, "无权查看或修改配件字段【{}】");

    // ========== ERP 商品分类 1-030-501-000 ==========
    ErrorCode PRODUCT_CATEGORY_NOT_EXISTS = new ErrorCode(1_030_501_000, "商品分类不存在");
    ErrorCode PRODUCT_CATEGORY_EXITS_CHILDREN = new ErrorCode(1_030_501_001, "存在子商品分类，无法删除");
    ErrorCode PRODUCT_CATEGORY_PARENT_NOT_EXITS = new ErrorCode(1_030_501_002,"父级商品分类不存在");
    ErrorCode PRODUCT_CATEGORY_PARENT_ERROR = new ErrorCode(1_030_501_003, "不能设置自己为父商品分类");
    ErrorCode PRODUCT_CATEGORY_NAME_DUPLICATE = new ErrorCode(1_030_501_004, "已经存在该分类名称的商品分类");
    ErrorCode PRODUCT_CATEGORY_PARENT_IS_CHILD = new ErrorCode(1_030_501_005, "不能设置自己的子分类为父分类");
    ErrorCode PRODUCT_CATEGORY_NOT_LEAF = new ErrorCode(1_030_501_006, "请选择最末级商品分类");
    ErrorCode PRODUCT_CATEGORY_CODE_DUPLICATE = new ErrorCode(1_030_501_007, "商品分类编码({})已存在");
    ErrorCode PRODUCT_CATEGORY_CODE_INVALID = new ErrorCode(1_030_501_008, "商品分类编码格式不正确，请按 001 或 001 001 的格式填写");
    ErrorCode PRODUCT_CATEGORY_PARENT_CODE_NOT_EXISTS = new ErrorCode(1_030_501_009, "父级分类编码({})不存在，请先新增父级分类");
    ErrorCode PRODUCT_CATEGORY_CODE_GENERATE_FAIL = new ErrorCode(1_030_501_010, "生成商品分类编码失败，请手动填写");
    ErrorCode PRODUCT_CATEGORY_CODE_UPDATE_FAIL_HAS_CHILDREN = new ErrorCode(1_030_501_011, "该商品分类存在子分类，不允许修改编码");
    ErrorCode PRODUCT_CATEGORY_EXITS_PRODUCT = new ErrorCode(1_030_502_002, "存在产品使用该分类，无法删除");

    // ========== ERP 产品单位 1-030-502-000 ==========
    ErrorCode PRODUCT_UNIT_NOT_EXISTS = new ErrorCode(1_030_502_000, "产品单位不存在");
    ErrorCode PRODUCT_UNIT_NAME_DUPLICATE = new ErrorCode(1_030_502_001, "已存在该名字的产品单位");
    ErrorCode PRODUCT_UNIT_EXITS_PRODUCT = new ErrorCode(1_030_502_002, "存在产品使用该单位，无法删除");

    // ========== ERP 配件品牌 1-030-506-000 ==========
    ErrorCode PRODUCT_BRAND_NOT_EXISTS = new ErrorCode(1_030_506_000, "配件品牌不存在");
    ErrorCode PRODUCT_BRAND_NAME_DUPLICATE = new ErrorCode(1_030_506_001, "已存在该名称的配件品牌");
    ErrorCode PRODUCT_BRAND_EXITS_PRODUCT = new ErrorCode(1_030_506_002, "存在配件使用该品牌，无法删除");
    ErrorCode PRODUCT_BRAND_NOT_ENABLED = new ErrorCode(1_030_506_003, "配件品牌({})已停用，不能选择");

    // ========== ERP 基础数据 1-030-503-000 ==========
    ErrorCode BASE_DATA_NOT_EXISTS = new ErrorCode(1_030_503_000, "基础数据不存在");
    ErrorCode BASE_DATA_NAME_DUPLICATE = new ErrorCode(1_030_503_001, "同类型下已存在该名字的基础数据");
    ErrorCode BASE_DATA_CODE_DUPLICATE = new ErrorCode(1_030_503_002, "同类型下已存在该编码的基础数据");

    // ========== ERP 价格体系 1-030-504-000 ==========
    ErrorCode PRICE_SYSTEM_NOT_EXISTS = new ErrorCode(1_030_504_000, "价格体系不存在");
    ErrorCode PRICE_SYSTEM_CODE_DUPLICATE = new ErrorCode(1_030_504_001, "已存在编码为【{}】的价格体系");
    ErrorCode PRICE_SYSTEM_IN_USE = new ErrorCode(1_030_504_002, "价格体系删除失败，已有商品关联");

    // ========== ERP 字段配置 1-030-505-000 ==========
    ErrorCode FIELD_CONFIG_NOT_EXISTS = new ErrorCode(1_030_505_000, "字段配置不存在");
    ErrorCode FIELD_CONFIG_MODULE_KEY_INVALID = new ErrorCode(1_030_505_001, "无效的模块标识：{}");
    ErrorCode FIELD_CONFIG_DUPLICATE = new ErrorCode(1_030_505_002, "字段配置重复：模块[{}]字段[{}]");
    ErrorCode SEARCH_FIELD_CONFIG_MODULE_KEY_INVALID = new ErrorCode(1_030_505_003, "无效的搜索字段配置模块标识：{}");
    ErrorCode SEARCH_FIELD_CONFIG_DUPLICATE = new ErrorCode(1_030_505_004, "搜索字段配置重复：模块[{}]字段[{}]");
    ErrorCode ERP_EXPORT_CAPTCHA_INVALID = new ErrorCode(1_030_505_005, "导出验证码不正确");
    ErrorCode FIELD_CONFIG_FIELD_NAME_EMPTY = new ErrorCode(1_030_505_006, "字段名不能为空");
    ErrorCode STOCK_SELECT_PRICE_CONFIG_CHANGED = new ErrorCode(1_030_505_007, "价格名称配置已被其他管理员修改，请刷新后重试");
    ErrorCode STOCK_SELECT_PRICE_INVALID_FIELD = new ErrorCode(1_030_505_008, "无效的价格字段：{}");
    ErrorCode STOCK_SELECT_PRICE_DUPLICATE_FIELD = new ErrorCode(1_030_505_009, "价格字段重复：{}");
    ErrorCode STOCK_SELECT_PRICE_INVALID_BIZ_TYPE = new ErrorCode(1_030_505_010, "无效的库存选择业务类型：{}");
    ErrorCode FIELD_CONFIG_FIELD_NAME_GENERATE_FAILED = new ErrorCode(1_030_505_011, "字段编码生成失败，请重试");

    // ========== ERP 结算账户 1-030-600-000 ==========
    ErrorCode ACCOUNT_NOT_EXISTS = new ErrorCode(1_030_600_000, "结算账户不存在");
    ErrorCode ACCOUNT_NOT_ENABLE = new ErrorCode(1_030_600_001, "结算账户({})未启用");
    ErrorCode ACCOUNT_DELETE_FAIL_REFERENCED = new ErrorCode(1_030_600_002, "该银行账户已被{}引用，无法删除");
    ErrorCode ACCOUNT_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_600_003, "结算账户草稿保存失败：{}");
    ErrorCode ACCOUNT_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_600_004, "结算账户草稿提交失败：{}");
    ErrorCode ACCOUNT_FORMAL_UPDATE_FAIL_DRAFT = new ErrorCode(1_030_600_005, "结算账户草稿请使用草稿提交功能");
    ErrorCode ACCOUNT_NOT_SUBMITTED = new ErrorCode(1_030_600_006, "结算账户({})尚未正式提交");

    // ========== ERP 付款单 1-030-601-000 ==========
    ErrorCode FINANCE_PAYMENT_NOT_EXISTS = new ErrorCode(1_030_601_000, "付款单不存在");
    ErrorCode FINANCE_PAYMENT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_601_001, "付款单({})已审核，无法删除");
    ErrorCode FINANCE_PAYMENT_PROCESS_FAIL = new ErrorCode(1_030_601_002, "反审核失败，只有已审核的付款单才能反审核");
    ErrorCode FINANCE_PAYMENT_APPROVE_FAIL = new ErrorCode(1_030_601_003, "审核失败，只有未审核的付款单才能审核");
    ErrorCode FINANCE_PAYMENT_NO_EXISTS = new ErrorCode(1_030_601_004, "生成付款单号失败，请重新提交");
    ErrorCode FINANCE_PAYMENT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_601_005, "付款单({})已审核，无法修改");
    ErrorCode FINANCE_PAYMENT_WRITEOFF_STATUS_INVALID = new ErrorCode(1_030_601_006, "只有已审核的付款单才能核销");
    ErrorCode FINANCE_PAYMENT_WRITEOFF_AMOUNT_INVALID = new ErrorCode(1_030_601_007, "付款核销金额不正确：{}");
    ErrorCode FINANCE_PAYMENT_WRITEOFF_AMOUNT_EXCEED = new ErrorCode(1_030_601_008, "本次核销后超过付款单可核销额度");
    ErrorCode FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID = new ErrorCode(1_030_601_009, "付款核销业务单据不符合条件：{}");
    ErrorCode FINANCE_PAYMENT_WRITEOFF_ITEM_NOT_EFFECTIVE = new ErrorCode(1_030_601_010, "付款核销明细不存在、已撤销或尚未生效");
    ErrorCode FINANCE_PAYMENT_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_601_011, "付款单({})不是可编辑草稿或状态已变化");
    ErrorCode FINANCE_PAYMENT_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_601_012, "付款单草稿提交失败：{}");
    ErrorCode FINANCE_PAYMENT_DRAFT_ITEMS_REQUIRED = new ErrorCode(1_030_601_013, "生成付款单草稿时至少需要一条有效明细");
    ErrorCode FINANCE_PAYMENT_ITEM_OPERATION_INVALID = new ErrorCode(1_030_601_014,
            "付款单明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode FINANCE_PAYMENT_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_601_015,
            "付款单明细不存在或不属于当前付款单");

    // ========== ERP 收款单 1-030-602-000 ==========
    ErrorCode FINANCE_RECEIPT_NOT_EXISTS = new ErrorCode(1_030_602_000, "收款单不存在");
    ErrorCode FINANCE_RECEIPT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_602_001, "收款单({})已审核，无法删除");
    ErrorCode FINANCE_RECEIPT_PROCESS_FAIL = new ErrorCode(1_030_602_002, "反审核失败，只有已审核的收款单才能反审核");
    ErrorCode FINANCE_RECEIPT_APPROVE_FAIL = new ErrorCode(1_030_602_003, "审核失败，只有未审核的收款单才能审核");
    ErrorCode FINANCE_RECEIPT_NO_EXISTS = new ErrorCode(1_030_602_004, "生成收款单号失败，请重新提交");
    ErrorCode FINANCE_RECEIPT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_602_005, "收款单({})已审核，无法修改");
    ErrorCode FINANCE_RECEIPT_WRITEOFF_STATUS_INVALID = new ErrorCode(1_030_602_006, "只有已审核的收款单才能核销");
    ErrorCode FINANCE_RECEIPT_WRITEOFF_AMOUNT_INVALID = new ErrorCode(1_030_602_007, "收款核销金额不正确：{}");
    ErrorCode FINANCE_RECEIPT_WRITEOFF_AMOUNT_EXCEED = new ErrorCode(1_030_602_008, "本次核销后超过收款单可核销额度");
    ErrorCode FINANCE_RECEIPT_WRITEOFF_BIZ_INVALID = new ErrorCode(1_030_602_009, "收款核销业务单据不符合条件：{}");
    ErrorCode FINANCE_RECEIPT_WRITEOFF_ITEM_NOT_EFFECTIVE = new ErrorCode(1_030_602_010, "收款核销明细不存在、已撤销或尚未生效");
    ErrorCode FINANCE_RECEIPT_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_602_011, "收款单({})不是可编辑草稿或状态已变化");
    ErrorCode FINANCE_RECEIPT_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_602_012, "收款单草稿提交失败：{}");
    ErrorCode FINANCE_RECEIPT_DRAFT_ITEMS_REQUIRED = new ErrorCode(1_030_602_013, "生成收款单草稿时至少需要一条有效明细");
    ErrorCode FINANCE_RECEIPT_CUSTOMER_DEPT_NOT_ALLOWED = new ErrorCode(1_030_602_014, "客户未分配给当前收款部门，不能保存收款单");
    ErrorCode FINANCE_RECEIPT_ITEM_OPERATION_INVALID = new ErrorCode(1_030_602_015,
            "收款单明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode FINANCE_RECEIPT_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_602_016,
            "收款单明细不存在或不属于当前收款单");

    // ========== ERP 银行转账 1-030-603-000 ==========
    ErrorCode FINANCE_TRANSFER_NOT_EXISTS = new ErrorCode(1_030_603_000, "银行转账单不存在");
    ErrorCode FINANCE_TRANSFER_DELETE_FAIL_APPROVE = new ErrorCode(1_030_603_001, "银行转账单({})已审核，无法删除");
    ErrorCode FINANCE_TRANSFER_PROCESS_FAIL = new ErrorCode(1_030_603_002, "反审核失败，只有已审核的银行转账单才能反审核");
    ErrorCode FINANCE_TRANSFER_APPROVE_FAIL = new ErrorCode(1_030_603_003, "审核失败，只有未审核的银行转账单才能审核");
    ErrorCode FINANCE_TRANSFER_NO_EXISTS = new ErrorCode(1_030_603_004, "生成银行转账单号失败，请重新提交");
    ErrorCode FINANCE_TRANSFER_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_603_005, "银行转账单({})已审核，无法修改");
    ErrorCode FINANCE_TRANSFER_ACCOUNTS_SAME = new ErrorCode(1_030_603_006, "转出账户与转入账户不能相同");
    ErrorCode FINANCE_TRANSFER_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_603_007,
            "银行转账单({})不是可编辑草稿或状态已变化");
    ErrorCode FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_603_008,
            "银行转账草稿提交失败：{}");
    ErrorCode FINANCE_TRANSFER_UPDATE_FAIL_STATUS_CHANGED = new ErrorCode(1_030_603_009,
            "银行转账单({})不是待审核状态或状态已变化");

    // ========== ERP 会计科目 1-030-610-000 ==========
    ErrorCode ACCOUNTING_SUBJECT_NOT_EXISTS = new ErrorCode(1_030_610_000, "会计科目不存在");
    ErrorCode ACCOUNTING_SUBJECT_CODE_DUPLICATE = new ErrorCode(1_030_610_001, "科目编码({})已存在");
    ErrorCode ACCOUNTING_SUBJECT_NOT_LEAF = new ErrorCode(1_030_610_002, "凭证只能使用末级科目");
    ErrorCode ACCOUNTING_SUBJECT_HAS_CHILDREN = new ErrorCode(1_030_610_003, "科目({})存在下级子科目，请先删除子科目");
    ErrorCode ACCOUNTING_SUBJECT_USED_BY_VOUCHER = new ErrorCode(1_030_610_004, "科目({})已被凭证引用，无法删除");
    ErrorCode ACCOUNTING_SUBJECT_IMPORT_CODE_DUPLICATE = new ErrorCode(1_030_610_005, "Excel 中存在重复的科目编码：{}");
    ErrorCode ACCOUNTING_SUBJECT_CATEGORY_INVALID = new ErrorCode(1_030_610_006, "科目大类不合法，当前值：{}");

    // ========== ERP 系统开账 1-030-611-000 ==========
    ErrorCode BOOK_OPEN_NOT_EXISTS = new ErrorCode(1_030_611_000, "系统开账记录不存在");
    ErrorCode BOOK_OPEN_DUPLICATE = new ErrorCode(1_030_611_001, "该年度已开账，请勿重复操作");
    ErrorCode BOOK_OPEN_NO_EXISTS = new ErrorCode(1_030_611_002, "生成开账编号失败");
    ErrorCode BOOK_OPEN_PERIOD_INVALID = new ErrorCode(1_030_611_005, "开账年度不合法：会计年应在 1900~9999 之间");

    // ========== ERP 凭证 1-030-612-000 ==========
    ErrorCode VOUCHER_NOT_EXISTS = new ErrorCode(1_030_612_000, "凭证不存在");
    ErrorCode VOUCHER_NO_EXISTS = new ErrorCode(1_030_612_001, "生成凭证编号失败");
    ErrorCode VOUCHER_DEBIT_CREDIT_NOT_BALANCE = new ErrorCode(1_030_612_002, "借贷不平衡：借方({}) 贷方({})");
    ErrorCode VOUCHER_AUDIT_FAIL = new ErrorCode(1_030_612_003, "审核失败，只有未审核的凭证才能审核");
    ErrorCode VOUCHER_PROCESS_FAIL = new ErrorCode(1_030_612_004, "反审核失败，只有已审核的凭证才能反审核");
    ErrorCode VOUCHER_ITEM_EMPTY = new ErrorCode(1_030_612_005, "凭证分录不能为空");
    ErrorCode VOUCHER_AUTO_GENERATE_FAIL = new ErrorCode(1_030_612_010, "自动生成凭证失败：{}");
    ErrorCode VOUCHER_SUBJECT_CODE_MISSING = new ErrorCode(1_030_612_011, "缺少必备会计科目：{}");
    ErrorCode BIZ_PROCESS_FAIL_VOUCHER_APPROVED = new ErrorCode(1_030_612_012, "凭证({})已审核，请先反审核凭证再反审单据");
    ErrorCode VOUCHER_BIZ_APPROVED_EXISTS = new ErrorCode(1_030_612_013, "业务单据({})已存在已审核凭证，请先反审核后再重新生成");

    // ========== ERP 凭证归属 1-030-613-000 ==========
    ErrorCode VOUCHER_ATTRIBUTION_NOT_EXISTS = new ErrorCode(1_030_613_000, "归属记录不存在");
    ErrorCode VOUCHER_ATTRIBUTION_MONTH_INVALID = new ErrorCode(1_030_613_001, "归属月份不能晚于实际制单月份");
    ErrorCode VOUCHER_ATTRIBUTION_ALREADY_GENERATED = new ErrorCode(1_030_613_002, "已生成凭证的单据不可再次归属");
    ErrorCode VOUCHER_ATTRIBUTION_MONTH_BEFORE_BIZ = new ErrorCode(1_030_613_003, "归属月份不能早于业务发生月份");
    ErrorCode VOUCHER_ATTRIBUTION_BOOK_NOT_OPEN = new ErrorCode(1_030_613_004, "该年度未开账或未启用对应凭证类型");

    // ========== ERP 凭证字 1-030-614-000 ==========
    ErrorCode VOUCHER_WORD_NOT_EXISTS = new ErrorCode(1_030_614_000, "凭证字不存在");

    // ========== ERP 报表模板 1-030-615-000 ==========
    ErrorCode REPORT_TEMPLATE_NOT_EXISTS = new ErrorCode(1_030_615_000, "报表项目不存在");

    // ========== ERP 打印模板 1-030-615-100 ==========
    ErrorCode PRINT_TEMPLATE_NOT_EXISTS = new ErrorCode(1_030_615_100, "打印模板不存在");
    ErrorCode PRINT_MODULE_NOT_SUPPORTED = new ErrorCode(1_030_615_101, "打印模块不支持");
    ErrorCode CLOUD_PRINT_DEVICE_NOT_EXISTS = new ErrorCode(1_030_615_200, "云打印设备不存在");
    ErrorCode CLOUD_PRINT_DEVICE_DISABLED = new ErrorCode(1_030_615_201, "云打印设备【{}】已停用");
    ErrorCode CLOUD_PRINT_DEVICE_OFFLINE = new ErrorCode(1_030_615_202, "打印机【{}】当前离线，请检查设备电源与网络");
    ErrorCode CLOUD_PRINT_DEVICE_HEIGHT_REQUIRED = new ErrorCode(1_030_615_203,
            "针式打印机【{}】未配置纸张高度，请先在设备档案填写二联纸高度");
    ErrorCode CLOUD_PRINT_SUBMIT_FAILED = new ErrorCode(1_030_615_204, "云打印提交失败：{}");
    ErrorCode CLOUD_PRINT_DEFAULT_DEVICE_NOT_EXISTS = new ErrorCode(1_030_615_205, "未配置默认云打印设备");
    ErrorCode CLOUD_PRINT_DEVICE_DEVID_DUPLICATE = new ErrorCode(1_030_615_206, "云打印设备机器码【{}】已存在");
    ErrorCode CLOUD_PRINT_DEVICE_BOUND_BY_WAREHOUSE = new ErrorCode(1_030_615_207,
            "云打印设备【{}】已被仓库【{}】绑定，不能删除或停用");

    // ========== ERP 科目辅助核算 1-030-616-000 ==========
    ErrorCode SUBJECT_AUXILIARY_NOT_EXISTS = new ErrorCode(1_030_616_000, "科目辅助核算不存在");
    ErrorCode SUBJECT_AUXILIARY_DUPLICATE = new ErrorCode(1_030_616_001, "科目({})已绑定该辅助核算类型");

    // ========== ERP 其他应收单 1-030-617-000 ==========
    ErrorCode OTHER_RECEIVABLE_NOT_EXISTS = new ErrorCode(1_030_617_000, "其他应收单不存在");
    ErrorCode OTHER_RECEIVABLE_DELETE_FAIL_APPROVE = new ErrorCode(1_030_617_001, "其他应收单({})已审核，无法删除");
    ErrorCode OTHER_RECEIVABLE_PROCESS_FAIL = new ErrorCode(1_030_617_002, "审核失败，只有待审核的其他应收单才能审核");
    ErrorCode OTHER_RECEIVABLE_APPROVE_FAIL = new ErrorCode(1_030_617_003, "审核失败，只有未审核的其他应收单才能审核");
    ErrorCode OTHER_RECEIVABLE_NO_EXISTS = new ErrorCode(1_030_617_004, "生成其他应收单号失败，请重新提交");
    ErrorCode OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_617_005, "其他应收单({})已审核，无法修改");
    ErrorCode OTHER_RECEIVABLE_UPDATE_FAIL_STATUS_CHANGED = new ErrorCode(1_030_617_006, "其他应收单状态已变更，请刷新后重试");
    ErrorCode OTHER_RECEIVABLE_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_617_007, "其他应收单({})不是可编辑草稿或状态已变化");
    ErrorCode OTHER_RECEIVABLE_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_617_008, "其他应收草稿提交失败：{}");
    ErrorCode OTHER_RECEIVABLE_DRAFT_SAVE_FAIL = new ErrorCode(1_030_617_009, "其他应收草稿保存失败：{}");
    ErrorCode OTHER_RECEIVABLE_SAVE_FAIL = new ErrorCode(1_030_617_010, "其他应收保存失败：{}");
    ErrorCode OTHER_RECEIVABLE_CUSTOMER_DEPT_NOT_ALLOWED = new ErrorCode(1_030_617_011,
            "当前应收调账部门不在该客户可用部门范围内");
    ErrorCode RECEIVABLE_WRITEOFF_AMOUNT_EXCEED = new ErrorCode(1_030_617_007, "核销金额({})不能超过未收余额({})");
    ErrorCode RECEIVABLE_WRITEOFF_BALANCE_EMPTY = new ErrorCode(1_030_617_008, "当前客户没有可核销的应收余额");

    // ========== ERP 预收款单 1-030-618-000 ==========
    ErrorCode PRE_RECEIPT_NOT_EXISTS = new ErrorCode(1_030_618_000, "预收款单不存在");
    ErrorCode PRE_RECEIPT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_618_001, "预收款单({})已审核，无法删除");
    ErrorCode PRE_RECEIPT_PROCESS_FAIL = new ErrorCode(1_030_618_002, "反审核失败，只有已审核的预收款单才能反审核");
    ErrorCode PRE_RECEIPT_APPROVE_FAIL = new ErrorCode(1_030_618_003, "审核失败，只有未审核的预收款单才能审核");
    ErrorCode PRE_RECEIPT_NO_EXISTS = new ErrorCode(1_030_618_004, "生成预收款单号失败，请重新提交");
    ErrorCode PRE_RECEIPT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_618_005, "预收款单({})已审核，无法修改");
    ErrorCode PRE_RECEIPT_UPDATE_FAIL_STATUS_CHANGED = new ErrorCode(1_030_618_006, "预收款单状态已变更，请刷新后重试");

    // ========== ERP 预付款单 1-030-620-000 ==========
    ErrorCode PRE_PAYMENT_NOT_EXISTS = new ErrorCode(1_030_620_000, "预付款单不存在");
    ErrorCode PRE_PAYMENT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_620_001, "预付款单({})已审核，无法删除");
    ErrorCode PRE_PAYMENT_PROCESS_FAIL = new ErrorCode(1_030_620_002, "反审核失败，只有已审核的预付款单才能反审核");
    ErrorCode PRE_PAYMENT_APPROVE_FAIL = new ErrorCode(1_030_620_003, "审核失败，只有未审核的预付款单才能审核");
    ErrorCode PRE_PAYMENT_NO_EXISTS = new ErrorCode(1_030_620_004, "生成预付款单号失败，请重新提交");
    ErrorCode PRE_PAYMENT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_620_005, "预付款单({})已审核，无法修改");
    ErrorCode PRE_PAYMENT_UPDATE_FAIL_STATUS_CHANGED = new ErrorCode(1_030_620_006, "预付款单状态已变更，请刷新后重试");

    // ========== ERP 其他应付单 1-030-621-000 ==========
    ErrorCode OTHER_PAYABLE_NOT_EXISTS = new ErrorCode(1_030_621_000, "其他应付单不存在");
    ErrorCode OTHER_PAYABLE_DELETE_FAIL_APPROVE = new ErrorCode(1_030_621_001, "其他应付单({})已审核，无法删除");
    ErrorCode OTHER_PAYABLE_PROCESS_FAIL = new ErrorCode(1_030_621_002, "反审核失败，只有已审核的其他应付单才能反审核");
    ErrorCode OTHER_PAYABLE_APPROVE_FAIL = new ErrorCode(1_030_621_003, "审核失败，只有未审核的其他应付单才能审核");
    ErrorCode OTHER_PAYABLE_NO_EXISTS = new ErrorCode(1_030_621_004, "生成其他应付单号失败，请重新提交");
    ErrorCode OTHER_PAYABLE_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_621_005, "其他应付单({})已审核，无法修改");
    ErrorCode OTHER_PAYABLE_UPDATE_FAIL_STATUS_CHANGED = new ErrorCode(1_030_621_006, "其他应付单状态已变更，请刷新后重试");
    ErrorCode OTHER_PAYABLE_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_621_009, "其他应付单({})不是可编辑草稿或状态已变化");
    ErrorCode OTHER_PAYABLE_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_621_010, "其他应付草稿提交失败：{}");
    ErrorCode OTHER_PAYABLE_DRAFT_SAVE_FAIL = new ErrorCode(1_030_621_011, "其他应付草稿保存失败：{}");
    ErrorCode OTHER_PAYABLE_SAVE_FAIL = new ErrorCode(1_030_621_012, "其他应付保存失败：{}");
    ErrorCode OTHER_PAYABLE_SUPPLIER_DEPT_NOT_ALLOWED = new ErrorCode(1_030_621_013,
            "当前应付调账部门不在该供应商可用部门范围内");
    ErrorCode PAYABLE_WRITEOFF_AMOUNT_EXCEED = new ErrorCode(1_030_621_007, "核销金额({})不能超过未付余额({})");
    ErrorCode PAYABLE_WRITEOFF_BALANCE_EMPTY = new ErrorCode(1_030_621_008, "当前供应商没有可核销的应付余额");

    // ========== ERP 预收账款单 1-030-622-000 ==========
    ErrorCode PRE_RECEIVABLE_NOT_EXISTS = new ErrorCode(1_030_622_000, "预收账款单不存在");
    ErrorCode PRE_RECEIVABLE_DELETE_FAIL_APPROVE = new ErrorCode(1_030_622_001, "预收账款单({})已审核，无法删除");
    ErrorCode PRE_RECEIVABLE_PROCESS_FAIL = new ErrorCode(1_030_622_002, "反审核失败，只有已审核的预收账款单才能反审核");
    ErrorCode PRE_RECEIVABLE_APPROVE_FAIL = new ErrorCode(1_030_622_003, "审核失败，只有未审核的预收账款单才能审核");
    ErrorCode PRE_RECEIVABLE_NO_EXISTS = new ErrorCode(1_030_622_004, "生成预收账款单号失败，请重新提交");
    ErrorCode PRE_RECEIVABLE_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_622_005, "预收账款单({})已审核，无法修改");
    ErrorCode PRE_RECEIVABLE_UPDATE_FAIL_STATUS_CHANGED = new ErrorCode(1_030_622_006, "预收账款单状态已变更，请刷新后重试");

    // ========== ERP 费用支付单 1-030-623-000 ==========
    ErrorCode PAYABLE_EXPENSE_NOT_EXISTS = new ErrorCode(1_030_623_000, "费用支付单不存在");
    ErrorCode PAYABLE_EXPENSE_DELETE_FAIL_APPROVE = new ErrorCode(1_030_623_001, "费用支付单({})已审核，无法删除");
    ErrorCode PAYABLE_EXPENSE_PROCESS_FAIL = new ErrorCode(1_030_623_002, "反审核失败，只有已审核的费用支付单才能反审核");
    ErrorCode PAYABLE_EXPENSE_APPROVE_FAIL = new ErrorCode(1_030_623_003, "审核失败，只有未审核的费用支付单才能审核");
    ErrorCode PAYABLE_EXPENSE_NO_EXISTS = new ErrorCode(1_030_623_004, "生成费用支付单号失败，请重新提交");
    ErrorCode PAYABLE_EXPENSE_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_623_005, "费用支付单({})已审核，无法修改");
    ErrorCode PAYABLE_EXPENSE_UPDATE_FAIL_STATUS_CHANGED = new ErrorCode(1_030_623_006, "费用支付单状态已变更，请刷新后重试");
    ErrorCode PAYABLE_EXPENSE_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_623_007, "费用支付草稿({})保存失败，当前状态不是草稿");
    ErrorCode PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_623_008, "费用支付草稿提交失败：{}");
    ErrorCode PAYABLE_EXPENSE_DRAFT_ITEMS_REQUIRED = new ErrorCode(1_030_623_009, "生成费用支付草稿时至少需要一条有效明细");
    ErrorCode PAYABLE_EXPENSE_DEPT_REQUIRED = new ErrorCode(1_030_623_010, "开单部门不能为空");
    ErrorCode PAYABLE_EXPENSE_OPTION_INVALID = new ErrorCode(1_030_623_011, "费用支付{}不合法：{}");
    ErrorCode PAYABLE_EXPENSE_ITEM_OPERATION_INVALID = new ErrorCode(1_030_623_012,
            "费用支付明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode PAYABLE_EXPENSE_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_623_013,
            "费用支付明细不存在或不属于当前费用支付单");

    // ========== ERP 其他应收 1-030-624-000 ==========
    ErrorCode RECEIVABLE_MISC_NOT_EXISTS = new ErrorCode(1_030_624_000, "其他应收单不存在");
    ErrorCode RECEIVABLE_MISC_DELETE_FAIL_APPROVE = new ErrorCode(1_030_624_001, "其他应收单({})已审核，无法删除");
    ErrorCode RECEIVABLE_MISC_PROCESS_FAIL = new ErrorCode(1_030_624_002, "审核失败，只有待审核的其他应收单才能审核");
    ErrorCode RECEIVABLE_MISC_APPROVE_FAIL = new ErrorCode(1_030_624_003, "审核失败，只有未审核的其他应收单才能审核");
    ErrorCode RECEIVABLE_MISC_NO_EXISTS = new ErrorCode(1_030_624_004, "生成其他应收单号失败，请重新提交");
    ErrorCode RECEIVABLE_MISC_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_624_005, "其他应收单({})已审核，无法修改");
    ErrorCode RECEIVABLE_MISC_UPDATE_FAIL_STATUS_CHANGED = new ErrorCode(1_030_624_006, "其他应收单状态已变更，请刷新后重试");
    ErrorCode RECEIVABLE_MISC_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_624_007, "其他应收单({})不是可编辑草稿或状态已变化");
    ErrorCode RECEIVABLE_MISC_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_624_008, "其他应收草稿提交失败：{}");
    ErrorCode RECEIVABLE_MISC_DRAFT_SAVE_FAIL = new ErrorCode(1_030_624_009, "其他应收草稿保存失败：{}");
    ErrorCode RECEIVABLE_MISC_SAVE_FAIL = new ErrorCode(1_030_624_010, "其他应收保存失败：{}");

    // ========== ERP 其他收入 1-030-625-000 ==========
    ErrorCode OTHER_INCOME_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_625_000,
            "其他收入单({})不是可编辑草稿或状态已变化");
    ErrorCode OTHER_INCOME_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_625_001,
            "其他收入草稿提交失败：{}");
    ErrorCode OTHER_INCOME_DRAFT_ITEMS_REQUIRED = new ErrorCode(1_030_625_002,
            "生成其他收入草稿时至少需要一条有效明细");
    ErrorCode OTHER_INCOME_OPTION_INVALID = new ErrorCode(1_030_625_003,
            "其他收入{}不合法：{}");
    ErrorCode OTHER_INCOME_DEPT_REQUIRED = new ErrorCode(1_030_625_004, "开单部门不能为空");
    ErrorCode OTHER_INCOME_ITEM_OPERATION_INVALID = new ErrorCode(1_030_625_005,
            "其他收入明细操作类型无效，请使用 insert、update 或 delete");
    ErrorCode OTHER_INCOME_ITEM_UPDATE_NOT_EXISTS = new ErrorCode(1_030_625_006,
            "其他收入明细不存在或不属于当前其他收入单");

    // ========== ERP 其他应付 1-030-626-000 ==========
    ErrorCode PAYABLE_MISC_NOT_EXISTS = new ErrorCode(1_030_626_000, "其他应付单不存在");
    ErrorCode PAYABLE_MISC_DELETE_FAIL_APPROVE = new ErrorCode(1_030_626_001, "其他应付单({})已审核，无法删除");
    ErrorCode PAYABLE_MISC_PROCESS_FAIL = new ErrorCode(1_030_626_002, "审核失败，只有待审核的其他应付单才能审核");
    ErrorCode PAYABLE_MISC_APPROVE_FAIL = new ErrorCode(1_030_626_003, "审核失败，只有未审核的其他应付单才能审核");
    ErrorCode PAYABLE_MISC_NO_EXISTS = new ErrorCode(1_030_626_004, "生成其他应付单号失败，请重新提交");
    ErrorCode PAYABLE_MISC_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_626_005, "其他应付单({})已审核，无法修改");
    ErrorCode PAYABLE_MISC_UPDATE_FAIL_STATUS_CHANGED = new ErrorCode(1_030_626_006, "其他应付单状态已变更，请刷新后重试");
    ErrorCode PAYABLE_MISC_DRAFT_UPDATE_FAIL = new ErrorCode(1_030_626_007, "其他应付单({})不是可编辑草稿或状态已变化");
    ErrorCode PAYABLE_MISC_DRAFT_SUBMIT_FAIL = new ErrorCode(1_030_626_008, "其他应付草稿提交失败：{}");
    ErrorCode PAYABLE_MISC_DRAFT_SAVE_FAIL = new ErrorCode(1_030_626_009, "其他应付草稿保存失败：{}");
    ErrorCode PAYABLE_MISC_SAVE_FAIL = new ErrorCode(1_030_626_010, "其他应付保存失败：{}");

    ErrorCode STOCK_MOVE_APPROVE_CROSS_DEPT_DENIED = new ErrorCode(1_030_402_010, "销售手推车跨部门调拨出库单只能由总公司审批");

}
