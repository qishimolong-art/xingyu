package cn.iocoder.yudao.module.erp.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * ERP 错误码枚举类
 * <p>
 * erp 系统，使用 1-030-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== ERP 供应商（1-030-100-000） ==========
    ErrorCode SUPPLIER_NOT_EXISTS = new ErrorCode(1_030_100_000, "供应商不存在");
    ErrorCode SUPPLIER_NOT_ENABLE = new ErrorCode(1_030_100_000, "供应商({})未启用");
    ErrorCode SUPPLIER_DELETE_FAIL_REFERENCED = new ErrorCode(1_030_100_001, "该供应商已被{}引用，无法删除");
    ErrorCode SUPPLIER_CATEGORY_INVALID = new ErrorCode(1_030_100_002, "供应商类别必须是：供应商、既是客户又是供应商");
    ErrorCode SUPPLIER_DISABLE_FAIL_PAYABLE_NOT_CLEAR = new ErrorCode(1_030_100_003, "供应商【{}】仍有未结清欠款【{}】，无法停用");
    ErrorCode SUPPLIER_CODE_DUPLICATE = new ErrorCode(1_030_100_004, "供应商编码({})已存在");

    // ========== ERP 采购订单（1-030-101-000） ==========
    ErrorCode PURCHASE_ORDER_NOT_EXISTS = new ErrorCode(1_030_101_000, "采购订单不存在");
    ErrorCode PURCHASE_ORDER_DELETE_FAIL_APPROVE = new ErrorCode(1_030_101_001, "采购订单({})已审核，无法删除");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL = new ErrorCode(1_030_101_002, "反审核失败，只有已审核的采购订单才能反审核");
    ErrorCode PURCHASE_ORDER_APPROVE_FAIL = new ErrorCode(1_030_101_003, "审核失败，只有未审核的采购订单才能审核");
    ErrorCode PURCHASE_ORDER_NO_EXISTS = new ErrorCode(1_030_101_004, "生成采购单号失败，请重新提交");
    ErrorCode PURCHASE_ORDER_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_101_005, "采购订单({})已审核，无法修改");
    ErrorCode PURCHASE_ORDER_NOT_APPROVE = new ErrorCode(1_030_101_006, "采购订单未审核，无法操作");
    ErrorCode PURCHASE_ORDER_ITEM_IN_FAIL_PRODUCT_EXCEED = new ErrorCode(1_030_101_007, "采购订单项({})超过最大允许入库数量({})");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN = new ErrorCode(1_030_101_008, "反审核失败，已存在对应的采购入库单");
