package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleQuoteService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutBillService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private PermissionApi permissionApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpSaleQuoteService saleQuoteService;
    @Mock
    private ErpSalePriceAdjustService salePriceAdjustService;
    @Mock
    private ErpSaleCartService saleCartService;
    @Mock
    private ErpStockInService stockInService;
    @Mock
    private ErpStockMoveService stockMoveService;
    @Mock
    private ErpStockOutBillService stockOutBillService;
    @Mock
    private ErpPurchaseOrderService purchaseOrderService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpReceivableAccountMapper receivableAccountMapper;

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
        assertEquals("document.checkerName", fieldCode(result, "验货人"));
        assertEquals("creator.nickname", fieldCode(result, "制单人"));
    }

    @Test
    void testGetFields_stockTransferOutDirectCustomer() {
        when(securityFrameworkService.hasPermission("erp:stock-transfer-out:print-template")).thenReturn(true);
        when(permissionApi.getFieldDefinitions("erp_stock_transfer_out")).thenReturn(List.of(
                definition("directCustomerName", "直发客户", 65)));

        ErpPrintFieldRespVO result = printService.getFields("stock_transfer_out");

        assertEquals("stock_transfer_out", result.getModuleKey());
        assertEquals("document.directCustomerName", fieldCode(result, "直发客户"));
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
        assertEquals("items.pieceCount", fieldCode(result, "件数"));
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
                .setReceiptPrice(new BigDecimal("40.00"));
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
        auditor.setNickname("验货审核人");
        when(securityFrameworkService.hasPermission("erp:sale-out:print")).thenReturn(true);
        when(saleOutService.getSaleOut(businessId)).thenReturn(saleOut);
        when(saleOutService.getSaleOutItemListByOutId(businessId)).thenReturn(List.of(firstItem, secondItem));
        when(customerService.getCustomerMap(Set.of(customerId))).thenReturn(Map.of(customerId, customer));
        when(customerService.getCustomer(customerId)).thenReturn(customer);
        when(receivableAccountMapper.selectByCustomerId(customerId)).thenReturn(
                new ErpReceivableAccountDO().setReceivableBalance(new BigDecimal("250.00")));
        when(stockOutBillService.getStockOutBillListBySaleOutId(businessId)).thenReturn(List.of(
                new ErpStockOutBillDO().setPickUserName("张三"),
                new ErpStockOutBillDO().setPick("李四")));
        when(saleQuoteService.getSaleQuote(sourceId)).thenReturn(quote);
        when(adminUserApi.getUser(sourceCreatorId)).thenReturn(sourceCreator);
        when(adminUserApi.getUser(auditorId)).thenReturn(auditor);

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
        assertEquals("张三、李四", document.get("pickerName"));
        assertEquals("验货审核人", document.get("checkerName"));
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
        assertEquals("张三、李四", main.get("document.pickerName"));
        assertEquals("验货审核人", main.get("document.checkerName"));
        assertEquals("来源制单人", main.get("document.sourceCreatorName"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(2, items.size());
        assertEquals(1, items.get(0).get("items.seq"));
        assertEquals(2, items.get(1).get("items.seq"));
        verify(saleOutService).getSaleOut(businessId);
        verify(saleOutService).getSaleOutItemListByOutId(businessId);
        verify(saleQuoteService).getSaleQuote(sourceId);
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
