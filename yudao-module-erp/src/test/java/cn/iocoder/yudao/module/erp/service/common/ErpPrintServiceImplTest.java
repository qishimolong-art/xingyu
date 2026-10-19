package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliveryItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintTemplateDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintTemplateMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinancePaymentService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceReceiptService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleQuoteService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockCheckService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRINT_TEMPLATE_DEFAULT_DELETE_FORBIDDEN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRINT_ITEM_SORT_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPrintServiceImplTest extends BaseMockitoUnitTest {

    @Mock private cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService voucherService;

    @InjectMocks
    private ErpPrintServiceImpl printService;

    @Mock
    private SecurityFrameworkService securityFrameworkService;
    @Mock
    private ErpPayableExpenseService payableExpenseService;
    @Mock
    private ErpFinancePaymentService financePaymentService;
    @Mock
    private ErpFinanceReceiptService financeReceiptService;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpSalePickDeliveryService salePickDeliveryService;
    @Mock
    private ErpSaleQuoteService saleQuoteService;
    @Mock
    private ErpSalePriceAdjustService salePriceAdjustService;
    @Mock
    private ErpSaleCartService saleCartService;
    @Mock
    private ErpSaleReturnService saleReturnService;
    @Mock
    private ErpStockInService stockInService;
    @Mock
    private ErpStockMoveService stockMoveService;
    @Mock
    private ErpStockOutBillService stockOutBillService;
    @Mock
    private ErpStockCheckService stockCheckService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpWarehouseMoveService warehouseMoveService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpPurchaseOrderService purchaseOrderService;
    @Mock
    private ErpPurchaseInService purchaseInService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpReceivableAccountMapper receivableAccountMapper;
    @Mock
    private ErpPayableAccountMapper payableAccountMapper;
    @Mock
    private ErpPrintTemplateMapper printTemplateMapper;

    @Test
    @SuppressWarnings("unchecked")
    void saleCartPrintUsesRequestedWholeDocumentOrderAndRenumbersRows() {
        Long businessId = 100L;
        List<ErpSaleCartItemDO> sortedItems = List.of(
                new ErpSaleCartItemDO().setId(20L).setCount(new BigDecimal("8")),
                new ErpSaleCartItemDO().setId(10L).setCount(new BigDecimal("2")));
        when(securityFrameworkService.hasPermission("erp:sale-cart:print")).thenReturn(true);
        when(saleCartService.getSaleCart(businessId)).thenReturn(new ErpSaleCartDO().setId(businessId));
        when(saleCartService.getSaleCartItemListByCartId(businessId, "count", "desc"))
                .thenReturn(sortedItems);

        Map<String, Object> result = printService.getPrintData(
                "sale_cart", businessId, "count", "DESC");

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("items");
        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).get("items.seq"));
        assertEquals(20L, rows.get(0).get("items.id"));
        assertEquals(2, rows.get(1).get("items.seq"));
        assertEquals(10L, rows.get(1).get("items.id"));
        verify(saleCartService).getSaleCartItemListByCartId(businessId, "count", "desc");
        verify(saleCartService, never()).getSaleCartItemListByCartId(businessId);
    }

    @Test
    void saleCartPrintWithoutSortKeepsLegacyRequestPath() {
        Long businessId = 101L;
        when(securityFrameworkService.hasPermission("erp:sale-cart:print")).thenReturn(true);
        when(saleCartService.getSaleCart(businessId)).thenReturn(new ErpSaleCartDO().setId(businessId));
        when(saleCartService.getSaleCartItemListByCartId(businessId))
                .thenReturn(List.of(new ErpSaleCartItemDO().setId(1L)));

        printService.getPrintData("sale_cart", businessId);

        verify(saleCartService).getSaleCartItemListByCartId(businessId);
        verify(saleCartService, never()).getSaleCartItemListByCartId(any(), any(), any());
    }

    @Test
    void printRejectsIncompleteUnsafeAndUnsupportedSortParameters() {
        when(securityFrameworkService.hasPermission("erp:sale-cart:print")).thenReturn(true);
        when(securityFrameworkService.hasPermission("erp:purchase-order:print")).thenReturn(true);

        assertServiceException(() -> printService.getPrintData(
                "sale_cart", 100L, "count", null), PRINT_ITEM_SORT_INVALID);
        assertServiceException(() -> printService.getPrintData(
                "sale_cart", 100L, "count desc; delete from erp_sale_cart_items", "asc"),
                PRINT_ITEM_SORT_INVALID);
        assertServiceException(() -> printService.getPrintData(
                "sale_cart", 100L, "count", "sideways"), PRINT_ITEM_SORT_INVALID);
        assertServiceException(() -> printService.getPrintData(
                "purchase_order", 100L, "notARealField", "asc"), PRINT_ITEM_SORT_INVALID);

        verify(saleCartService, never()).getSaleCartItemListByCartId(any());
        verify(saleCartService, never()).getSaleCartItemListByCartId(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void purchaseOrderPrintUsesRequestedWholeDocumentOrderAndRenumbersRows() {
        Long businessId = 102L;
        List<ErpPurchaseOrderItemDO> sortedItems = List.of(
                new ErpPurchaseOrderItemDO().setId(20L).setCount(new BigDecimal("8")),
                new ErpPurchaseOrderItemDO().setId(10L).setCount(new BigDecimal("2")));
        when(securityFrameworkService.hasPermission("erp:purchase-order:print")).thenReturn(true);
        when(purchaseOrderService.getPurchaseOrder(businessId))
                .thenReturn(new ErpPurchaseOrderDO().setId(businessId));
        when(purchaseOrderService.getPurchaseOrderItemListByOrderId(businessId, "count", "desc"))
                .thenReturn(sortedItems);

        Map<String, Object> result = printService.getPrintData(
                "purchase_order", businessId, "count", "DESC");

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("items");
        assertEquals(20L, rows.get(0).get("items.id"));
        assertEquals(1, rows.get(0).get("items.seq"));
        assertEquals(10L, rows.get(1).get("items.id"));
        assertEquals(2, rows.get(1).get("items.seq"));
        verify(purchaseOrderService)
                .getPurchaseOrderItemListByOrderId(businessId, "count", "desc");
        verify(purchaseOrderService, never()).getPurchaseOrderItemListByOrderId(businessId);
    }

    @Test
    void deleteTemplateDeletesNonDefaultTemplateWithBothPermissions() {
        Long templateId = 17L;
        when(printTemplateMapper.selectById(templateId)).thenReturn(new ErpPrintTemplateDO()
                .setId(templateId).setModuleKey("sale_out").setDefaulted(false));
        when(securityFrameworkService.hasPermission("erp:sale-out:print-template")).thenReturn(true);
        when(securityFrameworkService.hasPermission("erp:print-template:delete")).thenReturn(true);
        when(printTemplateMapper.deleteById(templateId)).thenReturn(1);

        printService.deleteTemplate(templateId);

        verify(printTemplateMapper).deleteById(templateId);
    }

    @Test
    void deleteTemplateRejectsDefaultTemplate() {
        Long templateId = 18L;
        when(printTemplateMapper.selectById(templateId)).thenReturn(new ErpPrintTemplateDO()
                .setId(templateId).setModuleKey("sale_out").setDefaulted(true));
        when(securityFrameworkService.hasPermission("erp:sale-out:print-template")).thenReturn(true);
        when(securityFrameworkService.hasPermission("erp:print-template:delete")).thenReturn(true);

        assertServiceException(() -> printService.deleteTemplate(templateId),
                PRINT_TEMPLATE_DEFAULT_DELETE_FORBIDDEN);
        verify(printTemplateMapper, never()).deleteById(templateId);
    }

    @Test
    void deleteTemplateRequiresIndependentDeletePermission() {
        Long templateId = 19L;
        when(printTemplateMapper.selectById(templateId)).thenReturn(new ErpPrintTemplateDO()
                .setId(templateId).setModuleKey("sale_out").setDefaulted(false));
        when(securityFrameworkService.hasPermission("erp:sale-out:print-template")).thenReturn(true);
        when(securityFrameworkService.hasPermission("erp:print-template:delete")).thenReturn(false);

        assertServiceException(() -> printService.deleteTemplate(templateId), FORBIDDEN);
        verify(printTemplateMapper, never()).deleteById(templateId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void voucherPrintKeepsAllAuxiliaryNamesAndLegacyNameOnlyRows() {
        var voucher=new cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO().setId(77L).setVoucherNo("记-77");
        var first=new cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO().setAuxiliaryName("供应商甲 / 销售部 / 项目乙");
        first.setAuxiliaries(List.of(
            cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherAuxiliarySupport.value("supplier",1L,"供应商甲"),
            cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherAuxiliarySupport.value("dept",2L,"销售部"),
            cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherAuxiliarySupport.value("project",3L,"项目乙")));
        var legacy=new cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO().setAuxiliaryName("历史名称（无ID）");
        when(securityFrameworkService.hasPermission("erp:voucher:print")).thenReturn(true);
        when(voucherService.getVoucher(77L)).thenReturn(voucher);
        when(voucherService.getVoucherItemListByVoucherId(77L)).thenReturn(List.of(first,legacy));
        Map<String,Object> result=printService.getPrintData("accounting_voucher",77L);
        List<Map<String,Object>> items=(List<Map<String,Object>>)result.get("items");
        assertEquals("供应商甲 / 销售部 / 项目乙",items.get(0).get("items.auxiliaryName"));
        assertEquals("历史名称（无ID）",items.get(1).get("items.auxiliaryName"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_payableExpense() {
        Long businessId = 1001L;
        ErpPayableExpenseDO expense = new ErpPayableExpenseDO()
                .setId(businessId)
                .setNo("FY-001")
                .setBizTime(LocalDate.of(2026, 8, 17))
                .setExpenseBizType("一般费用")
                .setTotalAmount(new BigDecimal("120.50"));
        ErpPayableExpenseItemDO item = new ErpPayableExpenseItemDO()
                .setId(2001L)
                .setExpenseId(businessId)
                .setItemName("办公用品")
                .setAmount(new BigDecimal("120.50"));
        when(securityFrameworkService.hasPermission("erp:payable-expense:print")).thenReturn(true);
        when(payableExpenseService.getPayableExpense(businessId)).thenReturn(expense);
        when(payableExpenseService.getPayableExpenseItemListByExpenseId(businessId)).thenReturn(List.of(item));

        Map<String, Object> result = printService.getPrintData("payable_expense", businessId);

        assertEquals("payable_expense", result.get("moduleKey"));
        assertEquals("费用支付", result.get("moduleName"));
        Map<String, Object> document = (Map<String, Object>) result.get("document");
        assertEquals("FY-001", document.get("no"));
        assertEquals("2026-08-17", document.get("bizTime"));
        assertEquals("一般费用", document.get("expenseBizType"));
        assertEquals("120.5", document.get("totalAmount"));
        Map<String, Object> main = (Map<String, Object>) result.get("main");
        assertEquals("壹佰贰拾元伍角", main.get("document.totalAmountUpper"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(1, items.size());
        assertEquals(1, items.get(0).get("items.seq"));
        assertEquals("办公用品", items.get(0).get("items.itemName"));
        assertEquals("120.5", items.get(0).get("items.amount"));
        verify(payableExpenseService).getPayableExpense(businessId);
        verify(payableExpenseService).getPayableExpenseItemListByExpenseId(businessId);
    }

    @Test
    void testGetFields_saleOutReadableStatusCodes() {
        when(securityFrameworkService.hasPermission("erp:sale-out:print-template")).thenReturn(true);
        when(permissionApi.getFieldDefinitions("erp_sale_out")).thenReturn(List.of(
                definition("no", "出库单号", 10),
                definition("status", "状态", 34),
                definition("settleStatus", "结算状态", 35),
                definition("customerCode", "客户编码", 36),
                definition("logisticsCompany", "物流公司", 37),
                definition("customerAddress", "客户地址", 38),
                definition("customerPhone", "客户电话", 39),
                definition("previousReceivable", "此前应收", 40),
                definition("currentDebt", "本次欠款", 41),
                definition("totalDebt", "总欠款金额", 42),
                definition("totalAmount", "合计总金额", 43),
                definition("totalAmountUpper", "大写金额", 44),
                definition("item_seq", "序号", 45),
                definition("billerName", "开单员", 46),
                definition("pickerName", "拣货人", 47),
                definition("checkerName", "验货人", 48),
                definition("senderName", "发货人", 49),
                definition("creatorName", "制单人", 900)));

        ErpPrintFieldRespVO result = printService.getFields("sale_out");

        assertEquals("sale_out", result.getModuleKey());
        assertEquals("document.no", fieldCode(result, "销售单号"));
        assertEquals("document.statusName", fieldCode(result, "状态"));
        assertEquals("document.settleStatusName", fieldCode(result, "结算状态"));
        assertEquals("document.customerCode", fieldCode(result, "客户编码"));
        assertEquals("document.logisticsCompany", fieldCode(result, "物流公司"));
        assertEquals("document.customerAddress", fieldCode(result, "客户地址"));
        assertEquals("document.customerPhone", fieldCode(result, "客户电话"));
        assertEquals("document.previousReceivable", fieldCode(result, "此前应收"));
        assertEquals("document.currentDebt", fieldCode(result, "本次欠款"));
        assertEquals("document.totalDebt", fieldCode(result, "总欠款金额"));
        assertEquals("document.totalAmount", fieldCode(result, "合计总金额"));
        assertEquals("document.totalAmountUpper", fieldCode(result, "大写金额"));
        assertEquals("items.seq", fieldCode(result, "序号"));
        assertEquals("creator.nickname", fieldCode(result, "开单员"));
        assertEquals("document.pickerName", fieldCode(result, "拣货人"));
        assertEquals("document.senderName", fieldCode(result, "发货人"));
        assertEquals(1, result.getGroups().stream().flatMap(group -> group.getFields().stream())
                .filter(field -> "发货人".equals(field.getName())).count());
        assertEquals("creator.nickname", fieldCode(result, "制单人"));
        assertEquals("print.pageNo", fieldCode(result, "当前页"));
        assertEquals("print.pageCount", fieldCode(result, "总页数"));
        assertEquals("print.pageText", fieldCode(result, "页码"));
    }

    @Test
    void testGetFields_saleReturnAddsCustomerContactFields() {
        when(securityFrameworkService.hasPermission("erp:sale-return:print-template")).thenReturn(true);
        when(permissionApi.getFieldDefinitions("erp_sale_return")).thenReturn(List.of());

        ErpPrintFieldRespVO result = printService.getFields("sale_return");

        assertEquals("sale_return", result.getModuleKey());
        assertEquals("document.customerAddress", fieldCode(result, "客户地址"));
        assertEquals("document.customerPhone", fieldCode(result, "客户电话"));
        assertEquals("print.pageText", fieldCode(result, "页码"));
    }

    @Test
    void testGetFields_financeReceiptAddsTemplateSummaryFields() {
        when(securityFrameworkService.hasPermission("erp:finance-receipt:print-template")).thenReturn(true);
        when(permissionApi.getFieldDefinitions("erp_finance_receipt")).thenReturn(List.of());

        ErpPrintFieldRespVO result = printService.getFields("finance_receipt");

        assertEquals("finance_receipt", result.getModuleKey());
        assertEquals("document.settleMethodName", fieldCode(result, "结算方式"));
        assertEquals("document.previousReceivable", fieldCode(result, "此前应收"));
        assertEquals("document.remainingReceivable", fieldCode(result, "余欠"));
    }

    @Test
    void testGetFields_financePaymentAddsTemplateSummaryFields() {
        when(securityFrameworkService.hasPermission("erp:finance-payment:print-template")).thenReturn(true);
        when(permissionApi.getFieldDefinitions("erp_finance_payment")).thenReturn(List.of());

        ErpPrintFieldRespVO result = printService.getFields("finance_payment");

        assertEquals("finance_payment", result.getModuleKey());
        assertEquals("document.settleMethodName", fieldCode(result, "结算方式"));
        assertEquals("document.previousPayable", fieldCode(result, "此前应付"));
        assertEquals("document.remainingPayable", fieldCode(result, "余欠"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_financePaymentAddsSettlementAndBalanceFields() {
        ReflectionTestUtils.setField(printService, "deptApi", deptApi);
        Long businessId = 1005L;
        Long supplierId = 2005L;
        Long deptId = 3005L;
        ErpFinancePaymentDO payment = new ErpFinancePaymentDO()
                .setId(businessId)
                .setNo("FKD20261016000001")
                .setSupplierId(supplierId)
                .setDeptId(deptId)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setTotalPrice(new BigDecimal("100.00"))
                .setDiscountPrice(new BigDecimal("20.00"))
                .setPaymentPrice(new BigDecimal("80.00"));
        ErpSupplierDO supplier = new ErpSupplierDO()
                .setId(supplierId)
                .setName("测试供应商")
                .setSettleMethod("月结");
        when(securityFrameworkService.hasPermission("erp:finance-payment:print")).thenReturn(true);
        when(financePaymentService.getFinancePayment(businessId)).thenReturn(payment);
        when(financePaymentService.getFinancePaymentItemListByPaymentId(businessId)).thenReturn(List.of());
        when(supplierService.getSupplierMap(Set.of(supplierId))).thenReturn(Map.of(supplierId, supplier));
        when(supplierService.getSupplier(supplierId)).thenReturn(supplier);
        when(payableAccountMapper.selectBySupplierIdAndDeptId(supplierId, deptId)).thenReturn(
                new ErpPayableAccountDO().setBalance(new BigDecimal("5000.00")));

        Map<String, Object> result = printService.getPrintData("finance_payment", businessId);

        Map<String, Object> main = (Map<String, Object>) result.get("main");
        assertEquals("月结", main.get("document.settleMethodName"));
        assertEquals("5100", main.get("document.previousPayable"));
        assertEquals("5000", main.get("document.remainingPayable"));
        assertEquals("测试供应商", main.get("supplier.name"));
        verify(financePaymentService).getFinancePayment(businessId);
        verify(payableAccountMapper).selectBySupplierIdAndDeptId(supplierId, deptId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_financeReceiptAddsSettlementAndBalanceFields() {
        ReflectionTestUtils.setField(printService, "deptApi", deptApi);
        Long businessId = 1004L;
        Long customerId = 2004L;
        Long deptId = 3004L;
        ErpFinanceReceiptDO receipt = new ErpFinanceReceiptDO()
                .setId(businessId)
                .setNo("SKD20261015000001")
                .setCustomerId(customerId)
                .setDeptId(deptId)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setTotalPrice(new BigDecimal("78.00"))
                .setDiscountPrice(BigDecimal.ZERO)
                .setReceiptPrice(new BigDecimal("78.00"));
        ErpCustomerDO customer = new ErpCustomerDO()
                .setId(customerId)
                .setName("雅安分公司（汽配）")
                .setSettleMethod(2);
        when(securityFrameworkService.hasPermission("erp:finance-receipt:print")).thenReturn(true);
        when(financeReceiptService.getFinanceReceipt(businessId)).thenReturn(receipt);
        when(financeReceiptService.getFinanceReceiptItemListByReceiptId(businessId)).thenReturn(List.of());
        when(customerService.getCustomerMap(Set.of(customerId))).thenReturn(Map.of(customerId, customer));
        when(customerService.getCustomer(customerId)).thenReturn(customer);
        when(receivableAccountMapper.selectByCustomerIdAndDeptId(customerId, deptId)).thenReturn(
                new ErpReceivableAccountDO().setReceivableBalance(new BigDecimal("261341.46")));

        Map<String, Object> result = printService.getPrintData("finance_receipt", businessId);

        Map<String, Object> main = (Map<String, Object>) result.get("main");
        assertEquals("挂账", main.get("document.settleMethodName"));
        assertEquals("261419.46", main.get("document.previousReceivable"));
        assertEquals("261341.46", main.get("document.remainingReceivable"));
        assertEquals("雅安分公司（汽配）", main.get("customer.name"));
        verify(financeReceiptService).getFinanceReceipt(businessId);
        verify(receivableAccountMapper).selectByCustomerIdAndDeptId(customerId, deptId);
    }

    @Test
    void testGetFields_stockTransferOutDirectCustomer() {
        when(securityFrameworkService.hasPermission("erp:stock-transfer-out:print-template")).thenReturn(true);
        when(permissionApi.getFieldDefinitions("erp_stock_transfer_out")).thenReturn(List.of(
                definition("directCustomerName", "直发客户", 65)));

        ErpPrintFieldRespVO result = printService.getFields("stock_transfer_out");

        assertEquals("stock_transfer_out", result.getModuleKey());
        assertEquals("document.directCustomerName", fieldCode(result, "直发客户"));
        assertEquals("document.moveDateTime", fieldCode(result, "调拨日期时间"));
        assertEquals("document.totalPieceCount", fieldCode(result, "调出件数"));
        assertEquals("document.totalPriceFormatted", fieldCode(result, "合计金额（格式化）"));
        assertEquals("document.printDateTime", fieldCode(result, "列印时间"));
        assertEquals("items.seq", fieldCode(result, "项次"));
        assertEquals("items.vehicleModel", fieldCode(result, "适用车型"));
        assertEquals("items.originPlace", fieldCode(result, "产地"));
        assertEquals("items.fromWarehouseName", fieldCode(result, "调出仓库"));
        assertEquals("items.fromShelf", fieldCode(result, "仓位"));
        assertEquals("items.pieceCount", fieldCode(result, "件数"));
    }

    @Test
    void testGetFields_purchaseAddsPieceCountDetailField() {
        when(securityFrameworkService.hasPermission("erp:purchase-invoice:print-template")).thenReturn(true);
        when(permissionApi.getFieldDefinitions("purchase_invoice")).thenReturn(List.of());

        ErpPrintFieldRespVO result = printService.getFields("purchase_invoice");

        assertEquals("purchase_invoice", result.getModuleKey());
        assertEquals("items.pieceCount", fieldCode(result, "件数"));
    }

    @Test
    void testGetFields_saleAddsPieceCountDetailField() {
        when(securityFrameworkService.hasPermission("erp:sale-quote:print-template")).thenReturn(true);
        when(permissionApi.getFieldDefinitions("erp_sale_quote")).thenReturn(List.of());

        ErpPrintFieldRespVO result = printService.getFields("sale_quote");

        assertEquals("sale_quote", result.getModuleKey());
        assertEquals("items.pieceCount", fieldCode(result, "件数"));
    }

    @Test
    void testGetFields_stockAddsPieceCountDetailField() {
        when(securityFrameworkService.hasPermission("erp:stock-check:print-template")).thenReturn(true);
        when(permissionApi.getFieldDefinitions("erp_stock_check", "base_info")).thenReturn(List.of());
        when(permissionApi.getFieldDefinitions("erp_stock_check", "detail_item")).thenReturn(List.of());

        ErpPrintFieldRespVO result = printService.getFields("stock_check");

        assertEquals("stock_check", result.getModuleKey());
        assertEquals("document.checkDateTime", fieldCode(result, "盘点日期时间"));
        assertEquals("document.warehouseNames", fieldCode(result, "仓库"));
        assertEquals("document.balanceTypeName", fieldCode(result, "盈亏类型"));
        assertEquals("document.balanceAmount", fieldCode(result, "盈亏金额"));
        assertEquals("creator.nickname", fieldCode(result, "制单人"));
        assertEquals("updater.nickname", fieldCode(result, "审核人"));
        assertEquals("document.printDateTime", fieldCode(result, "列印时间"));
        assertEquals("items.pieceCount", fieldCode(result, "件数"));
        assertEquals("print.pageText", fieldCode(result, "页码"));
    }

    @Test
    void testGetFields_warehouseMoveAddsPrintSpecificFields() {
        when(securityFrameworkService.hasPermission("erp:warehouse-move:print-template")).thenReturn(true);
        when(permissionApi.getFieldDefinitions("erp_warehouse_move")).thenReturn(List.of());

        ErpPrintFieldRespVO result = printService.getFields("warehouse_move");

        assertEquals("warehouse_move", result.getModuleKey());
        assertEquals("document.moveDateTime", fieldCode(result, "移货日期时间"));
        assertEquals("document.printDateTime", fieldCode(result, "列印时间"));
        assertEquals("items.seq", fieldCode(result, "项次"));
        assertEquals("items.standard", fieldCode(result, "规格"));
        assertEquals("items.pieceCount", fieldCode(result, "件数"));
        assertEquals("print.pageText", fieldCode(result, "页码"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_warehouseMoveAddsFullDateTimeAndTemplateFields() {
        Long businessId = 1006L;
        Long fromWarehouseId = 3006L;
        Long toWarehouseId = 3007L;
        Long handlerId = 4006L;
        Long creatorId = 4007L;
        Long approverId = 4008L;
        Long productId = 5006L;
        ErpWarehouseMoveDO move = new ErpWarehouseMoveDO()
                .setId(businessId)
                .setNo("YH20261016000001")
                .setMoveTime(LocalDateTime.of(2026, 10, 16, 9, 43, 52))
                .setFromWarehouseId(fromWarehouseId)
                .setToWarehouseId(toWarehouseId)
                .setHandlerId(handlerId)
                .setApproveUserId(approverId)
                .setTotalCount(new BigDecimal("10"));
        move.setCreator(String.valueOf(creatorId));
        move.setUpdater(String.valueOf(approverId));
        ErpWarehouseMoveItemDO item = new ErpWarehouseMoveItemDO()
                .setMoveId(businessId)
                .setProductId(productId)
                .setFromWarehouseId(fromWarehouseId)
                .setToWarehouseId(toWarehouseId)
                .setPackageQty(4)
                .setCount(new BigDecimal("10"))
                .setRemark("测试备注");
        ErpProductRespVO product = new ErpProductRespVO()
                .setId(productId)
                .setCode("3210003785")
                .setName("路极 225/60R17 99H DHT02 BGRX")
                .setStandard("17寸")
                .setPackageQty(4);
        ErpWarehouseDO fromWarehouse = new ErpWarehouseDO().setId(fromWarehouseId).setName("大塘仓库");
        ErpWarehouseDO toWarehouse = new ErpWarehouseDO().setId(toWarehouseId).setName("大塘路农仓");
        AdminUserRespDTO handler = new AdminUserRespDTO().setId(handlerId).setNickname("罗艳");
        AdminUserRespDTO creator = new AdminUserRespDTO().setId(creatorId).setNickname("制单员");
        AdminUserRespDTO approver = new AdminUserRespDTO().setId(approverId).setNickname("审核员");

        when(securityFrameworkService.hasPermission("erp:warehouse-move:print")).thenReturn(true);
        when(warehouseMoveService.getWarehouseMove(businessId)).thenReturn(move);
        when(warehouseMoveService.getWarehouseMoveItemListByMoveId(businessId)).thenReturn(List.of(item));
        Map<Long, ErpWarehouseDO> warehouses = new java.util.LinkedHashMap<>();
        warehouses.put(fromWarehouseId, fromWarehouse);
        warehouses.put(toWarehouseId, toWarehouse);
        when(warehouseService.getWarehouseMap(Set.of(fromWarehouseId, toWarehouseId))).thenReturn(warehouses);
        when(productService.getProductVOMap(Set.of(productId))).thenReturn(Map.of(productId, product));
        when(adminUserApi.getUserMap(Set.of(handlerId, creatorId, approverId)))
                .thenReturn(Map.of(handlerId, handler, creatorId, creator, approverId, approver));

        Map<String, Object> result = printService.getPrintData("warehouse_move", businessId);

        Map<String, Object> main = (Map<String, Object>) result.get("main");
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals("2026/10/16 09:43:52", main.get("document.moveDateTime"));
        assertEquals(true, String.valueOf(main.get("document.printDateTime"))
                .matches("\\d{4}/\\d{1,2}/\\d{1,2} \\d{2}:\\d{2}:\\d{2}"));
        assertEquals("大塘仓库", main.get("fromWarehouse.name"));
        assertEquals("大塘路农仓", main.get("toWarehouse.name"));
        assertEquals("罗艳", main.get("handler.nickname"));
        assertEquals("制单员", main.get("creator.nickname"));
        assertEquals("审核员", main.get("approveUser.nickname"));
        assertEquals("3210003785", items.get(0).get("items.productCode"));
        assertEquals("路极 225/60R17 99H DHT02 BGRX", items.get(0).get("items.productName"));
        assertEquals("17寸", items.get(0).get("items.standard"));
        assertEquals("2.5", items.get(0).get("items.pieceCount"));
        assertEquals("测试备注", items.get(0).get("items.remark"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_stockCheckAddsPrintHeaderAndBalanceFields() {
        Long businessId = 1003L;
        Long firstWarehouseId = 3001L;
        Long secondWarehouseId = 3002L;
        ErpStockCheckDO stockCheck = new ErpStockCheckDO()
                .setId(businessId)
                .setNo("QCPD-001")
                .setCheckTime(LocalDateTime.of(2026, 10, 16, 11, 1, 44))
                .setCheckType(1)
                .setTotalCount(new BigDecimal("-4"))
                .setTotalPrice(new BigDecimal("-2156.15"))
                .setStatus(20);
        ErpStockCheckItemDO firstItem = new ErpStockCheckItemDO()
                .setCheckId(businessId)
                .setWarehouseId(firstWarehouseId)
                .setStockCount(new BigDecimal("5"))
                .setActualCount(new BigDecimal("1"))
                .setCount(new BigDecimal("-4"));
        ErpStockCheckItemDO secondItem = new ErpStockCheckItemDO()
                .setCheckId(businessId)
                .setWarehouseId(secondWarehouseId)
                .setStockCount(new BigDecimal("4"))
                .setActualCount(new BigDecimal("4"))
                .setCount(BigDecimal.ZERO);
        when(securityFrameworkService.hasPermission("erp:stock-check:print")).thenReturn(true);
        when(stockCheckService.getStockCheck(businessId)).thenReturn(stockCheck);
        when(stockCheckService.getStockCheckItemListByCheckId(businessId)).thenReturn(List.of(firstItem, secondItem));
        Map<Long, ErpWarehouseDO> warehouses = new java.util.LinkedHashMap<>();
        warehouses.put(firstWarehouseId, new ErpWarehouseDO().setId(firstWarehouseId).setName("宜宾仓"));
        warehouses.put(secondWarehouseId, new ErpWarehouseDO().setId(secondWarehouseId).setName("备件仓"));
        when(warehouseService.getWarehouseMap(Set.of(firstWarehouseId, secondWarehouseId))).thenReturn(warehouses);

        Map<String, Object> result = printService.getPrintData("stock_check", businessId);

        Map<String, Object> main = (Map<String, Object>) result.get("main");
        assertEquals("2026/10/16 11:01:44", main.get("document.checkDateTime"));
        assertEquals("宜宾仓、备件仓", main.get("document.warehouseNames"));
        assertEquals(2, main.get("document.itemCount"));
        assertEquals("盘亏出库", main.get("document.balanceTypeName"));
        assertEquals("盘亏金额", main.get("document.balanceAmountLabel"));
        assertEquals("2,156.15", main.get("document.balanceAmount"));
        assertEquals(true, String.valueOf(main.get("document.printDateTime")).matches("\\d{4}/\\d{1,2}/\\d{1,2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_purchaseCalculatesPieceCount() {
        Long businessId = 1001L;
        ErpPurchaseOrderDO order = new ErpPurchaseOrderDO().setId(businessId).setNo("CG-001");
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setOrderId(businessId)
                .setProductId(2001L)
                .setCount(new BigDecimal("10"));
        when(securityFrameworkService.hasPermission("erp:purchase-order:print")).thenReturn(true);
        when(purchaseOrderService.getPurchaseOrder(businessId)).thenReturn(order);
        when(purchaseOrderService.getPurchaseOrderItemListByOrderId(businessId)).thenReturn(List.of(item));
        when(productService.getProductVOMap(Set.of(2001L)))
                .thenReturn(Map.of(2001L, new ErpProductRespVO().setId(2001L).setPackageQty(4)));

        Map<String, Object> result = printService.getPrintData("purchase_order", businessId);

        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(4, items.get(0).get("items.packageQty"));
        assertEquals("2.5", items.get(0).get("items.pieceCount"));
        verify(purchaseOrderService).getPurchaseOrder(businessId);
        verify(purchaseOrderService).getPurchaseOrderItemListByOrderId(businessId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_purchaseInEnrichesCustomerTemplateDetailFields() {
        Long businessId = 1002L;
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(businessId).setNo("CGRK-001");
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setInId(businessId)
                .setProductId(2002L)
                .setCount(new BigDecimal("10"))
                .setPackageQty(4)
                .setVehicleModel("入库快照车型")
                .setWarehousePosition("A-01-02");
        ErpProductRespVO product = new ErpProductRespVO()
                .setId(2002L)
                .setCode("1100835")
                .setName("钻石 DP026 舒适型")
                .setUnitName("根")
                .setStandard("155/65R14")
                .setVehicleModel("商品资料车型")
                .setOriginPlace("万力")
                .setPackageQty(6);
        when(securityFrameworkService.hasPermission("erp:purchase-in:print")).thenReturn(true);
        when(purchaseInService.getPurchaseIn(businessId)).thenReturn(purchaseIn);
        when(purchaseInService.getPurchaseInItemListByInId(businessId)).thenReturn(List.of(item));
        when(productService.getProductVOMap(Set.of(2002L))).thenReturn(Map.of(2002L, product));

        Map<String, Object> result = printService.getPrintData("purchase_in", businessId);

        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        Map<String, Object> row = items.get(0);
        assertEquals("1100835", row.get("items.productCode"));
        assertEquals("钻石 DP026 舒适型", row.get("items.productName"));
        assertEquals("根", row.get("items.productUnitName"));
        assertEquals("155/65R14", row.get("items.standard"));
        assertEquals("入库快照车型", row.get("items.vehicleModel"));
        assertEquals("万力", row.get("items.originPlace"));
        assertEquals("A-01-02", row.get("items.warehousePosition"));
        assertEquals("2.5", row.get("items.pieceCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_saleCalculatesPieceCount() {
        Long businessId = 1001L;
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO().setId(businessId).setNo("BJ-001");
        ErpSaleQuoteItemDO item = new ErpSaleQuoteItemDO()
                .setQuoteId(businessId)
                .setProductId(2001L)
                .setCount(new BigDecimal("9"))
                .setPackageQty(4);
        when(securityFrameworkService.hasPermission("erp:sale-quote:print")).thenReturn(true);
        when(saleQuoteService.getSaleQuote(businessId)).thenReturn(quote);
        when(saleQuoteService.getSaleQuoteItemListByQuoteId(businessId)).thenReturn(List.of(item));
        when(productService.getProductVOMap(Set.of(2001L)))
                .thenReturn(Map.of(2001L, new ErpProductRespVO().setId(2001L).setPackageQty(3)));

        Map<String, Object> result = printService.getPrintData("sale_quote", businessId);

        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(4, items.get(0).get("items.packageQty"));
        assertEquals("2.25", items.get(0).get("items.pieceCount"));
        verify(saleQuoteService).getSaleQuote(businessId);
        verify(saleQuoteService).getSaleQuoteItemListByQuoteId(businessId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_salePriceAdjustCalculatesPieceCountWithOutCount() {
        Long businessId = 1001L;
        ErpSalePriceAdjustDO adjust = new ErpSalePriceAdjustDO().setId(businessId).setNo("XTJ-001");
        ErpSalePriceAdjustItemDO item = new ErpSalePriceAdjustItemDO()
                .setAdjustId(businessId)
                .setProductId(2001L)
                .setOutCount(new BigDecimal("11"))
                .setPackageQty(4);
        when(securityFrameworkService.hasPermission("erp:sale-price-adjust:print")).thenReturn(true);
        when(salePriceAdjustService.getSalePriceAdjust(businessId)).thenReturn(adjust);
        when(salePriceAdjustService.getSalePriceAdjustItemListByAdjustId(businessId)).thenReturn(List.of(item));
        when(productService.getProductVOMap(Set.of(2001L)))
                .thenReturn(Map.of(2001L, new ErpProductRespVO().setId(2001L).setPackageQty(3)));

        Map<String, Object> result = printService.getPrintData("sale_price_adjust", businessId);

        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(4, items.get(0).get("items.packageQty"));
        assertEquals("2.75", items.get(0).get("items.pieceCount"));
        verify(salePriceAdjustService).getSalePriceAdjust(businessId);
        verify(salePriceAdjustService).getSalePriceAdjustItemListByAdjustId(businessId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_stockCalculatesPieceCount() {
        Long businessId = 1001L;
        ErpStockInDO stockIn = new ErpStockInDO().setId(businessId).setNo("QTRK-001");
        ErpStockInItemDO item = new ErpStockInItemDO()
                .setInId(businessId)
                .setProductId(2001L)
                .setCount(new BigDecimal("12"))
                .setPackageQty(5);
        when(securityFrameworkService.hasPermission("erp:stock-in:print")).thenReturn(true);
        when(stockInService.getStockIn(businessId)).thenReturn(stockIn);
        when(stockInService.getStockInItemListByInId(businessId)).thenReturn(List.of(item));
        when(productService.getProductVOMap(Set.of(2001L)))
                .thenReturn(Map.of(2001L, new ErpProductRespVO().setId(2001L).setPackageQty(4)));

        Map<String, Object> result = printService.getPrintData("stock_in", businessId);

        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(5, items.get(0).get("items.packageQty"));
        assertEquals("2.4", items.get(0).get("items.pieceCount"));
        verify(stockInService).getStockIn(businessId);
        verify(stockInService).getStockInItemListByInId(businessId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void stockInPrintUsesRequestedWholeDocumentOrderAndRenumbersRows() {
        Long businessId = 1002L;
        List<ErpStockInItemDO> sortedItems = List.of(
                new ErpStockInItemDO().setId(20L).setCount(new BigDecimal("8")),
                new ErpStockInItemDO().setId(10L).setCount(new BigDecimal("2")));
        when(securityFrameworkService.hasPermission("erp:stock-in:print")).thenReturn(true);
        when(stockInService.getStockIn(businessId)).thenReturn(new ErpStockInDO().setId(businessId));
        when(stockInService.getStockInItemListByInId(businessId, "count", "desc"))
                .thenReturn(sortedItems);

        Map<String, Object> result = printService.getPrintData(
                "stock_in", businessId, "count", "DESC");

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("items");
        assertEquals(20L, rows.get(0).get("items.id"));
        assertEquals(1, rows.get(0).get("items.seq"));
        assertEquals(10L, rows.get(1).get("items.id"));
        assertEquals(2, rows.get(1).get("items.seq"));
        verify(stockInService).getStockInItemListByInId(businessId, "count", "desc");
        verify(stockInService, never()).getStockInItemListByInId(businessId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_saleOutEnrichesDetailFields() {
        Long businessId = 1001L;
        Long customerId = 2001L;
        Long sourceId = 3001L;
        Long sourceCreatorId = 4001L;
        Long auditorId = 5001L;
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setId(businessId)
                .setNo("XS-001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setSettleStatus(1)
                .setCustomerId(customerId)
                .setAuditorId(auditorId)
                .setSourceType(ErpSaleBizSourceTypeEnum.QUOTE.getType())
                .setSourceId(sourceId)
                .setDiscountPrice(new BigDecimal("8.00"))
                .setOtherPrice(new BigDecimal("3.50"))
                .setTotalPrice(new BigDecimal("100.00"))
                .setReceiptPrice(new BigDecimal("40.00"))
                .setSenderName("发货员A");
        ErpCustomerDO customer = new ErpCustomerDO().setId(customerId).setName("星宇客户").setCode("CUST-001")
                .setMobile("13800138000").setTelephone("0571-88888888").setAddress("杭州市西湖区文三路");
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO().setId(sourceId).setFreightType("到付");
        ErpSaleOutItemDO firstItem = new ErpSaleOutItemDO().setId(7001L).setOutId(businessId);
        ErpSaleOutItemDO secondItem = new ErpSaleOutItemDO().setId(7002L).setOutId(businessId);
        quote.setCreator(String.valueOf(sourceCreatorId));
        quote.setCreateTime(LocalDateTime.of(2026, 9, 3, 10, 30));
        AdminUserRespDTO sourceCreator = new AdminUserRespDTO();
        sourceCreator.setId(sourceCreatorId);
        sourceCreator.setNickname("来源制单人");
        AdminUserRespDTO auditor = new AdminUserRespDTO();
        auditor.setId(auditorId);
        auditor.setNickname("发货审核人");
        when(securityFrameworkService.hasPermission("erp:sale-out:print")).thenReturn(true);
        when(saleOutService.getSaleOut(businessId)).thenReturn(saleOut);
        when(saleOutService.getSaleOutItemListByOutId(businessId)).thenReturn(List.of(firstItem, secondItem));
        when(customerService.getCustomerMap(Set.of(customerId))).thenReturn(Map.of(customerId, customer));
        when(customerService.getCustomer(customerId)).thenReturn(customer);
        when(receivableAccountMapper.selectByCustomerId(customerId)).thenReturn(
                new ErpReceivableAccountDO().setReceivableBalance(new BigDecimal("250.00")));
        ErpSalePickDeliveryItemRespVO pickItem = new ErpSalePickDeliveryItemRespVO();
        pickItem.setPickUserName("王五");
        ErpSalePickDeliveryItemRespVO duplicatePickItem = new ErpSalePickDeliveryItemRespVO();
        duplicatePickItem.setPickUserName("张三");
        when(salePickDeliveryService.getSaleOutPickDeliveryItemPage(eq(businessId), any())).thenReturn(
                new PageResult<>(List.of(pickItem, duplicatePickItem), 2L));
        when(stockOutBillService.getStockOutBillListBySaleOutId(businessId)).thenReturn(List.of(
                new ErpStockOutBillDO().setPickUserName("张三"),
                new ErpStockOutBillDO().setPick("李四")));
        when(saleQuoteService.getSaleQuote(sourceId)).thenReturn(quote);
        when(adminUserApi.getUser(sourceCreatorId)).thenReturn(sourceCreator);
        when(salePickDeliveryService.getSaleOutDeliveryUserNames(businessId)).thenReturn("送货员甲、送货员乙");

        Map<String, Object> result = printService.getPrintData("sale_out", businessId);

        Map<String, Object> document = (Map<String, Object>) result.get("document");
        assertEquals("CUST-001", document.get("customerCode"));
        assertEquals("杭州市西湖区文三路", document.get("customerAddress"));
        assertEquals("13800138000", document.get("customerPhone"));
        assertEquals("已审核", document.get("statusName"));
        assertEquals("部分结算", document.get("settleStatusName"));
        assertEquals("3.5", document.get("feeAmount"));
        assertEquals("8", document.get("reductionAmount"));
        assertEquals("100", document.get("afterReductionAmount"));
        assertEquals("100", document.get("totalAmount"));
        assertEquals("100", document.get("currentDebt"));
        assertEquals("250", document.get("previousReceivable"));
        assertEquals("350", document.get("totalDebt"));
        assertEquals("王五、张三、李四", document.get("pickerName"));
        assertEquals("送货员甲、送货员乙", document.get("checkerName"));
        assertEquals("送货员甲、送货员乙", document.get("senderName"));
        assertEquals("2026-09-03", document.get("sourceCreateTime"));
        assertEquals("来源制单人", document.get("sourceCreatorName"));
        assertEquals("到付", document.get("freightType"));
        Map<String, Object> main = (Map<String, Object>) result.get("main");
        assertEquals("CUST-001", main.get("document.customerCode"));
        assertEquals("杭州市西湖区文三路", main.get("document.customerAddress"));
        assertEquals("13800138000", main.get("document.customerPhone"));
        assertEquals("已审核", main.get("document.statusName"));
        assertEquals("部分结算", main.get("document.settleStatusName"));
        assertEquals("壹佰元整", main.get("document.totalAmountUpper"));
        assertEquals("100", main.get("document.currentDebt"));
        assertEquals("250", main.get("document.previousReceivable"));
        assertEquals("350", main.get("document.totalDebt"));
        assertEquals("王五、张三、李四", main.get("document.pickerName"));
        assertEquals("送货员甲、送货员乙", main.get("document.checkerName"));
        assertEquals("送货员甲、送货员乙", main.get("document.senderName"));
        assertEquals("来源制单人", main.get("document.sourceCreatorName"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(2, items.size());
        assertEquals(1, items.get(0).get("items.seq"));
        assertEquals(2, items.get(1).get("items.seq"));
        verify(saleOutService).getSaleOut(businessId);
        verify(saleOutService).getSaleOutItemListByOutId(businessId);
        verify(saleQuoteService).getSaleQuote(sourceId);
        when(salePickDeliveryService.getSaleOutDeliveryUserNames(businessId)).thenReturn("");
        Map<String, Object> emptyData = printService.getPrintData("sale_out", businessId);
        Map<String, Object> emptyMain = (Map<String, Object>) emptyData.get("main");
        assertEquals("", emptyMain.get("document.senderName"));
        assertEquals("", emptyMain.get("document.checkerName"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_saleReturnEnrichesCustomerContact() {
        Long businessId = 1101L;
        Long customerId = 2101L;
        ErpSaleReturnDO saleReturn = new ErpSaleReturnDO()
                .setId(businessId)
                .setNo("ST2610140001")
                .setCustomerId(customerId)
                .setReturnTime(LocalDateTime.of(2026, 10, 14, 9, 30))
                .setTotalPrice(new BigDecimal("3600.00"));
        ErpCustomerDO customer = new ErpCustomerDO()
                .setId(customerId)
                .setName("贵州零雨商贸有限公司")
                .setAddress("贵州省贵阳市南明区富源南路200号")
                .setMobile("13541313593")
                .setTelephone("0851-88888888");
        when(securityFrameworkService.hasPermission("erp:sale-return:print")).thenReturn(true);
        when(saleReturnService.getSaleReturn(businessId)).thenReturn(saleReturn);
        when(saleReturnService.getSaleReturnItemListByReturnId(businessId)).thenReturn(List.of());
        when(customerService.getCustomerMap(Set.of(customerId))).thenReturn(Map.of(customerId, customer));
        when(customerService.getCustomer(customerId)).thenReturn(customer);

        Map<String, Object> result = printService.getPrintData("sale_return", businessId);

        Map<String, Object> document = (Map<String, Object>) result.get("document");
        Map<String, Object> main = (Map<String, Object>) result.get("main");
        assertEquals("贵州省贵阳市南明区富源南路200号", document.get("customerAddress"));
        assertEquals("13541313593", document.get("customerPhone"));
        assertEquals("贵州省贵阳市南明区富源南路200号", main.get("document.customerAddress"));
        assertEquals("13541313593", main.get("document.customerPhone"));
        assertEquals("叁仟陆佰元整", main.get("document.totalPriceUpper"));
        verify(saleReturnService).getSaleReturn(businessId);
        verify(saleReturnService).getSaleReturnItemListByReturnId(businessId);
        verify(customerService).getCustomer(customerId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_stockTransferOutEnrichesDirectCustomerName() {
        Long businessId = 1001L;
        Long cartId = 2001L;
        Long customerId = 3001L;
        ErpStockMoveDO stockMove = new ErpStockMoveDO()
                .setId(businessId)
                .setNo("DBCK-001")
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType())
                .setSourceId(cartId);
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setCustomerId(customerId);
        ErpCustomerDO customer = new ErpCustomerDO().setId(customerId).setName("直发客户A");
        when(securityFrameworkService.hasPermission("erp:stock-transfer-out:print")).thenReturn(true);
        when(stockMoveService.getVisibleStockTransferOut(businessId)).thenReturn(stockMove);
        when(stockMoveService.getStockMoveItemListByMoveId(businessId)).thenReturn(List.of());
        when(saleCartService.getSaleCart(cartId)).thenReturn(cart);
        when(customerService.getCustomer(customerId)).thenReturn(customer);

        Map<String, Object> result = printService.getPrintData("stock_transfer_out", businessId);

        Map<String, Object> document = (Map<String, Object>) result.get("document");
        assertEquals("直发客户A", document.get("directCustomerName"));
        Map<String, Object> main = (Map<String, Object>) result.get("main");
        assertEquals("直发客户A", main.get("document.directCustomerName"));
        verify(stockMoveService).getVisibleStockTransferOut(businessId);
        verify(saleCartService).getSaleCart(cartId);
        verify(customerService).getCustomer(customerId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_stockTransferOutAddsTemplateFieldsForNormalTransfer() {
        Long businessId = 1007L;
        Long productId = 2007L;
        Long warehouseId = 3007L;
        ErpStockMoveDO stockMove = new ErpStockMoveDO()
                .setId(businessId)
                .setNo("QCDB20261016000002")
                .setMoveTime(LocalDateTime.of(2026, 10, 16, 9, 48, 40))
                .setTotalCount(new BigDecimal("32"))
                .setTotalPrice(new BigDecimal("3536"));
        ErpStockMoveItemDO firstItem = new ErpStockMoveItemDO()
                .setMoveId(businessId)
                .setProductId(productId)
                .setFromWarehouseId(warehouseId)
                .setFromShelf("A-01")
                .setPackageQty(12)
                .setCount(new BigDecimal("24"))
                .setProductPrice(new BigDecimal("68"))
                .setTotalPrice(new BigDecimal("1632"));
        ErpStockMoveItemDO secondItem = new ErpStockMoveItemDO()
                .setMoveId(businessId)
                .setProductId(productId)
                .setFromWarehouseId(warehouseId)
                .setFromShelf("A-02")
                .setPackageQty(4)
                .setCount(new BigDecimal("8"))
                .setProductPrice(new BigDecimal("238"))
                .setTotalPrice(new BigDecimal("1904"));
        ErpProductRespVO product = new ErpProductRespVO()
                .setId(productId)
                .setCode("148642")
                .setName("美孚1号经典表现 0W-20 SQ GF-7")
                .setVehicleModel("汽机油")
                .setOriginPlace("美孚");
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(warehouseId).setName("蛟龙港");

        when(securityFrameworkService.hasPermission("erp:stock-transfer-out:print")).thenReturn(true);
        when(stockMoveService.getVisibleStockTransferOut(businessId)).thenReturn(stockMove);
        when(stockMoveService.getStockMoveItemListByMoveId(businessId)).thenReturn(List.of(firstItem, secondItem));
        when(productService.getProductVOMap(Set.of(productId))).thenReturn(Map.of(productId, product));
        Map<Long, ErpWarehouseDO> warehouses = new java.util.LinkedHashMap<>();
        warehouses.put(warehouseId, warehouse);
        when(warehouseService.getWarehouseMap(Set.of(warehouseId))).thenReturn(warehouses);

        Map<String, Object> result = printService.getPrintData("stock_transfer_out", businessId);

        Map<String, Object> main = (Map<String, Object>) result.get("main");
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals("2026/10/16 09:48:40", main.get("document.moveDateTime"));
        assertEquals("4", main.get("document.totalPieceCount"));
        assertEquals("3,536.00", main.get("document.totalPriceFormatted"));
        assertEquals(true, String.valueOf(main.get("document.printDateTime"))
                .matches("\\d{4}/\\d{1,2}/\\d{1,2} \\d{2}:\\d{2}:\\d{2}"));
        assertEquals("148642", items.get(0).get("items.productCode"));
        assertEquals("美孚1号经典表现 0W-20 SQ GF-7", items.get(0).get("items.productName"));
        assertEquals("汽机油", items.get(0).get("items.vehicleModel"));
        assertEquals("美孚", items.get(0).get("items.originPlace"));
        assertEquals("蛟龙港", items.get(0).get("items.fromWarehouseName"));
        assertEquals("A-01", items.get(0).get("items.fromShelf"));
        assertEquals("2", items.get(0).get("items.pieceCount"));
        assertEquals("2", items.get(1).get("items.pieceCount"));
    }

    private FieldDefinitionRespDTO definition(String key, String label, Integer sort) {
        FieldDefinitionRespDTO definition = new FieldDefinitionRespDTO();
        definition.setFieldKey(key);
        definition.setFieldLabel(label);
        definition.setSort(sort);
        return definition;
    }

    private String fieldCode(ErpPrintFieldRespVO fields, String name) {
        return fields.getGroups().stream()
                .flatMap(group -> group.getFields().stream())
                .filter(field -> name.equals(field.getName()))
                .findFirst()
                .orElseThrow()
                .getCode();
    }

}