ErrorCode PURCHASE_ORDER_ITEM_RETURN_FAIL_IN_EXCEED = new ErrorCode(1_030_101_009, "采购订单项({})超过最大允许退货数量({})");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN = new ErrorCode(1_030_101_010, "反审核失败，已存在对应的采购退货单");
    ErrorCode PURCHASE_ORDER_ITEM_GIFT_MODIFY_FAIL_HAS_IN = new ErrorCode(1_030_101_011, "采购订单项({})已有入库记录，不允许修改赠品标记");
    ErrorCode PURCHASE_ORDER_IN_EXCEED_INABLE = new ErrorCode(1_030_101_012, "入库数量超过可入库数量，商品[{}] 可入[{}] 实入[{}]");
    ErrorCode PURCHASE_ORDER_ITEM_COUNT_POSITIVE = new ErrorCode(1_030_101_013, "订货数量不得小于等于 0");
    ErrorCode PURCHASE_ORDER_ITEM_PRICE_POSITIVE = new ErrorCode(1_030_101_014, "订货价格不得小于等于 0");
    ErrorCode PURCHASE_ORDER_ITEM_DUPLICATE = new ErrorCode(1_030_101_015, "同一采购订单明细中产品、仓库、赠品标识重复：{}");

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
    ErrorCode PURCHASE_IN_ITEM_PRICE_POSITIVE = new ErrorCode(1_030_102_016, "入库价格不得小于 0");
    ErrorCode PURCHASE_IN_ITEM_DUPLICATE = new ErrorCode(1_030_102_017, "同一采购入库明细中产品、仓库、赠品标识重复：{}");

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

    // ========== ERP 客户（1-030-200-000）==========
    ErrorCode CUSTOMER_NOT_EXISTS = new ErrorCode(1_020_200_000, "客户不存在");
    ErrorCode CUSTOMER_NOT_ENABLE = new ErrorCode(1_020_200_001, "客户({})未启用");
    ErrorCode CUSTOMER_DELETE_FAIL_REFERENCED = new ErrorCode(1_020_200_002, "该客户已被{}引用，无法删除");
    ErrorCode CUSTOMER_DISABLE_FAIL_RECEIVABLE_NOT_CLEAR = new ErrorCode(1_020_200_003, "客户【{}】仍有未结清应收账款【{}】，无法停用");
    ErrorCode CUSTOMER_CODE_DUPLICATE = new ErrorCode(1_020_200_004, "客户编码({})已存在");
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

    // ========== ERP 仓库 1-030-400-000 ==========
    ErrorCode WAREHOUSE_NOT_EXISTS = new ErrorCode(1_030_400_000, "仓库不存在");
    ErrorCode WAREHOUSE_NOT_ENABLE = new ErrorCode(1_030_400_001, "仓库({})未启用");
    ErrorCode WAREHOUSE_DELETE_FAIL_REFERENCED = new ErrorCode(1_030_400_002, "该仓库已被{}引用，无法删除");
    ErrorCode WAREHOUSE_DISABLE_FAIL_STOCK_NOT_ZERO = new ErrorCode(1_030_400_003, "仓库【{}】仍存在非零库存，无法停用");
    ErrorCode WAREHOUSE_SALE_DISABLE_FAIL_STOCK_NOT_ZERO = new ErrorCode(1_030_400_004, "仓库【{}】仍存在非零库存，无法关闭销售启用");
    ErrorCode WAREHOUSE_PURCHASE_NOT_ENABLE = new ErrorCode(1_030_400_005, "仓库【{}】未启用采购，不能在采购模块选择");
    ErrorCode WAREHOUSE_SALE_NOT_ENABLE = new ErrorCode(1_030_400_006, "仓库【{}】未启用销售，不能在销售模块选择");

    // ========== ERP 其它入库单 1-030-401-000 ==========
    ErrorCode STOCK_IN_NOT_EXISTS = new ErrorCode(1_030_401_000, "其它入库单不存在");
    ErrorCode STOCK_IN_DELETE_FAIL_APPROVE = new ErrorCode(1_030_401_001, "其它入库单({})已审核，无法删除");
    ErrorCode STOCK_IN_PROCESS_FAIL = new ErrorCode(1_030_401_002, "反审核失败，只有已审核的入库单才能反审核");
    ErrorCode STOCK_IN_APPROVE_FAIL = new ErrorCode(1_030_401_003, "审核失败，只有未审核的入库单才能审核");
    ErrorCode STOCK_IN_NO_EXISTS = new ErrorCode(1_030_401_004, "生成入库单失败，请重新提交");
    ErrorCode STOCK_IN_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_401_005, "其它入库单({})已审核，无法修改");
    ErrorCode STOCK_IN_ITEM_DUPLICATE = new ErrorCode(1_030_401_006, "同一入库单明细中产品和仓库重复：{}");

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

    // ========== ERP 库存调拨单 1-030-403-000 ==========
    ErrorCode STOCK_MOVE_NOT_EXISTS = new ErrorCode(1_030_402_000, "库存调拨单不存在");
    ErrorCode STOCK_MOVE_DELETE_FAIL_APPROVE = new ErrorCode(1_030_402_001, "库存调拨单({})已审核，无法删除");
    ErrorCode STOCK_MOVE_PROCESS_FAIL = new ErrorCode(1_030_402_002, "反审核失败，只有已审核的调拨单才能反审核");
    ErrorCode STOCK_MOVE_APPROVE_FAIL = new ErrorCode(1_030_402_003, "审核失败，只有未审核的调拨单才能审核");
    ErrorCode STOCK_MOVE_NO_EXISTS = new ErrorCode(1_030_402_004, "生成调拨号失败，请重新提交");
    ErrorCode STOCK_MOVE_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_402_005, "库存调拨单({})已审核，无法修改");
    ErrorCode STOCK_MOVE_ITEM_DUPLICATE = new ErrorCode(1_030_402_006, "同一调拨单明细中产品、调出仓库和调入仓库重复：{}");
    ErrorCode STOCK_MOVE_WAREHOUSE_REQUIRED = new ErrorCode(1_030_402_007, "库存调拨单明细的调出仓库和调入仓库不能为空");
    ErrorCode STOCK_MOVE_WAREHOUSE_SAME = new ErrorCode(1_030_402_008, "库存调拨单明细的调出仓库和调入仓库不能相同");
    ErrorCode STOCK_MOVE_SHARE_PRICE_REQUIRED = new ErrorCode(1_030_402_009, "产品({})跨部门调拨需要维护股份价");

    // ========== ERP 库存盘点单 1-030-403-000 ==========
    ErrorCode STOCK_CHECK_NOT_EXISTS = new ErrorCode(1_030_403_000, "库存盘点单不存在");
    ErrorCode STOCK_CHECK_DELETE_FAIL_APPROVE = new ErrorCode(1_030_403_001, "库存盘点单({})已审核，无法删除");
    ErrorCode STOCK_CHECK_PROCESS_FAIL = new ErrorCode(1_030_403_002, "反审核失败，只有已审核的盘点单才能反审核");
    ErrorCode STOCK_CHECK_APPROVE_FAIL = new ErrorCode(1_030_403_003, "审核失败，只有未审核的盘点单才能审核");
    ErrorCode STOCK_CHECK_NO_EXISTS = new ErrorCode(1_030_403_004, "生成盘点号失败，请重新提交");
    ErrorCode STOCK_CHECK_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_403_005, "库存盘点单({})已审核，无法修改");
    ErrorCode STOCK_CHECK_ITEM_DUPLICATE = new ErrorCode(1_030_403_006, "同一盘点单明细中产品和仓库重复：{}");

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

    // ========== ERP 库存占用 1-030-405-000 ==========
    ErrorCode STOCK_LOCK_NOT_EXISTS = new ErrorCode(1_030_405_000, "库存占用记录不存在");
    ErrorCode STOCK_LOCK_AVAILABLE_COUNT_NOT_ENOUGH = new ErrorCode(1_030_405_001, "产品({})在仓库({})的可用库存不足，可用：{}，需要：{}");

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

    // ========== ERP 报价订单 1-030-205-000 ==========
    ErrorCode SALE_QUOTE_NOT_EXISTS = new ErrorCode(1_030_205_000, "报价订单不存在");
    ErrorCode SALE_QUOTE_NO_EXISTS = new ErrorCode(1_030_205_001, "生成报价单号失败，请重新提交");
    ErrorCode SALE_QUOTE_UPDATE_FAIL_GENERATED = new ErrorCode(1_030_205_002, "报价订单({})已生成销售单，无法修改");
    ErrorCode SALE_QUOTE_DELETE_FAIL_GENERATED = new ErrorCode(1_030_205_003, "报价订单({})已生成销售单，无法删除");
    ErrorCode SALE_QUOTE_APPROVE_FAIL = new ErrorCode(1_030_205_004, "审核失败，只有草稿状态的报价订单才能审核");
    ErrorCode SALE_QUOTE_CONVERT_CART_FAIL = new ErrorCode(1_030_205_005, "报价订单转销售手推车失败，只有草稿或部分转换状态才能转换");
    ErrorCode SALE_QUOTE_CONVERT_COUNT_EXCEED = new ErrorCode(1_030_205_006, "报价订单项({})本次转换数量({})超过可转数量({})");
    ErrorCode SALE_QUOTE_CONVERT_COUNT_POSITIVE = new ErrorCode(1_030_205_007, "报价订单项({})本次转换数量必须大于 0");
    ErrorCode SALE_QUOTE_ITEM_NOT_EXISTS = new ErrorCode(1_030_205_008, "报价订单项不存在：{}");
    ErrorCode SALE_QUOTE_UPDATE_FAIL_NOT_DRAFT = new ErrorCode(1_030_205_009, "报价订单({})不是草稿状态，不允许编辑");
    ErrorCode SALE_QUOTE_ITEM_PRODUCT_PRICE_NOT_NULL = new ErrorCode(1_030_205_010, "报价订单项({})单价不能为空");
    ErrorCode SALE_QUOTE_ITEM_DUPLICATE = new ErrorCode(1_030_205_011, "同一报价订单明细中产品、仓库、赠品标识重复：{}");

    // ========== ERP 销售手推车 1-030-206-000 ==========
    ErrorCode SALE_CART_NOT_EXISTS = new ErrorCode(1_030_206_000, "销售手推车不存在");
    ErrorCode SALE_CART_NO_EXISTS = new ErrorCode(1_030_206_001, "生成销售手推车单号失败，请重新提交");
    ErrorCode SALE_CART_SUBMIT_FAIL = new ErrorCode(1_030_206_002, "提交失败，只有草稿状态的销售手推车才能提交");
    ErrorCode SALE_CART_FIRST_APPROVE_FAIL = new ErrorCode(1_030_206_003, "初审失败，只有待初审的销售手推车才能初审");
    ErrorCode SALE_CART_FINAL_APPROVE_FAIL = new ErrorCode(1_030_206_004, "终审失败，只有初审通过的销售手推车才能终审");
    ErrorCode SALE_CART_UPDATE_FAIL_GENERATED = new ErrorCode(1_030_206_005, "销售手推车({})已生成销售单，无法修改");
    ErrorCode SALE_CART_CONVERT_QUOTE_FAIL = new ErrorCode(1_030_206_006, "销售手推车转报价订单失败，只有草稿状态才能转换");
    ErrorCode SALE_CART_DELETE_FAIL_FINAL_APPROVED = new ErrorCode(1_030_206_007, "销售手推车({})已终审生成销售单，无法删除");
    ErrorCode SALE_CART_DELETE_FAIL_CONVERTED = new ErrorCode(1_030_206_008, "销售手推车({})已转为报价订单，无法删除");
    ErrorCode SALE_CART_UPDATE_FAIL_NOT_PROCESS = new ErrorCode(1_030_206_009, "销售手推车({})不是草稿状态，无法修改");
    ErrorCode SALE_CART_REJECT_FAIL = new ErrorCode(1_030_206_010, "驳回失败，只有已提交或初审通过的销售手推车才能驳回");
    ErrorCode SALE_CART_ITEM_COUNT_POSITIVE = new ErrorCode(1_030_206_011, "产品数量必须大于 0");
    ErrorCode SALE_CART_ITEM_PRICE_POSITIVE = new ErrorCode(1_030_206_012, "产品单价必须大于 0");
    ErrorCode SALE_CART_CONVERT_QUOTE_ITEMS_EMPTY = new ErrorCode(1_030_206_013, "转出商品列表为空");
    ErrorCode SALE_CART_UPDATE_BASIC_FAIL_STATUS = new ErrorCode(1_030_206_014, "销售手推车({})不是终审通过或已生成销售单状态，无法修改基础信息");
    ErrorCode SALE_CART_DELETE_FAIL_NOT_DRAFT = new ErrorCode(1_030_206_015, "销售手推车({})已审核，无法删除");

    // ========== ERP 销售配置 1-030-207-000 ==========
    ErrorCode SALE_CONFIG_NOT_EXISTS = new ErrorCode(1_030_207_000, "销售配置不存在");
    ErrorCode SALE_CONFIG_CODE_DUPLICATE = new ErrorCode(1_030_207_001, "销售配置类型【{}】下已存在编码【{}】");

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
    ErrorCode PRODUCT_WAREHOUSE_NOT_EXISTS = new ErrorCode(1_030_500_005, "默认仓库不存在");
    ErrorCode PRODUCT_UNIVERSAL_CODE_INVALID = new ErrorCode(1_030_500_006, "通用件编码({})不存在");
    ErrorCode PRODUCT_UNIVERSAL_CODE_SELF = new ErrorCode(1_030_500_007, "通用件编码不能是自身");
    ErrorCode PRODUCT_WAREHOUSE_REQUIRED = new ErrorCode(1_030_500_008, "默认仓库不能为空");
    ErrorCode PRODUCT_DELETE_FAIL_STOCK_EXISTS = new ErrorCode(1_030_500_009, "产品({})已有库存或库存历史记录，不允许删除，请停用或先清理后再操作");
    ErrorCode PRODUCT_DELETE_FAIL_REFERENCED = new ErrorCode(1_030_500_010, "该产品已被{}引用，无法删除");
    ErrorCode ERP_ITEM_BATCH_NO_REQUIRED = new ErrorCode(1_030_500_011, "第 {} 行：该配件已开启批次号管理，请填写批次号");

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

    // ========== ERP 结算账户 1-030-600-000 ==========
    ErrorCode ACCOUNT_NOT_EXISTS = new ErrorCode(1_030_600_000, "结算账户不存在");
    ErrorCode ACCOUNT_NOT_ENABLE = new ErrorCode(1_030_600_001, "结算账户({})未启用");
    ErrorCode ACCOUNT_DELETE_FAIL_REFERENCED = new ErrorCode(1_030_600_002, "该银行账户已被{}引用，无法删除");

    // ========== ERP 付款单 1-030-601-000 ==========
    ErrorCode FINANCE_PAYMENT_NOT_EXISTS = new ErrorCode(1_030_601_000, "付款单不存在");
    ErrorCode FINANCE_PAYMENT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_601_001, "付款单({})已审核，无法删除");
    ErrorCode FINANCE_PAYMENT_PROCESS_FAIL = new ErrorCode(1_030_601_002, "反审核失败，只有已审核的付款单才能反审核");
    ErrorCode FINANCE_PAYMENT_APPROVE_FAIL = new ErrorCode(1_030_601_003, "审核失败，只有未审核的付款单才能审核");
    ErrorCode FINANCE_PAYMENT_NO_EXISTS = new ErrorCode(1_030_601_004, "生成付款单号失败，请重新提交");
    ErrorCode FINANCE_PAYMENT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_601_005, "付款单({})已审核，无法修改");

    // ========== ERP 收款单 1-030-602-000 ==========
    ErrorCode FINANCE_RECEIPT_NOT_EXISTS = new ErrorCode(1_030_602_000, "收款单不存在");
    ErrorCode FINANCE_RECEIPT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_602_001, "收款单({})已审核，无法删除");
    ErrorCode FINANCE_RECEIPT_PROCESS_FAIL = new ErrorCode(1_030_602_002, "反审核失败，只有已审核的收款单才能反审核");
    ErrorCode FINANCE_RECEIPT_APPROVE_FAIL = new ErrorCode(1_030_602_003, "审核失败，只有未审核的收款单才能审核");
    ErrorCode FINANCE_RECEIPT_NO_EXISTS = new ErrorCode(1_030_602_004, "生成收款单号失败，请重新提交");
    ErrorCode FINANCE_RECEIPT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_602_005, "收款单({})已审核，无法修改");

    // ========== ERP 银行转账 1-030-603-000 ==========
    ErrorCode FINANCE_TRANSFER_NOT_EXISTS = new ErrorCode(1_030_603_000, "银行转账单不存在");
    ErrorCode FINANCE_TRANSFER_DELETE_FAIL_APPROVE = new ErrorCode(1_030_603_001, "银行转账单({})已审核，无法删除");
    ErrorCode FINANCE_TRANSFER_PROCESS_FAIL = new ErrorCode(1_030_603_002, "反审核失败，只有已审核的银行转账单才能反审核");
    ErrorCode FINANCE_TRANSFER_APPROVE_FAIL = new ErrorCode(1_030_603_003, "审核失败，只有未审核的银行转账单才能审核");
    ErrorCode FINANCE_TRANSFER_NO_EXISTS = new ErrorCode(1_030_603_004, "生成银行转账单号失败，请重新提交");
    ErrorCode FINANCE_TRANSFER_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_603_005, "银行转账单({})已审核，无法修改");
    ErrorCode FINANCE_TRANSFER_ACCOUNTS_SAME = new ErrorCode(1_030_603_006, "转出账户与转入账户不能相同");

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
    ErrorCode BOOK_OPEN_DUPLICATE = new ErrorCode(1_030_611_001, "该期间已开账，请勿重复操作");
    ErrorCode BOOK_OPEN_NO_EXISTS = new ErrorCode(1_030_611_002, "生成开账编号失败");
    ErrorCode BOOK_OPEN_PERIOD_INVALID = new ErrorCode(1_030_611_005, "开账期间不合法：会计年应在 1900~9999 之间，会计期应在 1~12 之间");

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
    ErrorCode VOUCHER_ATTRIBUTION_BOOK_NOT_OPEN = new ErrorCode(1_030_613_004, "该期间未开账或未启用对应凭证类型");

    // ========== ERP 凭证字 1-030-614-000 ==========
    ErrorCode VOUCHER_WORD_NOT_EXISTS = new ErrorCode(1_030_614_000, "凭证字不存在");

    // ========== ERP 报表模板 1-030-615-000 ==========
    ErrorCode REPORT_TEMPLATE_NOT_EXISTS = new ErrorCode(1_030_615_000, "报表项目不存在");

    // ========== ERP 科目辅助核算 1-030-616-000 ==========
    ErrorCode SUBJECT_AUXILIARY_NOT_EXISTS = new ErrorCode(1_030_616_000, "科目辅助核算不存在");
    ErrorCode SUBJECT_AUXILIARY_DUPLICATE = new ErrorCode(1_030_616_001, "科目({})已绑定该辅助核算类型");

    // ========== ERP 其他应收单 1-030-617-000 ==========
    ErrorCode OTHER_RECEIVABLE_NOT_EXISTS = new ErrorCode(1_030_617_000, "其他应收单不存在");
    ErrorCode OTHER_RECEIVABLE_DELETE_FAIL_APPROVE = new ErrorCode(1_030_617_001, "其他应收单({})已审核，无法删除");
    ErrorCode OTHER_RECEIVABLE_PROCESS_FAIL = new ErrorCode(1_030_617_002, "反审核失败，只有已审核的其他应收单才能反审核");
    ErrorCode OTHER_RECEIVABLE_APPROVE_FAIL = new ErrorCode(1_030_617_003, "审核失败，只有未审核的其他应收单才能审核");
    ErrorCode OTHER_RECEIVABLE_NO_EXISTS = new ErrorCode(1_030_617_004, "生成其他应收单号失败，请重新提交");
    ErrorCode OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_617_005, "其他应收单({})已审核，无法修改");
    ErrorCode OTHER_RECEIVABLE_UPDATE_FAIL_STATUS_CHANGED = new ErrorCode(1_030_617_006, "其他应收单状态已变更，请刷新后重试");
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

}
