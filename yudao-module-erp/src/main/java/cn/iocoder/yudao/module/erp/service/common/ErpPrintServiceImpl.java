package cn.iocoder.yudao.module.erp.service.common;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintRecordCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintTemplateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintTemplateSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintTemplateDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintTemplateMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.print.ErpPrintModuleEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinancePaymentService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceReceiptService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceTransferService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseService;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableMiscService;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableOtherService;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableMiscService;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherIncomeService;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInvoiceService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchasePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleQuoteService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockCheckService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRINT_MODULE_NOT_SUPPORTED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRINT_TEMPLATE_NOT_EXISTS;

@Service
@Validated
public class ErpPrintServiceImpl implements ErpPrintService {

    public static final String MODULE_PURCHASE_ORDER = "purchase_order";

    @Resource
    private ErpPrintTemplateMapper printTemplateMapper;
    @Resource
    private ErpPrintRecordMapper printRecordMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private ErpPurchaseOrderService purchaseOrderService;
    @Resource
    private ErpPurchaseInService purchaseInService;
    @Resource
    private ErpPurchaseReturnService purchaseReturnService;
    @Resource
    private ErpPurchaseInvoiceService purchaseInvoiceService;
    @Resource
    private ErpPurchasePriceAdjustService purchasePriceAdjustService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpSaleOrderService saleOrderService;
    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private ErpSaleReturnService saleReturnService;
    @Resource
    private ErpSaleQuoteService saleQuoteService;
    @Resource
    private ErpSalePriceAdjustService salePriceAdjustService;
    @Resource
    private ErpSaleCartService saleCartService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpFinancePaymentService financePaymentService;
    @Resource
    private ErpFinanceReceiptService financeReceiptService;
    @Resource
    private ErpPayableMiscService payableMiscService;
    @Resource
    private ErpPayableOtherService payableOtherService;
    @Resource
    private ErpPayableExpenseService payableExpenseService;
    @Resource
    private ErpReceivableMiscService receivableMiscService;
    @Resource
    private ErpReceivableOtherService receivableOtherService;
    @Resource
    private ErpReceivableOtherIncomeService receivableOtherIncomeService;
    @Resource
    private ErpReceivableAccountMapper receivableAccountMapper;
    @Resource
    private ErpFinanceTransferService financeTransferService;
    @Resource
    private ErpVoucherService voucherService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpStockInService stockInService;
    @Resource
    private ErpStockMoveService stockMoveService;
    @Resource
    private ErpStockOutService stockOutService;
    @Resource
    private ErpStockOutBillService stockOutBillService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpWarehouseMoveService warehouseMoveService;
    @Resource
    private ErpStockCheckService stockCheckService;
    @Resource
    private ErpWarehouseService warehouseService;

    @Override
    public ErpPrintFieldRespVO getFields(String moduleKey) {
        ErpPrintModuleEnum module = validateModule(moduleKey);
        assertTemplatePermission(module);
        ErpPrintFieldRespVO respVO = new ErpPrintFieldRespVO();
        respVO.setModuleKey(moduleKey);
        respVO.setGroups(buildFields(module));
        return respVO;
    }

    @Override
    public Map<String, Object> getPrintData(String moduleKey, Long businessId) {
        ErpPrintModuleEnum module = validateModule(moduleKey);
        assertPrintPermission(module);
        return switch (module) {
            case PURCHASE_ORDER -> buildPurchaseOrderPrintData(businessId);
            case PURCHASE_IN -> buildPrintData(module, purchaseInService.getPurchaseIn(businessId),
                    purchaseInService.getPurchaseInItemListByInId(businessId));
            case PURCHASE_RETURN -> buildPrintData(module, purchaseReturnService.getPurchaseReturn(businessId),
                    purchaseReturnService.getPurchaseReturnItemListByReturnId(businessId));
            case PURCHASE_INVOICE -> buildPrintData(module, purchaseInvoiceService.getPurchaseInvoice(businessId),
                    purchaseInvoiceService.getPurchaseInvoiceItemListByInvoiceId(businessId));
            case PURCHASE_PRICE_ADJUST -> buildPrintData(module,
                    purchasePriceAdjustService.getPurchasePriceAdjust(businessId),
                    purchasePriceAdjustService.getPurchasePriceAdjustItemListByAdjustId(businessId));
            case SALE_ORDER -> buildPrintData(module, saleOrderService.getSaleOrder(businessId),
                    saleOrderService.getSaleOrderItemListByOrderId(businessId));
            case SALE_OUT -> buildSaleOutPrintData(businessId);
            case SALE_RETURN -> buildPrintData(module, saleReturnService.getSaleReturn(businessId),
                    saleReturnService.getSaleReturnItemListByReturnId(businessId));
            case SALE_QUOTE -> buildPrintData(module, saleQuoteService.getSaleQuote(businessId),
                    saleQuoteService.getSaleQuoteItemListByQuoteId(businessId));
            case SALE_PRICE_ADJUST -> buildPrintData(module,
                    salePriceAdjustService.getSalePriceAdjust(businessId),
                    salePriceAdjustService.getSalePriceAdjustItemListByAdjustId(businessId));
            case SALE_CART -> buildPrintData(module, saleCartService.getSaleCart(businessId),
                    saleCartService.getSaleCartItemListByCartId(businessId));
            case FINANCE_PAYMENT -> buildPrintData(module, financePaymentService.getFinancePayment(businessId),
                    financePaymentService.getFinancePaymentItemListByPaymentId(businessId));
            case FINANCE_RECEIPT -> buildPrintData(module, financeReceiptService.getFinanceReceipt(businessId),
                    financeReceiptService.getFinanceReceiptItemListByReceiptId(businessId));
            case PAYABLE_MISC -> buildPayableMiscPrintData(businessId);
            case PAYABLE_OTHER -> buildOtherPayablePrintData(businessId);
            case PAYABLE_EXPENSE -> buildPrintData(module, payableExpenseService.getPayableExpense(businessId),
                    payableExpenseService.getPayableExpenseItemListByExpenseId(businessId));
            case RECEIVABLE_MISC -> buildReceivableMiscPrintData(businessId);
            case RECEIVABLE_OTHER -> buildOtherReceivablePrintData(businessId);
            case RECEIVABLE_OTHER_INCOME -> buildOtherIncomePrintData(businessId);
            case FINANCE_TRANSFER -> buildPrintData(module, financeTransferService.getFinanceTransfer(businessId),
                    Collections.emptyList());
            case ACCOUNTING_VOUCHER -> buildVoucherPrintData(businessId);
            case STOCK_IN -> buildStockInPrintData(businessId);
            case STOCK_OUT -> buildStockOutPrintData(businessId);
            case STOCK_TRANSFER_OUT -> buildStockTransferOutPrintData(businessId);
            case WAREHOUSE_MOVE -> buildWarehouseMovePrintData(businessId);
            case STOCK_CHECK -> buildStockCheckPrintData(businessId);
        };
    }

    @Override
    public ErpPrintTemplateRespVO getDefaultTemplate(String moduleKey) {
        ErpPrintModuleEnum module = validateTemplateModuleKey(moduleKey);
        assertReadPermission(module);
        ErpPrintTemplateDO template = printTemplateMapper.selectDefaultByModuleKey(moduleKey);
        return template == null ? null : BeanUtils.toBean(template, ErpPrintTemplateRespVO.class);
    }

    @Override
    public List<ErpPrintTemplateRespVO> getTemplateList(String moduleKey) {
        ErpPrintModuleEnum module = validateTemplateModuleKey(moduleKey);
        assertReadPermission(module);
        return BeanUtils.toBean(printTemplateMapper.selectListByModuleKey(moduleKey), ErpPrintTemplateRespVO.class);
    }

    @Override
    public ErpPrintTemplateRespVO getTemplate(Long id) {
        ErpPrintTemplateDO template = validateTemplateExists(id);
        assertReadPermission(validateTemplateModuleKey(template.getModuleKey()));
        return BeanUtils.toBean(template, ErpPrintTemplateRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTemplate(ErpPrintTemplateSaveReqVO reqVO) {
        ErpPrintModuleEnum module = validateTemplateModuleKey(reqVO.getModuleKey());
        assertTemplatePermission(module);
        ErpPrintTemplateDO saveObj = BeanUtils.toBean(reqVO, ErpPrintTemplateDO.class);
        if (saveObj.getStatus() == null) {
            saveObj.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        if (saveObj.getId() == null) {
            saveObj.setDefaulted(false);
            printTemplateMapper.insert(saveObj);
            return saveObj.getId();
        }
        ErpPrintTemplateDO oldTemplate = validateTemplateExists(saveObj.getId());
        ErpPrintModuleEnum oldModule = validateTemplateModuleKey(oldTemplate.getModuleKey());
        assertTemplatePermission(oldModule);
        saveObj.setModuleKey(oldTemplate.getModuleKey());
        saveObj.setDefaulted(oldTemplate.getDefaulted());
        printTemplateMapper.updateById(saveObj);
        return saveObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAsTemplate(ErpPrintTemplateSaveReqVO reqVO) {
        reqVO.setId(null);
        reqVO.setDefaulted(false);
        return saveTemplate(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultTemplate(Long id) {
        ErpPrintTemplateDO template = validateTemplateExists(id);
        ErpPrintModuleEnum module = validateTemplateModuleKey(template.getModuleKey());
        assertTemplatePermission(module);
        printTemplateMapper.clearDefaultByModuleKey(template.getModuleKey());
        printTemplateMapper.updateById(new ErpPrintTemplateDO()
                .setId(id)
                .setDefaulted(true)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()));
    }

    @Override
    public void recordPrint(ErpPrintRecordCreateReqVO reqVO) {
        ErpPrintModuleEnum module = validateModule(reqVO.getModuleKey());
        assertPrintPermission(module);
        Long loginUserId = getLoginUserId();
        AdminUserRespDTO user = loginUserId == null ? null : adminUserApi.getUser(loginUserId);
        printRecordMapper.insert(ErpPrintRecordDO.builder()
                .moduleKey(reqVO.getModuleKey())
                .businessId(reqVO.getBusinessId())
                .businessNo(reqVO.getBusinessNo())
                .templateId(reqVO.getTemplateId())
                .printerId(loginUserId)
                .printerName(user == null ? null : user.getNickname())
                .printTime(LocalDateTime.now())
                .build());
    }

    @Override
    public Map<Long, Long> getPrintCountMap(String moduleKey, Collection<Long> businessIds) {
        if (CollUtil.isEmpty(businessIds)) {
            return new LinkedHashMap<>();
        }
        Map<Long, Long> result = new LinkedHashMap<>();
        businessIds.forEach(id -> result.put(id, 0L));
        printRecordMapper.selectListByBusinessIds(moduleKey, businessIds)
                .forEach(record -> result.compute(record.getBusinessId(), (key, value) -> value == null ? 1L : value + 1L));
        return result;
    }

    @Override
    public Map<Long, LocalDateTime> getLastPrintTimeMap(String moduleKey, Collection<Long> businessIds) {
        if (CollUtil.isEmpty(businessIds)) {
            return new LinkedHashMap<>();
        }
        Map<Long, LocalDateTime> result = new LinkedHashMap<>();
        printRecordMapper.selectListByBusinessIds(moduleKey, businessIds).forEach(record -> {
            if (!result.containsKey(record.getBusinessId())) {
                result.put(record.getBusinessId(), record.getPrintTime());
            }
        });
        return result;
    }

    private Map<String, Object> buildPurchaseOrderPrintData(Long businessId) {
        ErpPurchaseOrderDO order = purchaseOrderService.getPurchaseOrder(businessId);
        if (order == null) {
            return null;
        }
        return buildPrintData(ErpPrintModuleEnum.PURCHASE_ORDER, order,
                purchaseOrderService.getPurchaseOrderItemListByOrderId(businessId));
    }

    private Map<String, Object> buildVoucherPrintData(Long businessId) {
        ErpVoucherDO voucher = voucherService.getVoucher(businessId);
        if (voucher == null) {
            return null;
        }
        List<ErpVoucherItemDO> items = voucherService.getVoucherItemListByVoucherId(businessId);
        Map<String, Object> data = buildPrintData(ErpPrintModuleEnum.ACCOUNTING_VOUCHER, voucher, items);
        putVoucherPeriod(data, voucher);
        return data;
    }

    private Map<String, Object> buildOtherReceivablePrintData(Long businessId) {
        ErpReceivableOtherDO receivable = receivableOtherService.getReceivableOther(businessId);
        return buildPrintData(ErpPrintModuleEnum.RECEIVABLE_OTHER, receivable, Collections.emptyList());
    }

    private Map<String, Object> buildReceivableMiscPrintData(Long businessId) {
        ErpReceivableMiscDO receivable = receivableMiscService.getReceivableMisc(businessId);
        return buildPrintData(ErpPrintModuleEnum.RECEIVABLE_MISC, receivable, Collections.emptyList());
    }

    private Map<String, Object> buildOtherPayablePrintData(Long businessId) {
        ErpPayableOtherDO payable = payableOtherService.getPayableOther(businessId);
        return buildPrintData(ErpPrintModuleEnum.PAYABLE_OTHER, payable, Collections.emptyList());
    }

    private Map<String, Object> buildPayableMiscPrintData(Long businessId) {
        ErpPayableMiscDO payable = payableMiscService.getPayableMisc(businessId);
        return buildPrintData(ErpPrintModuleEnum.PAYABLE_MISC, payable, Collections.emptyList());
    }

    private Map<String, Object> buildOtherIncomePrintData(Long businessId) {
        ErpReceivableOtherIncomeDO income = receivableOtherIncomeService.getOtherIncome(businessId);
        return buildPrintData(ErpPrintModuleEnum.RECEIVABLE_OTHER_INCOME, income,
                receivableOtherIncomeService.getOtherIncomeItemListByIncomeId(businessId));
    }

    private Map<String, Object> buildSaleOutPrintData(Long businessId) {
        ErpSaleOutDO saleOut = saleOutService.getSaleOut(businessId);
        if (saleOut == null) {
            return null;
        }
        Map<String, Object> data = buildPrintData(ErpPrintModuleEnum.SALE_OUT, saleOut,
                saleOutService.getSaleOutItemListByOutId(businessId));
        enrichSaleOutPrintData(data, saleOut);
        return data;
    }

    private Map<String, Object> buildStockTransferOutPrintData(Long businessId) {
        ErpStockMoveDO stockMove = stockMoveService.getVisibleStockTransferOut(businessId);
        if (stockMove == null) {
            return null;
        }
        List<ErpStockMoveItemDO> items = stockMoveService.getStockMoveItemListByMoveId(businessId);
        Map<String, Object> data = buildPrintData(ErpPrintModuleEnum.STOCK_TRANSFER_OUT, stockMove, items);
        enrichStockTransferOutPrintData(data, stockMove);
        return data;
    }

    private Map<String, Object> buildWarehouseMovePrintData(Long businessId) {
        ErpWarehouseMoveDO warehouseMove = warehouseMoveService.getWarehouseMove(businessId);
        if (warehouseMove == null) {
            return null;
        }
        List<ErpWarehouseMoveItemDO> items = warehouseMoveService.getWarehouseMoveItemListByMoveId(businessId);
        return buildPrintData(ErpPrintModuleEnum.WAREHOUSE_MOVE, warehouseMove, items);
    }

    private Map<String, Object> buildStockOutPrintData(Long businessId) {
        ErpStockOutDO stockOut = stockOutService.getStockOut(businessId);
        if (stockOut == null) {
            return null;
        }
        List<ErpStockOutItemDO> items = stockOutService.getStockOutItemListByOutId(businessId);
        return buildPrintData(ErpPrintModuleEnum.STOCK_OUT, stockOut, items);
    }

    private Map<String, Object> buildStockInPrintData(Long businessId) {
        ErpStockInDO stockIn = stockInService.getStockIn(businessId);
        if (stockIn == null) {
            return null;
        }
        List<ErpStockInItemDO> items = stockInService.getStockInItemListByInId(businessId);
        Map<String, Object> data = buildPrintData(ErpPrintModuleEnum.STOCK_IN, stockIn, items);
        putStockInAggregateFields(data);
        return data;
    }

    @SuppressWarnings("unchecked")
    private void putStockInAggregateFields(Map<String, Object> data) {
        Object itemRowsObj = data.get("items");
        int itemCount = itemRowsObj instanceof List<?> itemRows ? itemRows.size() : 0;
        String warehouseNames = "";
        if (itemRowsObj instanceof List<?> itemRows) {
            Set<String> names = new LinkedHashSet<>();
            itemRows.forEach(itemRow -> {
                if (!(itemRow instanceof Map<?, ?> row)) {
                    return;
                }
                Object value = row.get("items.warehouseName");
                if (value != null && StringUtils.hasText(String.valueOf(value))) {
                    names.add(String.valueOf(value));
                }
            });
            warehouseNames = CollUtil.join(names, ", ");
        }
        Object document = data.get("document");
        Object main = data.get("main");
        if (document instanceof Map<?, ?> documentMap) {
            ((Map<String, Object>) documentMap).put("itemCount", itemCount);
            ((Map<String, Object>) documentMap).put("warehouseNames", warehouseNames);
        }
        if (main instanceof Map<?, ?> mainMap) {
            ((Map<String, Object>) mainMap).put("document.itemCount", itemCount);
            ((Map<String, Object>) mainMap).put("document.warehouseNames", warehouseNames);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildStockCheckPrintData(Long businessId) {
        ErpStockCheckDO stockCheck = stockCheckService.getStockCheck(businessId);
        if (stockCheck == null) {
            return null;
        }
        List<ErpStockCheckItemDO> items = stockCheckService.getStockCheckItemListByCheckId(businessId);
        Map<String, Object> data = buildPrintData(ErpPrintModuleEnum.STOCK_CHECK, stockCheck, items);
        Object document = data.get("document");
        Object main = data.get("main");
        String checkTypeName = formatStockCheckType(stockCheck.getCheckType());
        String statusName = formatAuditStatus(stockCheck.getStatus());
        if (document instanceof Map<?, ?> documentMap) {
            ((Map<String, Object>) documentMap).put("checkTypeName", checkTypeName);
            ((Map<String, Object>) documentMap).put("statusName", statusName);
        }
        if (main instanceof Map<?, ?> mainMap) {
            ((Map<String, Object>) mainMap).put("document.checkTypeName", checkTypeName);
            ((Map<String, Object>) mainMap).put("document.statusName", statusName);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private void putVoucherPeriod(Map<String, Object> data, ErpVoucherDO voucher) {
        if (voucher.getPeriodYear() == null || voucher.getPeriodMonth() == null) {
            return;
        }
        String period = String.format("%d-%02d", voucher.getPeriodYear(), voucher.getPeriodMonth());
        Object document = data.get("document");
        if (document instanceof Map<?, ?> documentMap) {
            ((Map<String, Object>) documentMap).put("period", period);
        }
        Object main = data.get("main");
        if (main instanceof Map<?, ?> mainMap) {
            ((Map<String, Object>) mainMap).put("document.period", period);
        }
    }

    private Map<String, Object> buildPrintData(ErpPrintModuleEnum module, Object document, List<?> items) {
        if (document == null) {
            return null;
        }
        Map<String, Object> documentMap = toDisplayMap(document);
        Map<String, Object> main = new LinkedHashMap<>();
        documentMap.forEach((key, value) -> {
            main.put("document." + key, value);
            if (module == ErpPrintModuleEnum.PURCHASE_ORDER) {
                main.put("order." + key, value);
            }
        });
        addAmountUpper(main, "document.totalPriceUpper", documentMap.get("totalPrice"));
        addAmountUpper(main, "document.totalAmountUpper", documentMap.get("totalAmount"));
        if (module == ErpPrintModuleEnum.PURCHASE_ORDER) {
            addAmountUpper(main, "order.totalPriceUpper", documentMap.get("totalPrice"));
        }

        RelatedMaps relatedMaps = loadRelatedMaps(documentMap, items);
        enrichMain(main, documentMap, relatedMaps);
        List<Map<String, Object>> itemRows = buildPrintItems(module, items, relatedMaps);
        Map<String, Object> system = buildPrintSystem();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduleKey", module.getKey());
        result.put("moduleName", module.getName());
        result.put("document", documentMap);
        result.put("supplier", simpleNameMap(relatedMaps.supplier(documentMap.get("supplierId"))));
        result.put("customer", simpleNameMap(relatedMaps.customer(documentMap.get("customerId"))));
        result.put("dept", simpleNameMap(relatedMaps.dept(documentMap.get("deptId"))));
        result.put("fromDept", simpleNameMap(relatedMaps.dept(documentMap.get("fromDeptId"))));
        result.put("toDept", simpleNameMap(relatedMaps.dept(documentMap.get("toDeptId"))));
        result.put("warehouse", simpleNameMap(relatedMaps.warehouse(documentMap.get("warehouseId"))));
        result.put("fromWarehouse", simpleNameMap(relatedMaps.warehouse(documentMap.get("fromWarehouseId"))));
        result.put("toWarehouse", simpleNameMap(relatedMaps.warehouse(documentMap.get("toWarehouseId"))));
        result.put("account", simpleNameMap(relatedMaps.account(documentMap.get("accountId"))));
        result.put("outAccount", simpleNameMap(relatedMaps.account(documentMap.get("outAccountId"))));
        result.put("inAccount", simpleNameMap(relatedMaps.account(documentMap.get("inAccountId"))));
        result.put("financeUser", simpleUserMap(firstUser(relatedMaps, documentMap.get("financeUserId"))));
        result.put("approveUser", simpleUserMap(firstUser(relatedMaps, documentMap.get("approveUserId"))));
        result.put("main", main);
        result.put("items", itemRows);
        result.put("system", system);
        result.put("currentUser", system);
        result.put("print", system);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void enrichSaleOutPrintData(Map<String, Object> data, ErpSaleOutDO saleOut) {
        if (data == null || saleOut == null) {
            return;
        }
        Map<String, Object> documentMap = (Map<String, Object>) data.get("document");
        Map<String, Object> mainMap = (Map<String, Object>) data.get("main");
        if (documentMap == null || mainMap == null) {
            return;
        }

        if (saleOut.getCustomerId() != null) {
            ErpCustomerDO customer = customerService.getCustomer(saleOut.getCustomerId());
            putDocumentField(documentMap, mainMap, "customerCode", customer == null ? null : customer.getCode());
            putDocumentField(documentMap, mainMap, "customerAddress", customer == null ? null : customer.getAddress());
            putDocumentField(documentMap, mainMap, "customerPhone",
                    customer == null ? null : firstNonBlank(customer.getMobile(), customer.getTelephone()));
        }
        putDocumentField(documentMap, mainMap, "statusName", formatSaleOutAuditStatus(saleOut.getStatus()));
        putDocumentField(documentMap, mainMap, "settleStatusName", formatSaleOutSettleStatus(saleOut.getSettleStatus()));
        putDocumentField(documentMap, mainMap, "feeAmount",
                firstNonBlank(saleOut.getFeeAmount(), saleOut.getExtraFee(), saleOut.getOtherPrice()));
        putDocumentField(documentMap, mainMap, "reductionAmount",
                firstNonBlank(saleOut.getReductionAmount(), saleOut.getDiscountPrice()));
        putDocumentField(documentMap, mainMap, "afterReductionAmount",
                firstNonBlank(saleOut.getAfterReductionAmount(), saleOut.getTotalPrice()));
        putDocumentField(documentMap, mainMap, "totalAmount", saleOut.getTotalPrice());
        addAmountUpper(mainMap, "document.totalAmountUpper", saleOut.getTotalPrice());
        enrichSaleOutReceivablePrintData(documentMap, mainMap, saleOut);
        enrichSaleOutOperatorPrintData(documentMap, mainMap, saleOut);

        SaleOutSourceDocumentMeta sourceMeta = getSaleOutSourceDocumentMeta(saleOut);
        if (sourceMeta == null) {
            return;
        }
        putDocumentField(documentMap, mainMap, "sourceCreateTime",
                firstNonBlank(saleOut.getSourceCreateTime(), sourceMeta.createTime()));
        putDocumentField(documentMap, mainMap, "freightType", sourceMeta.freightType());
        Long sourceCreatorId = parseLong(sourceMeta.creator());
        AdminUserRespDTO sourceCreator = sourceCreatorId == null ? null : adminUserApi.getUser(sourceCreatorId);
        putDocumentField(documentMap, mainMap, "sourceCreatorName",
                sourceCreator == null ? null : sourceCreator.getNickname());
    }

    private void enrichSaleOutReceivablePrintData(Map<String, Object> documentMap, Map<String, Object> mainMap,
                                                  ErpSaleOutDO saleOut) {
        BigDecimal currentDebt = nullToZero(saleOut.getTotalPrice());
        putDocumentField(documentMap, mainMap, "currentDebt", currentDebt);
        if (saleOut.getCustomerId() == null) {
            putDocumentField(documentMap, mainMap, "previousReceivable", BigDecimal.ZERO);
            putDocumentField(documentMap, mainMap, "totalDebt", currentDebt);
            return;
        }
        ErpReceivableAccountDO account = receivableAccountMapper.selectByCustomerId(saleOut.getCustomerId());
        BigDecimal receivableBalance = account == null ? BigDecimal.ZERO : nullToZero(account.getReceivableBalance());
        putDocumentField(documentMap, mainMap, "previousReceivable", receivableBalance);
        putDocumentField(documentMap, mainMap, "totalDebt", receivableBalance.add(currentDebt));
    }

    private void enrichSaleOutOperatorPrintData(Map<String, Object> documentMap, Map<String, Object> mainMap,
                                                ErpSaleOutDO saleOut) {
        Long auditorId = saleOut.getAuditorId();
        AdminUserRespDTO auditor = auditorId == null ? null : adminUserApi.getUser(auditorId);
        putDocumentField(documentMap, mainMap, "checkerName", auditor == null ? null : auditor.getNickname());
        putDocumentField(documentMap, mainMap, "pickerName", collectSaleOutPickerNames(saleOut.getId()));
    }

    private String collectSaleOutPickerNames(Long saleOutId) {
        if (saleOutId == null || stockOutBillService == null) {
            return "";
        }
        List<ErpStockOutBillDO> bills = stockOutBillService.getStockOutBillListBySaleOutId(saleOutId);
        if (CollUtil.isEmpty(bills)) {
            return "";
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (ErpStockOutBillDO bill : bills) {
            Object name = firstNonBlank(bill.getPickUserName(), bill.getPick());
            String text = name == null ? null : String.valueOf(name);
            if (StringUtils.hasText(text)) {
                names.add(text);
            }
        }
        return String.join("、", names);
    }

    @SuppressWarnings("unchecked")
    private void enrichStockTransferOutPrintData(Map<String, Object> data, ErpStockMoveDO stockMove) {
        if (data == null || stockMove == null
                || !ErpSaleBizSourceTypeEnum.CART.getType().equals(stockMove.getSourceType())
                || stockMove.getSourceId() == null) {
            return;
        }
        Map<String, Object> documentMap = (Map<String, Object>) data.get("document");
        Map<String, Object> mainMap = (Map<String, Object>) data.get("main");
        if (documentMap == null || mainMap == null) {
            return;
        }
        ErpSaleCartDO cart = saleCartService.getSaleCart(stockMove.getSourceId());
        if (cart == null || cart.getCustomerId() == null) {
            return;
        }
        ErpCustomerDO customer = DataPermissionUtils.executeIgnore(() -> customerService.getCustomer(cart.getCustomerId()));
        putDocumentField(documentMap, mainMap, "directCustomerName", customer == null ? null : customer.getName());
    }

    private void putDocumentField(Map<String, Object> documentMap, Map<String, Object> mainMap,
                                  String fieldKey, Object value) {
        Object formatted = formatPrintValue(value);
        documentMap.put(fieldKey, formatted);
        mainMap.put("document." + fieldKey, formatted);
    }

    private void enrichMain(Map<String, Object> main, Map<String, Object> documentMap, RelatedMaps relatedMaps) {
        ErpSupplierDO supplier = relatedMaps.supplier(documentMap.get("supplierId"));
        ErpCustomerDO customer = relatedMaps.customer(documentMap.get("customerId"));
        DeptRespDTO dept = relatedMaps.dept(documentMap.get("deptId"));
        DeptRespDTO fromDept = relatedMaps.dept(documentMap.get("fromDeptId"));
        DeptRespDTO toDept = relatedMaps.dept(documentMap.get("toDeptId"));
        ErpWarehouseDO warehouse = relatedMaps.warehouse(documentMap.get("warehouseId"));
        ErpWarehouseDO fromWarehouse = relatedMaps.warehouse(documentMap.get("fromWarehouseId"));
        ErpWarehouseDO toWarehouse = relatedMaps.warehouse(documentMap.get("toWarehouseId"));
        ErpAccountDO account = relatedMaps.account(documentMap.get("accountId"));
        ErpAccountDO outAccount = relatedMaps.account(documentMap.get("outAccountId"));
        ErpAccountDO inAccount = relatedMaps.account(documentMap.get("inAccountId"));
        AdminUserRespDTO handler = firstUser(relatedMaps, documentMap.get("handlerId"), documentMap.get("handler"));
        AdminUserRespDTO adjuster = firstUser(relatedMaps, documentMap.get("adjuster"), documentMap.get("adjustUserId"));
        AdminUserRespDTO purchaser = firstUser(relatedMaps, documentMap.get("purchaser"));
        AdminUserRespDTO saleUser = firstUser(relatedMaps, documentMap.get("saleUserId"));
        AdminUserRespDTO developer = firstUser(relatedMaps, documentMap.get("developerUserId"));
        AdminUserRespDTO auditor = firstUser(relatedMaps, documentMap.get("auditorId"));
        AdminUserRespDTO firstAuditor = firstUser(relatedMaps, documentMap.get("firstAuditUserId"));
        AdminUserRespDTO finalAuditor = firstUser(relatedMaps, documentMap.get("finalAuditUserId"));
        AdminUserRespDTO financeUser = firstUser(relatedMaps, documentMap.get("financeUserId"));
        AdminUserRespDTO approveUser = firstUser(relatedMaps, documentMap.get("approveUserId"));
        AdminUserRespDTO creator = firstUser(relatedMaps, documentMap.get("creator"));
        AdminUserRespDTO updater = firstUser(relatedMaps, documentMap.get("updater"));
        putIfPresent(main, "supplier.name", supplier == null ? null : supplier.getName());
        putIfPresent(main, "customer.name", customer == null ? null : customer.getName());
        putIfPresent(main, "dept.name", dept == null ? null : dept.getName());
        putIfPresent(main, "fromDept.name", fromDept == null ? null : fromDept.getName());
        putIfPresent(main, "toDept.name", toDept == null ? null : toDept.getName());
        putIfPresent(main, "warehouse.name", warehouse == null ? null : warehouse.getName());
        putIfPresent(main, "fromWarehouse.name", fromWarehouse == null ? null : fromWarehouse.getName());
        putIfPresent(main, "toWarehouse.name", toWarehouse == null ? null : toWarehouse.getName());
        putIfPresent(main, "account.name", account == null ? null : account.getName());
        putIfPresent(main, "outAccount.name", outAccount == null ? null : outAccount.getName());
        putIfPresent(main, "inAccount.name", inAccount == null ? null : inAccount.getName());
        putIfPresent(main, "handler.nickname", handler == null ? null : handler.getNickname());
        putIfPresent(main, "adjuster.nickname", adjuster == null ? null : adjuster.getNickname());
        putIfPresent(main, "adjustUser.nickname", adjuster == null ? null : adjuster.getNickname());
        putIfPresent(main, "purchaser.nickname", purchaser == null ? null : purchaser.getNickname());
        putIfPresent(main, "saleUser.nickname", saleUser == null ? null : saleUser.getNickname());
        putIfPresent(main, "developer.nickname", developer == null ? null : developer.getNickname());
        putIfPresent(main, "auditor.nickname", auditor == null ? null : auditor.getNickname());
        putIfPresent(main, "firstAuditor.nickname", firstAuditor == null ? null : firstAuditor.getNickname());
        putIfPresent(main, "finalAuditor.nickname", finalAuditor == null ? null : finalAuditor.getNickname());
        putIfPresent(main, "financeUser.nickname", financeUser == null ? null : financeUser.getNickname());
        putIfPresent(main, "approveUser.nickname", approveUser == null ? null : approveUser.getNickname());
        putIfPresent(main, "creator.nickname", creator == null ? null : creator.getNickname());
        putIfPresent(main, "updater.nickname", updater == null ? null : updater.getNickname());
    }

    private List<Map<String, Object>> buildPrintItems(ErpPrintModuleEnum module, List<?> items,
                                                      RelatedMaps relatedMaps) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> itemMap = toDisplayMap(items.get(i));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("items.seq", i + 1);
            itemMap.forEach((key, value) -> row.put("items." + key, value));
            ErpProductRespVO product = relatedMaps.product(itemMap.get("productId"));
            ErpWarehouseDO warehouse = relatedMaps.warehouse(itemMap.get("warehouseId"));
            ErpWarehouseDO fromWarehouse = relatedMaps.warehouse(itemMap.get("fromWarehouseId"));
            ErpWarehouseDO toWarehouse = relatedMaps.warehouse(itemMap.get("toWarehouseId"));
            DeptRespDTO dept = relatedMaps.dept(itemMap.get("deptId"));
            DeptRespDTO fromDept = relatedMaps.dept(itemMap.get("fromDeptId"));
            DeptRespDTO toDept = relatedMaps.dept(itemMap.get("toDeptId"));
            putIfPresent(row, "items.productCode", firstNonBlank(itemMap.get("productCode"), itemMap.get("partCode"),
                    product == null ? null : product.getCode()));
            putIfPresent(row, "items.productName", firstNonBlank(itemMap.get("productName"), itemMap.get("partName"),
                    product == null ? null : product.getName()));
            putIfPresent(row, "items.productUnitName", firstNonBlank(itemMap.get("productUnitName"),
                    itemMap.get("unit"), product == null ? null : product.getUnitName()));
            putIfPresent(row, "items.weight", firstNonBlank(itemMap.get("weight"), itemMap.get("unitWeight"),
                    product == null ? null : product.getWeight()));
            putIfPresent(row, "items.packageQty", firstNonBlank(itemMap.get("packageQty"),
                    product == null ? null : product.getPackageQty()));
            if (isPieceCountPrintModule(module)) {
                row.put("items.pieceCount",
                        calculatePieceCount(getPieceCountValue(module, itemMap), row.get("items.packageQty")));
            }
            putIfPresent(row, "items.productBarCode", firstNonBlank(itemMap.get("productBarCode"),
                    product == null ? null : product.getBarCode()));
            putIfPresent(row, "items.warehouseName", warehouse == null ? null : warehouse.getName());
            putIfPresent(row, "items.warehouseId", warehouse == null ? row.get("items.warehouseId") : warehouse.getName());
            putIfPresent(row, "items.deptName", dept == null ? null : dept.getName());
            putIfPresent(row, "items.fromWarehouseName", fromWarehouse == null ? null : fromWarehouse.getName());
            putIfPresent(row, "items.fromWarehouseId", fromWarehouse == null
                    ? row.get("items.fromWarehouseId") : fromWarehouse.getName());
            putIfPresent(row, "items.toWarehouseName", toWarehouse == null ? null : toWarehouse.getName());
            putIfPresent(row, "items.toWarehouseId", toWarehouse == null
                    ? row.get("items.toWarehouseId") : toWarehouse.getName());
            putIfPresent(row, "items.fromDeptName", fromDept == null ? null : fromDept.getName());
            putIfPresent(row, "items.fromDeptId", fromDept == null ? row.get("items.fromDeptId") : fromDept.getName());
            putIfPresent(row, "items.toDeptName", toDept == null ? null : toDept.getName());
            putIfPresent(row, "items.toDeptId", toDept == null ? row.get("items.toDeptId") : toDept.getName());
            putWarehouseMoveStockCount(module, itemMap, row);
            rows.add(row);
        }
        return rows;
    }

    private void putWarehouseMoveStockCount(ErpPrintModuleEnum module, Map<String, Object> itemMap,
                                            Map<String, Object> row) {
        if (module != ErpPrintModuleEnum.WAREHOUSE_MOVE) {
            return;
        }
        Long productId = parseLong(itemMap.get("productId"));
        if (productId == null) {
            return;
        }
        putStockCount(row, "items.fromStockCount", productId, itemMap.get("fromWarehouseId"));
        putStockCount(row, "items.toStockCount", productId, itemMap.get("toWarehouseId"));
    }

    private void putStockCount(Map<String, Object> row, String key, Long productId, Object warehouseIdValue) {
        Long warehouseId = parseLong(warehouseIdValue);
        if (warehouseId == null) {
            return;
        }
        ErpStockDO stock = DataPermissionUtils.executeIgnore(() -> stockService.getStock(productId, warehouseId));
        row.put(key, formatPrintValue(stock == null ? BigDecimal.ZERO : stock.getCount()));
    }

    private RelatedMaps loadRelatedMaps(Map<String, Object> documentMap, List<?> items) {
        Set<Long> supplierIds = new LinkedHashSet<>();
        Set<Long> customerIds = new LinkedHashSet<>();
        Set<Long> accountIds = new LinkedHashSet<>();
        Set<Long> deptIds = new LinkedHashSet<>();
        Set<Long> productIds = new LinkedHashSet<>();
        Set<Long> warehouseIds = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();

        addLong(supplierIds, documentMap.get("supplierId"));
        addLong(customerIds, documentMap.get("customerId"));
        addLong(accountIds, documentMap.get("accountId"));
        addLong(accountIds, documentMap.get("outAccountId"));
        addLong(accountIds, documentMap.get("inAccountId"));
        addLong(deptIds, documentMap.get("deptId"));
        addLong(deptIds, documentMap.get("fromDeptId"));
        addLong(deptIds, documentMap.get("toDeptId"));
        addLong(warehouseIds, documentMap.get("warehouseId"));
        addLong(warehouseIds, documentMap.get("fromWarehouseId"));
        addLong(warehouseIds, documentMap.get("toWarehouseId"));
        addLong(userIds, documentMap.get("handlerId"));
        addLong(userIds, documentMap.get("handler"));
        addLong(userIds, documentMap.get("adjuster"));
        addLong(userIds, documentMap.get("adjustUserId"));
        addLong(userIds, documentMap.get("purchaser"));
        addLong(userIds, documentMap.get("saleUserId"));
        addLong(userIds, documentMap.get("developerUserId"));
        addLong(userIds, documentMap.get("auditorId"));
        addLong(userIds, documentMap.get("firstAuditUserId"));
        addLong(userIds, documentMap.get("finalAuditUserId"));
        addLong(userIds, documentMap.get("financeUserId"));
        addLong(userIds, documentMap.get("approveUserId"));
        addLong(userIds, documentMap.get("creator"));
        addLong(userIds, documentMap.get("updater"));

        if (CollUtil.isNotEmpty(items)) {
            for (Object item : items) {
                Map<String, Object> itemMap = toRawMap(item);
                addLong(productIds, itemMap.get("productId"));
                addLong(warehouseIds, itemMap.get("warehouseId"));
                addLong(warehouseIds, itemMap.get("fromWarehouseId"));
                addLong(warehouseIds, itemMap.get("toWarehouseId"));
                addLong(deptIds, itemMap.get("deptId"));
                addLong(deptIds, itemMap.get("fromDeptId"));
                addLong(deptIds, itemMap.get("toDeptId"));
            }
        }

        return new RelatedMaps(
                supplierIds.isEmpty() ? Collections.emptyMap() : supplierService.getSupplierMap(supplierIds),
                customerIds.isEmpty() ? Collections.emptyMap() : customerService.getCustomerMap(customerIds),
                accountIds.isEmpty() ? Collections.emptyMap() : loadAccountMap(accountIds),
                deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds),
                productIds.isEmpty() ? Collections.emptyMap()
                        : DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(productIds)),
                warehouseIds.isEmpty() ? Collections.emptyMap()
                        : DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds)),
                userIds.isEmpty() ? Collections.emptyMap() : adminUserApi.getUserMap(userIds)
        );
    }

    private Map<Long, ErpAccountDO> loadAccountMap(Collection<Long> accountIds) {
        Map<Long, ErpAccountDO> result = new LinkedHashMap<>();
        accountIds.forEach(id -> {
            ErpAccountDO account = accountService.getAccount(id);
            if (account != null) {
                result.put(id, account);
            }
        });
        return result;
    }

    private Map<String, Object> buildPrintSystem() {
        AdminUserRespDTO currentUser = getLoginUserId() == null ? null : adminUserApi.getUser(getLoginUserId());
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("print.now", formatDateTime(LocalDateTime.now()));
        system.put("currentUser.nickname", currentUser == null ? null : currentUser.getNickname());
        return system;
    }

    private List<ErpPrintFieldRespVO.Group> buildFields(ErpPrintModuleEnum module) {
        if (module == ErpPrintModuleEnum.STOCK_CHECK) {
            return buildStockCheckFields(module);
        }
        List<ErpPrintFieldRespVO.Field> mainFields = new ArrayList<>();
        List<ErpPrintFieldRespVO.Field> detailFields = new ArrayList<>();
        for (FieldDefinitionRespDTO definition : permissionApi.getFieldDefinitions(module.getFieldModuleKey())) {
            ErpPrintFieldRespVO.Field field = buildField(module, definition);
            if (field == null) {
                continue;
            }
            if (definition.getFieldKey() != null && definition.getFieldKey().startsWith("item_")) {
                detailFields.add(field);
            } else {
                mainFields.add(field);
            }
        }
        if (module == ErpPrintModuleEnum.STOCK_IN) {
            addFieldIfAbsent(mainFields, "仓库", "document.warehouseNames", module.getFieldModuleKey() + " / main_form");
            addFieldIfAbsent(mainFields, "项数", "document.itemCount", module.getFieldModuleKey() + " / main_form");
        }
        if (isPieceCountPrintModule(module)) {
            addFieldIfAbsent(detailFields, "件数", "items.pieceCount", module.getFieldModuleKey() + " / detail_item");
        }
        List<ErpPrintFieldRespVO.Group> groups = new ArrayList<>();
        groups.add(new ErpPrintFieldRespVO.Group("main", "主表字段", "拖到单元格", mainFields));
        groups.add(new ErpPrintFieldRespVO.Group("detail", "明细字段", "拖到明细行", detailFields));
        groups.add(new ErpPrintFieldRespVO.Group("system", "系统字段", "通用", List.of(
                field("打印时间", "print.now", "system"),
                field("当前用户", "currentUser.nickname", "system")
        )));
        return groups;
    }

    private List<ErpPrintFieldRespVO.Group> buildStockCheckFields(ErpPrintModuleEnum module) {
        List<ErpPrintFieldRespVO.Field> mainFields = new ArrayList<>();
        List<ErpPrintFieldRespVO.Field> detailFields = new ArrayList<>();
        for (FieldDefinitionRespDTO definition : permissionApi.getFieldDefinitions(module.getFieldModuleKey(), "base_info")) {
            ErpPrintFieldRespVO.Field field = buildField(module, definition);
            if (field != null) {
                mainFields.add(field);
            }
        }
        for (FieldDefinitionRespDTO definition : permissionApi.getFieldDefinitions(module.getFieldModuleKey(), "detail_item")) {
            ErpPrintFieldRespVO.Field field = buildStockCheckItemField(module, definition);
            if (field != null) {
                detailFields.add(field);
            }
        }
        addFieldIfAbsent(detailFields, "件数", "items.pieceCount", module.getFieldModuleKey() + " / detail_item");
        List<ErpPrintFieldRespVO.Group> groups = new ArrayList<>();
        groups.add(new ErpPrintFieldRespVO.Group("main", "主表字段", "拖到单元格", mainFields));
        groups.add(new ErpPrintFieldRespVO.Group("detail", "明细字段", "拖到明细行", detailFields));
        groups.add(new ErpPrintFieldRespVO.Group("system", "系统字段", "通用", List.of(
                field("打印时间", "print.now", "system"),
                field("当前用户", "currentUser.nickname", "system")
        )));
        return groups;
    }

    private ErpPrintFieldRespVO.Field buildStockCheckItemField(ErpPrintModuleEnum module,
                                                               FieldDefinitionRespDTO definition) {
        if (definition == null || definition.getFieldKey() == null) {
            return null;
        }
        String code = toPrintCode(module, "item_" + definition.getFieldKey());
        return code == null ? null : field(definition.getFieldLabel(), code, module.getFieldModuleKey() + " / detail_item");
    }

    private void addFieldIfAbsent(List<ErpPrintFieldRespVO.Field> fields, String name, String code, String source) {
        boolean exists = fields.stream().anyMatch(field -> code.equals(field.getCode()));
        if (!exists) {
            fields.add(field(name, code, source));
        }
    }

    private ErpPrintFieldRespVO.Field buildField(ErpPrintModuleEnum module, FieldDefinitionRespDTO definition) {
        if (definition == null || definition.getFieldKey() == null) {
            return null;
        }
        String code = toPrintCode(module, definition.getFieldKey());
        if (code == null) {
            return null;
        }
        String source = definition.getFieldKey().startsWith("item_")
                ? module.getFieldModuleKey() + " / detail_item"
                : module.getFieldModuleKey() + " / main_form";
        String name = module == ErpPrintModuleEnum.SALE_OUT && "no".equals(definition.getFieldKey())
                ? "销售单号" : definition.getFieldLabel();
        return field(name, code, source);
    }

    private String toPrintCode(ErpPrintModuleEnum module, String fieldKey) {
        if ("items".equals(fieldKey)) {
            return "items";
        }
        if (module == ErpPrintModuleEnum.ACCOUNTING_VOUCHER && fieldKey.startsWith("col_")) {
            return null;
        }
        if (fieldKey.startsWith("item_")) {
            String itemKey = fieldKey.substring("item_".length());
            return switch (itemKey) {
                case "productId" -> "items.productName";
                case "subjectId" -> "items.subjectName";
                case "warehouseId" -> "items.warehouseName";
                case "fromWarehouseId" -> "items.fromWarehouseName";
                case "toWarehouseId" -> "items.toWarehouseName";
                case "deptId" -> "items.deptName";
                case "fromDeptId" -> "items.fromDeptName";
                case "toDeptId" -> "items.toDeptName";
                case "totalProductPrice" -> "items.totalPrice";
                default -> "items." + lowerFirst(itemKey);
            };
        }
        if (module == ErpPrintModuleEnum.STOCK_TRANSFER_OUT) {
            if (fieldKey.startsWith("col_") || fieldKey.startsWith("query_")) {
                return null;
            }
            return switch (fieldKey) {
                case "deptId" -> "dept.name";
                case "fromDeptId" -> "fromDept.name";
                case "toDeptId" -> "toDept.name";
                case "approveUserId", "approveUserName" -> "approveUser.nickname";
                case "creator", "creatorName" -> "creator.nickname";
                case "updater", "updaterName" -> "updater.nickname";
                default -> "document." + lowerFirst(fieldKey);
            };
        }
        if (module == ErpPrintModuleEnum.WAREHOUSE_MOVE) {
            if (fieldKey.startsWith("col_") || fieldKey.startsWith("query_")) {
                return null;
            }
            return switch (fieldKey) {
                case "deptId" -> "dept.name";
                case "fromWarehouseId" -> "fromWarehouse.name";
                case "toWarehouseId" -> "toWarehouse.name";
                case "handlerId", "handlerName" -> "handler.nickname";
                case "approveUserId", "approveUserName" -> "approveUser.nickname";
                case "creator", "creatorName" -> "creator.nickname";
                case "updater", "updaterName" -> "updater.nickname";
                default -> "document." + lowerFirst(fieldKey);
            };
        }
        if (module == ErpPrintModuleEnum.STOCK_OUT) {
            if (fieldKey.startsWith("col_") || fieldKey.startsWith("query_")) {
                return null;
            }
            return switch (fieldKey) {
                case "customerId" -> "customer.name";
                case "deptId" -> "dept.name";
                case "creator", "creatorName" -> "creator.nickname";
                case "updater", "updaterName" -> "updater.nickname";
                default -> "document." + lowerFirst(fieldKey);
            };
        }
        if (module == ErpPrintModuleEnum.STOCK_IN) {
            if (fieldKey.startsWith("col_") || fieldKey.startsWith("query_")) {
                return null;
            }
            return switch (fieldKey) {
                case "supplierId" -> "supplier.name";
                case "deptId" -> "dept.name";
                case "creator", "creatorName" -> "creator.nickname";
                case "updater", "updaterName" -> "updater.nickname";
                case "warehouseNames" -> "document.warehouseNames";
                case "itemCount" -> "document.itemCount";
                default -> "document." + lowerFirst(fieldKey);
            };
        }
        if (module == ErpPrintModuleEnum.STOCK_CHECK) {
            if (fieldKey.startsWith("col_") || fieldKey.startsWith("query_")) {
                return null;
            }
            return switch (fieldKey) {
                case "checkType" -> "document.checkTypeName";
                case "status" -> "document.statusName";
                case "deptId" -> "dept.name";
                case "creator", "creatorName" -> "creator.nickname";
                case "updater", "updaterName" -> "updater.nickname";
                default -> "document." + lowerFirst(fieldKey);
            };
        }
        if (module == ErpPrintModuleEnum.PURCHASE_ORDER) {
            return switch (fieldKey) {
                case "no" -> "order.no";
                case "supplierId" -> "supplier.name";
                case "purchaser" -> "purchaser.nickname";
                case "deptId" -> "dept.name";
                default -> "order." + lowerFirst(fieldKey);
            };
        }
        if (module == ErpPrintModuleEnum.SALE_OUT) {
            return switch (fieldKey) {
                case "status" -> "document.statusName";
                case "settleStatus" -> "document.settleStatusName";
                case "customerId" -> "customer.name";
                case "deptId" -> "dept.name";
                case "accountId" -> "account.name";
                case "saleUserId", "saleUserName" -> "saleUser.nickname";
                case "auditorId", "auditorName" -> "auditor.nickname";
                case "creator", "creatorName", "billerName" -> "creator.nickname";
                case "updater", "updaterName" -> "updater.nickname";
                case "totalAmount" -> "document.totalAmount";
                case "totalAmountUpper" -> "document.totalAmountUpper";
                case "totalPriceUpper" -> "document.totalPriceUpper";
                case "checkerName" -> "document.checkerName";
                default -> "document." + lowerFirst(fieldKey);
            };
        }
        if (module == ErpPrintModuleEnum.ACCOUNTING_VOUCHER) {
            return switch (fieldKey) {
                case "period" -> "document.period";
                case "bookkeeperUserId" -> "document.bookkeeper";
                case "makerUserId" -> "document.makerUserName";
                case "auditorUserId" -> "document.auditorUserName";
                default -> "document." + lowerFirst(fieldKey);
            };
        }
        return switch (fieldKey) {
            case "supplierId" -> "supplier.name";
            case "customerId" -> "customer.name";
            case "deptId" -> "dept.name";
            case "accountId" -> "account.name";
            case "outAccountId" -> "outAccount.name";
            case "inAccountId" -> "inAccount.name";
            case "handlerId" -> "handler.nickname";
            case "handler" -> "handler.nickname";
            case "adjuster" -> "adjuster.nickname";
            case "adjustUserId" -> "adjustUser.nickname";
            case "saleUserId" -> "saleUser.nickname";
            case "developerUserId" -> "developer.nickname";
            case "auditorId" -> "auditor.nickname";
            case "firstAuditUserId" -> "firstAuditor.nickname";
            case "finalAuditUserId" -> "finalAuditor.nickname";
            case "financeUserId" -> "financeUser.nickname";
            case "creator" -> "creator.nickname";
            case "updater" -> "updater.nickname";
            default -> "document." + lowerFirst(fieldKey);
        };
    }

    private ErpPrintTemplateDO validateTemplateExists(Long id) {
        ErpPrintTemplateDO template = printTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(PRINT_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private ErpPrintModuleEnum validateModule(String moduleKey) {
        ErpPrintModuleEnum module = ErpPrintModuleEnum.fromKey(moduleKey);
        if (module == null) {
            throw exception(PRINT_MODULE_NOT_SUPPORTED);
        }
        return module;
    }

    private ErpPrintModuleEnum validateTemplateModuleKey(String moduleKey) {
        if (!StringUtils.hasText(moduleKey)) {
            throw exception(PRINT_MODULE_NOT_SUPPORTED);
        }
        return validateModule(moduleKey);
    }

    private void assertReadPermission(ErpPrintModuleEnum module) {
        if (!securityFrameworkService.hasAnyPermissions(module.getPrintPermission(),
                module.getPrintTemplatePermission())) {
            throw exception(FORBIDDEN);
        }
    }

    private void assertPrintPermission(ErpPrintModuleEnum module) {
        if (!securityFrameworkService.hasPermission(module.getPrintPermission())) {
            throw exception(FORBIDDEN);
        }
    }

    private void assertTemplatePermission(ErpPrintModuleEnum module) {
        if (!securityFrameworkService.hasPermission(module.getPrintTemplatePermission())) {
            throw exception(FORBIDDEN);
        }
    }

    private Map<String, Object> toRawMap(Object bean) {
        return bean == null ? new LinkedHashMap<>() : BeanUtil.beanToMap(bean, false, true);
    }

    private Map<String, Object> toDisplayMap(Object bean) {
        Map<String, Object> raw = toRawMap(bean);
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, value) -> result.put(key, formatPrintValue(value)));
        return result;
    }

    private Object formatPrintValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime time) {
            return formatDateTime(time);
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        if (value instanceof BigDecimal decimal) {
            return formatDecimal(decimal);
        }
        if (value instanceof Boolean bool) {
            return Boolean.TRUE.equals(bool) ? "是" : "否";
        }
        return value;
    }

    private String formatDateTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.toLocalDate().toString();
    }

    private String formatDecimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String formatAmountUpper(Object value) {
        if (value == null || "".equals(value)) {
            return "";
        }
        try {
            return MoneyUtils.formatAmountUpper(new BigDecimal(String.valueOf(value)));
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    private void addAmountUpper(Map<String, Object> target, String key, Object value) {
        String upper = formatAmountUpper(value);
        if (StringUtils.hasText(upper)) {
            target.put(key, upper);
        }
    }

    private void addLong(Set<Long> target, Object value) {
        Long parsed = parseLong(value);
        if (parsed != null) {
            target.add(parsed);
        }
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String calculatePieceCount(Object countValue, Object packageQtyValue) {
        BigDecimal count = parseDecimal(countValue);
        BigDecimal packageQty = parseDecimal(packageQtyValue);
        if (count == null || packageQty == null || packageQty.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        return formatDecimal(count.divide(packageQty, 3, RoundingMode.HALF_UP));
    }

    private BigDecimal parseDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private Object getPieceCountValue(ErpPrintModuleEnum module, Map<String, Object> itemMap) {
        if (module == ErpPrintModuleEnum.SALE_PRICE_ADJUST) {
            return itemMap.get("outCount");
        }
        return itemMap.get("count");
    }

    private boolean isPieceCountPrintModule(ErpPrintModuleEnum module) {
        return isPurchasePrintModule(module) || isSalePrintModule(module) || isStockPrintModule(module);
    }

    private boolean isPurchasePrintModule(ErpPrintModuleEnum module) {
        return module == ErpPrintModuleEnum.PURCHASE_ORDER
                || module == ErpPrintModuleEnum.PURCHASE_IN
                || module == ErpPrintModuleEnum.PURCHASE_RETURN
                || module == ErpPrintModuleEnum.PURCHASE_INVOICE
                || module == ErpPrintModuleEnum.PURCHASE_PRICE_ADJUST;
    }

    private boolean isSalePrintModule(ErpPrintModuleEnum module) {
        return module == ErpPrintModuleEnum.SALE_ORDER
                || module == ErpPrintModuleEnum.SALE_OUT
                || module == ErpPrintModuleEnum.SALE_RETURN
                || module == ErpPrintModuleEnum.SALE_QUOTE
                || module == ErpPrintModuleEnum.SALE_PRICE_ADJUST
                || module == ErpPrintModuleEnum.SALE_CART;
    }

    private boolean isStockPrintModule(ErpPrintModuleEnum module) {
        return module == ErpPrintModuleEnum.STOCK_IN
                || module == ErpPrintModuleEnum.STOCK_OUT
                || module == ErpPrintModuleEnum.STOCK_CHECK
                || module == ErpPrintModuleEnum.STOCK_TRANSFER_OUT
                || module == ErpPrintModuleEnum.WAREHOUSE_MOVE;
    }

    private AdminUserRespDTO firstUser(RelatedMaps relatedMaps, Object... values) {
        for (Object value : values) {
            Long id = parseLong(value);
            if (id != null && relatedMaps.users.containsKey(id)) {
                return relatedMaps.users.get(id);
            }
        }
        return null;
    }

    private Object firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value instanceof String text) {
                if (StringUtils.hasText(text)) {
                    return value;
                }
                continue;
            }
            if (value != null && !"".equals(value)) {
                return value;
            }
        }
        return null;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null && !"".equals(value)) {
            target.put(key, value);
        }
    }

    private Map<String, Object> simpleNameMap(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof ErpSupplierDO supplier) {
            result.put("id", supplier.getId());
            result.put("name", supplier.getName());
        } else if (value instanceof ErpCustomerDO customer) {
            result.put("id", customer.getId());
            result.put("name", customer.getName());
        } else if (value instanceof DeptRespDTO dept) {
            result.put("id", dept.getId());
            result.put("name", dept.getName());
        } else if (value instanceof ErpAccountDO account) {
            result.put("id", account.getId());
            result.put("name", account.getName());
        } else if (value instanceof ErpWarehouseDO warehouse) {
            result.put("id", warehouse.getId());
            result.put("name", warehouse.getName());
        }
        return result;
    }

    private Map<String, Object> simpleUserMap(AdminUserRespDTO user) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (user != null) {
            result.put("id", user.getId());
            result.put("nickname", user.getNickname());
        }
        return result;
    }

    private String lowerFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    private String formatStockCheckType(Integer checkType) {
        if (Integer.valueOf(2).equals(checkType)) {
            return "盘成本";
        }
        return "盘数量";
    }

    private String formatAuditStatus(Integer status) {
        if (Integer.valueOf(20).equals(status)) {
            return "已审核";
        }
        if (Integer.valueOf(0).equals(status)) {
            return "草稿";
        }
        return "待审核";
    }

    private String formatSaleOutAuditStatus(Integer status) {
        if (Objects.equals(ErpAuditStatus.APPROVE.getStatus(), status)) {
            return ErpAuditStatus.APPROVE.getName();
        }
        if (Objects.equals(ErpAuditStatus.PROCESS.getStatus(), status)) {
            return ErpAuditStatus.PROCESS.getName();
        }
        return "";
    }

    private String formatSaleOutSettleStatus(Integer settleStatus) {
        if (Integer.valueOf(2).equals(settleStatus)) {
            return "已结算";
        }
        if (Integer.valueOf(1).equals(settleStatus)) {
            return "部分结算";
        }
        return "未结算";
    }

    private SaleOutSourceDocumentMeta getSaleOutSourceDocumentMeta(ErpSaleOutDO saleOut) {
        if (saleOut.getSourceType() == null || saleOut.getSourceId() == null) {
            return null;
        }
        Long sourceId = saleOut.getSourceId();
        if (Objects.equals(ErpSaleBizSourceTypeEnum.LEGACY_ORDER.getType(), saleOut.getSourceType())) {
            ErpSaleOrderDO order = DataPermissionUtils.executeIgnore(() -> saleOrderService.getSaleOrder(sourceId));
            return order == null ? null : new SaleOutSourceDocumentMeta(order.getCreator(), order.getCreateTime(), null);
        }
        if (Objects.equals(ErpSaleBizSourceTypeEnum.QUOTE.getType(), saleOut.getSourceType())) {
            ErpSaleQuoteDO quote = DataPermissionUtils.executeIgnore(() -> saleQuoteService.getSaleQuote(sourceId));
            return quote == null ? null
                    : new SaleOutSourceDocumentMeta(quote.getCreator(), quote.getCreateTime(), quote.getFreightType());
        }
        if (Objects.equals(ErpSaleBizSourceTypeEnum.CART.getType(), saleOut.getSourceType())) {
            ErpSaleCartDO cart = DataPermissionUtils.executeIgnore(() -> saleCartService.getSaleCart(sourceId));
            return cart == null ? null
                    : new SaleOutSourceDocumentMeta(cart.getCreator(), cart.getCreateTime(), cart.getFreightType());
        }
        if (Objects.equals(ErpSaleBizSourceTypeEnum.PRICE_ADJUST.getType(), saleOut.getSourceType())) {
            ErpSalePriceAdjustDO adjust = DataPermissionUtils.executeIgnore(
                    () -> salePriceAdjustService.getSalePriceAdjust(sourceId));
            return adjust == null ? null
                    : new SaleOutSourceDocumentMeta(adjust.getCreator(), adjust.getCreateTime(), null);
        }
        if (Objects.equals(ErpSaleBizSourceTypeEnum.PURCHASE_IN.getType(), saleOut.getSourceType())) {
            ErpPurchaseInDO purchaseIn = DataPermissionUtils.executeIgnore(() -> purchaseInService.getPurchaseIn(sourceId));
            return purchaseIn == null ? null
                    : new SaleOutSourceDocumentMeta(purchaseIn.getCreator(), purchaseIn.getCreateTime(), null);
        }
        return null;
    }

    private ErpPrintFieldRespVO.Field field(String name, String code, String source) {
        return new ErpPrintFieldRespVO.Field(name, code, source);
    }

    private record SaleOutSourceDocumentMeta(String creator, LocalDateTime createTime, String freightType) {
    }

    private static final class RelatedMaps {

        private final Map<Long, ErpSupplierDO> suppliers;
        private final Map<Long, ErpCustomerDO> customers;
        private final Map<Long, ErpAccountDO> accounts;
        private final Map<Long, DeptRespDTO> depts;
        private final Map<Long, ErpProductRespVO> products;
        private final Map<Long, ErpWarehouseDO> warehouses;
        private final Map<Long, AdminUserRespDTO> users;

        private RelatedMaps(Map<Long, ErpSupplierDO> suppliers,
                            Map<Long, ErpCustomerDO> customers,
                            Map<Long, ErpAccountDO> accounts,
                            Map<Long, DeptRespDTO> depts,
                            Map<Long, ErpProductRespVO> products,
                            Map<Long, ErpWarehouseDO> warehouses,
                            Map<Long, AdminUserRespDTO> users) {
            this.suppliers = suppliers;
            this.customers = customers;
            this.accounts = accounts;
            this.depts = depts;
            this.products = products;
            this.warehouses = warehouses;
            this.users = users;
        }

        private ErpSupplierDO supplier(Object id) {
            return suppliers.get(parse(id, Long::parseLong));
        }

        private ErpAccountDO account(Object id) {
            return accounts.get(parse(id, Long::parseLong));
        }

        private ErpCustomerDO customer(Object id) {
            return customers.get(parse(id, Long::parseLong));
        }

        private DeptRespDTO dept(Object id) {
            return depts.get(parse(id, Long::parseLong));
        }

        private ErpProductRespVO product(Object id) {
            return products.get(parse(id, Long::parseLong));
        }

        private ErpWarehouseDO warehouse(Object id) {
            return warehouses.get(parse(id, Long::parseLong));
        }

        private static Long parse(Object value, Function<String, Long> parser) {
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && StringUtils.hasText(text)) {
                try {
                    return parser.apply(text);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }
    }

}
