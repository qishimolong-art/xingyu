package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpAccountingSubjectDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpAccountingSubjectCodeConstants;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpAuxiliaryTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_DEBIT_CREDIT_NOT_BALANCE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_SUBJECT_CODE_MISSING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * {@link ErpAutoVoucherBuilder} 单元测试。
 *
 * 覆盖：
 *  - 8 个 build 方法（采购入库/采购退货/销售出库/销售退货/其他入库/其他出库 + 5 张业务单据）
 *  - filterZeroLines 零额行过滤 + lineNo 重排
 *  - buildLine 科目反查异常路径
 *  - 借贷平衡校验（重点：S1 Bug 暴露）
 *
 * 重点 Bug 暴露：
 *  - S1（生产 Block）：buildPurchaseInItems 借方 = totalProductPrice + totalTaxPrice，
 *    贷方 = totalPrice，但 totalPrice = totalProductPrice + totalTaxPrice - discountPrice + otherPrice。
 *    一旦客户填了 discountPrice 或 otherPrice，借贷不等。
 */
public class ErpAutoVoucherBuilderTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpAutoVoucherBuilder builder;

    @Mock
    private ErpAccountingSubjectService subjectService;

    /**
     * 注册 8 个固定科目，避免每个用例都重复 Mock。
     * 使用 lenient() 防止 UnnecessaryStubbingException —— 不同用例只用其中几个。
     */
    private void mockAllSubjects() {
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.CASH)))
                .thenReturn(buildSubject(1L, "1001", "现金"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.BANK)))
                .thenReturn(buildSubject(2L, "1002", "银行存款"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.AR)))
                .thenReturn(buildSubject(3L, "1122", "应收账款"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.PREPAID)))
                .thenReturn(buildSubject(4L, "1123", "预付账款"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.OTHER_RECEIVABLE)))
                .thenReturn(buildSubject(5L, "1221", "其他应收款"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.INVENTORY)))
                .thenReturn(buildSubject(6L, "1405", "库存商品"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.LOSS_AND_GAIN)))
                .thenReturn(buildSubject(7L, "1901", "待处理财产损益"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.AP)))
                .thenReturn(buildSubject(8L, "2202", "应付账款"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.PRE_RECEIPT)))
                .thenReturn(buildSubject(9L, "2203", "预收账款"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.PRE_RECEIVABLE)))
                .thenReturn(buildSubject(10L, "2204", "预收账款(另)"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.TAX_PAYABLE)))
                .thenReturn(buildSubject(11L, "2221", "应交税费"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.OTHER_PAYABLE)))
                .thenReturn(buildSubject(12L, "2241", "其他应付款"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.REVENUE)))
                .thenReturn(buildSubject(13L, "6001", "主营业务收入"));
        lenient().when(subjectService.getSubjectByCode(eq(ErpAccountingSubjectCodeConstants.COST)))
                .thenReturn(buildSubject(14L, "6401", "主营业务成本"));
    }

    private ErpAccountingSubjectDO buildSubject(Long id, String code, String name) {
        return ErpAccountingSubjectDO.builder()
                .id(id).subjectCode(code).subjectName(name)
                .isLeaf(true).build();
    }

    private static BigDecimal sumDebit(List<ErpVoucherItemDO> items) {
        return items.stream().map(ErpVoucherItemDO::getDebitAmount)
                .filter(d -> d != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumCredit(List<ErpVoucherItemDO> items) {
        return items.stream().map(ErpVoucherItemDO::getCreditAmount)
                .filter(d -> d != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== buildPurchaseInItems ====================

    @Test
    @DisplayName("buildPurchaseInItems：正常 - 借库存+借税金=贷应付，3 行借贷平衡")
    public void testBuildPurchaseInItems_normal() {
        mockAllSubjects();
        ErpPurchaseInDO in = new ErpPurchaseInDO()
                .setSupplierId(100L)
                .setTotalProductPrice(new BigDecimal("100.00"))
                .setTotalTaxPrice(new BigDecimal("13.00"))
                .setDiscountPrice(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO)
                .setTotalPrice(new BigDecimal("113.00"));

        List<ErpVoucherItemDO> items = builder.buildPurchaseInItems(in, "供应商A");

        assertEquals(3, items.size());
        assertThat(sumDebit(items)).isEqualByComparingTo("113.00");
        assertThat(sumCredit(items)).isEqualByComparingTo("113.00");
        // 借应付分录摘要 + 辅助核算正确
        ErpVoucherItemDO ap = items.get(2);
        assertEquals("2202", ap.getSubjectCode());
        assertEquals(ErpAuxiliaryTypeEnum.SUPPLIER.getType(), ap.getAuxiliaryType());
        assertEquals(100L, ap.getAuxiliaryId());
        assertEquals("采购入库 - 供应商A", ap.getSummary());
    }

    @Test
    @DisplayName("buildPurchaseInItems：税额为 0 时过滤零额行，剩 2 行")
    public void testBuildPurchaseInItems_zeroTax_filterEmptyLine() {
        mockAllSubjects();
        ErpPurchaseInDO in = new ErpPurchaseInDO()
                .setSupplierId(100L)
                .setTotalProductPrice(new BigDecimal("100.00"))
                .setTotalTaxPrice(BigDecimal.ZERO)
                .setDiscountPrice(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO)
                .setTotalPrice(new BigDecimal("100.00"));

        List<ErpVoucherItemDO> items = builder.buildPurchaseInItems(in, "供应商A");

        assertEquals(2, items.size(), "税额为 0，应交税费行应被过滤");
        // lineNo 应该被重排为 1, 2（不留空号）
        assertEquals(1, items.get(0).getLineNo());
        assertEquals(2, items.get(1).getLineNo());
        assertThat(sumDebit(items)).isEqualByComparingTo(sumCredit(items));
    }

    @Test
    @DisplayName("buildPurchaseInItems：supplierName 为 null 时摘要拼成 '采购入库 - '（不报 NPE）")
    public void testBuildPurchaseInItems_nullSupplierName() {
        mockAllSubjects();
        ErpPurchaseInDO in = new ErpPurchaseInDO()
                .setSupplierId(100L)
                .setTotalProductPrice(new BigDecimal("100.00"))
                .setTotalTaxPrice(new BigDecimal("13.00"))
                .setDiscountPrice(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO)
                .setTotalPrice(new BigDecimal("113.00"));

        List<ErpVoucherItemDO> items = builder.buildPurchaseInItems(in, null);

        assertEquals("采购入库 - ", items.get(0).getSummary());
    }

    @Test
    @DisplayName("S1 修复：buildPurchaseInItems 当 discountPrice > 0 时 validateBalance 抛 VOUCHER_DEBIT_CREDIT_NOT_BALANCE")
    public void testBuildPurchaseInItems_bugS1_discountUnbalance() {
        // S1 业务场景：客户填了优惠/折扣 discountPrice = 13.00
        // S1 修复后：builder 末尾调 validateBalance；
        //   生成的分录：借 1405=100 + 借 2221=13 + 贷 2202=100 + 借 1002=13（折扣分录）
        //   借方合计 = 126，贷方合计 = 100 → validateBalance 直接抛 VOUCHER_DEBIT_CREDIT_NOT_BALANCE，
        //   保护下游凭证生成不写入坏数据。
        mockAllSubjects();
        ErpPurchaseInDO in = new ErpPurchaseInDO()
                .setSupplierId(100L)
                .setTotalProductPrice(new BigDecimal("100.00"))
                .setTotalTaxPrice(new BigDecimal("13.00"))
                .setDiscountPrice(new BigDecimal("13.00"))
                .setOtherPrice(BigDecimal.ZERO)
                .setTotalPrice(new BigDecimal("100.00"));

        AssertUtils.assertServiceException(
                () -> builder.buildPurchaseInItems(in, "供应商A"),
                VOUCHER_DEBIT_CREDIT_NOT_BALANCE,
                "126.00", "100.00");
    }

    @Test
    @DisplayName("S1 修复：buildPurchaseInItems 当 otherPrice > 0 时 validateBalance 抛 VOUCHER_DEBIT_CREDIT_NOT_BALANCE")
    public void testBuildPurchaseInItems_bugS1_otherPriceUnbalance() {
        mockAllSubjects();
        ErpPurchaseInDO in = new ErpPurchaseInDO()
                .setSupplierId(100L)
                .setTotalProductPrice(new BigDecimal("100.00"))
                .setTotalTaxPrice(new BigDecimal("13.00"))
                .setDiscountPrice(BigDecimal.ZERO)
                .setOtherPrice(new BigDecimal("5.00"))
                .setTotalPrice(new BigDecimal("118.00"));  // 100 + 13 - 0 + 5

        // S1 修复后：借 1405=100 + 借 2221=13 + 贷 2202=118 + 贷 1002=5（其他费用分录）
        // 借方合计 = 113，贷方合计 = 123 → validateBalance 抛 VOUCHER_DEBIT_CREDIT_NOT_BALANCE
        AssertUtils.assertServiceException(
                () -> builder.buildPurchaseInItems(in, "供应商A"),
                VOUCHER_DEBIT_CREDIT_NOT_BALANCE,
                "113.00", "123.00");
    }

    // ==================== buildPurchaseReturnItems ====================

    @Test
    @DisplayName("buildPurchaseReturnItems：红字反向 - 借应付/贷库存/贷税，借贷平衡")
    public void testBuildPurchaseReturnItems_normal() {
        mockAllSubjects();
        ErpPurchaseReturnDO returnDO = new ErpPurchaseReturnDO()
                .setSupplierId(100L)
                .setTotalProductPrice(new BigDecimal("100.00"))
                .setTotalTaxPrice(new BigDecimal("13.00"))
                .setDiscountPrice(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO)
                .setTotalPrice(new BigDecimal("113.00"));

        List<ErpVoucherItemDO> items = builder.buildPurchaseReturnItems(returnDO, "供应商A");

        assertEquals(3, items.size());
        assertThat(sumDebit(items)).isEqualByComparingTo("113.00");
        assertThat(sumCredit(items)).isEqualByComparingTo("113.00");
        // 第一行借应付（红字反向）
        assertEquals("2202", items.get(0).getSubjectCode());
        assertThat(items.get(0).getDebitAmount()).isEqualByComparingTo("113.00");
    }

    // ==================== buildSaleOutItems ====================

    @Test
    @DisplayName("buildSaleOutItems：5 行分录 - 借应收/贷收入/贷税 + 借成本/贷库存")
    public void testBuildSaleOutItems_normal() {
        mockAllSubjects();
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setCustomerId(200L)
                .setTotalProductPrice(new BigDecimal("100.00"))
                .setTotalTaxPrice(new BigDecimal("13.00"))
                .setDiscountPrice(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO)
                .setTotalPrice(new BigDecimal("113.00"));
        BigDecimal sumCost = new BigDecimal("60.00");

        List<ErpVoucherItemDO> items = builder.buildSaleOutItems(saleOut, "客户B", sumCost);

        assertEquals(5, items.size());
        // 借方 = totalPrice + sumCost = 113 + 60 = 173
        // 贷方 = totalProductPrice + totalTaxPrice + sumCost = 100 + 13 + 60 = 173
        assertThat(sumDebit(items)).isEqualByComparingTo("173.00");
        assertThat(sumCredit(items)).isEqualByComparingTo("173.00");
        // 应收第一行的辅助核算 customer
        assertEquals(ErpAuxiliaryTypeEnum.CUSTOMER.getType(), items.get(0).getAuxiliaryType());
        assertEquals(200L, items.get(0).getAuxiliaryId());
    }

    @Test
    @DisplayName("buildSaleOutItems：sumCost=0 时过滤成本+库存两行")
    public void testBuildSaleOutItems_zeroCost_filterTwoLines() {
        mockAllSubjects();
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setCustomerId(200L)
                .setTotalProductPrice(new BigDecimal("100.00"))
                .setTotalTaxPrice(new BigDecimal("13.00"))
                .setTotalPrice(new BigDecimal("113.00"));

        List<ErpVoucherItemDO> items = builder.buildSaleOutItems(saleOut, "客户B", BigDecimal.ZERO);

        assertEquals(3, items.size(), "sumCost=0 时成本+库存两行应过滤");
        // lineNo 重排
        assertEquals(1, items.get(0).getLineNo());
        assertEquals(2, items.get(1).getLineNo());
        assertEquals(3, items.get(2).getLineNo());
    }

    // ==================== buildSaleReturnItems ====================

    @Test
    @DisplayName("buildSaleReturnItems：红字反向 - 贷应收/借收入/借税/贷成本/借库存")
    public void testBuildSaleReturnItems_normal() {
        mockAllSubjects();
        ErpSaleReturnDO returnDO = new ErpSaleReturnDO()
                .setCustomerId(200L)
                .setTotalProductPrice(new BigDecimal("100.00"))
                .setTotalTaxPrice(new BigDecimal("13.00"))
                .setTotalPrice(new BigDecimal("113.00"));
        BigDecimal sumCost = new BigDecimal("60.00");

        List<ErpVoucherItemDO> items = builder.buildSaleReturnItems(returnDO, "客户B", sumCost);

        assertEquals(5, items.size());
        assertThat(sumDebit(items)).isEqualByComparingTo("173.00");
        assertThat(sumCredit(items)).isEqualByComparingTo("173.00");
        // 第一行贷应收（红字反向）
        assertThat(items.get(0).getCreditAmount()).isEqualByComparingTo("113.00");
    }

    // ==================== buildStockInItems / buildStockOutItems ====================

    @Test
    @DisplayName("buildStockInItems：借 1405 库存 / 贷 1901 待处理财产损益")
    public void testBuildStockInItems_normal() {
        mockAllSubjects();
        ErpStockInDO stockIn = new ErpStockInDO().setNo("QTRK20260520000001");
        BigDecimal sumCost = new BigDecimal("88.00");

        List<ErpVoucherItemDO> items = builder.buildStockInItems(stockIn, sumCost);

        assertEquals(2, items.size());
        assertEquals("1405", items.get(0).getSubjectCode());
        assertThat(items.get(0).getDebitAmount()).isEqualByComparingTo("88.00");
        assertEquals("1901", items.get(1).getSubjectCode());
        assertThat(items.get(1).getCreditAmount()).isEqualByComparingTo("88.00");
        assertEquals("其他入库 - QTRK20260520000001", items.get(0).getSummary());
    }

    @Test
    @DisplayName("buildStockOutItems：借 1901 / 贷 1405（其他入库的镜像）")
    public void testBuildStockOutItems_normal() {
        mockAllSubjects();
        ErpStockOutDO stockOut = new ErpStockOutDO().setNo("QCKD20260520000001");
        BigDecimal sumCost = new BigDecimal("66.00");

        List<ErpVoucherItemDO> items = builder.buildStockOutItems(stockOut, sumCost);

        assertEquals(2, items.size());
        assertEquals("1901", items.get(0).getSubjectCode());
        assertThat(items.get(0).getDebitAmount()).isEqualByComparingTo("66.00");
        assertEquals("1405", items.get(1).getSubjectCode());
        assertThat(items.get(1).getCreditAmount()).isEqualByComparingTo("66.00");
    }

    // ==================== 5 张业务单据 ====================

    @Test
    @DisplayName("buildOtherReceivableItems：借 1221 其他应收款 / 贷 1002 银行存款")
    public void testBuildOtherReceivableItems_normal() {
        mockAllSubjects();
        ErpOtherReceivableDO receivable = new ErpOtherReceivableDO()
                .setPartyType(1).setPartyId(300L).setPartyName("客户C")
                .setActualAmount(new BigDecimal("500.00"));

        List<ErpVoucherItemDO> items = builder.buildOtherReceivableItems(receivable);

        assertEquals(2, items.size());
        assertEquals("1221", items.get(0).getSubjectCode());
        assertEquals(ErpAuxiliaryTypeEnum.CUSTOMER.getType(), items.get(0).getAuxiliaryType());
        assertEquals(300L, items.get(0).getAuxiliaryId());
        assertEquals("1002", items.get(1).getSubjectCode());
        assertThat(sumDebit(items)).isEqualByComparingTo(sumCredit(items));
    }

    @Test
    @DisplayName("buildOtherReceivableItems：actualAmount 为 null 时取 ZERO（兜底防 NPE） + 全部过滤")
    public void testBuildOtherReceivableItems_nullActualAmount() {
        mockAllSubjects();
        ErpOtherReceivableDO receivable = new ErpOtherReceivableDO()
                .setPartyType(1).setPartyId(300L).setPartyName("客户C")
                .setActualAmount(null);

        List<ErpVoucherItemDO> items = builder.buildOtherReceivableItems(receivable);

        // actualAmount=null → 兜底 ZERO → 两行都被过滤
        assertEquals(0, items.size());
    }

    @Test
    @DisplayName("buildPreReceiptItems：借 1002 银行 / 贷 2203 预收账款 + 辅助核算映射 partyType")
    public void testBuildPreReceiptItems_normal() {
        mockAllSubjects();
        ErpPreReceiptDO preReceipt = new ErpPreReceiptDO()
                .setPartyType(2).setPartyId(400L).setPartyName("供应商D")
                .setActualAmount(new BigDecimal("1000.00"));

        List<ErpVoucherItemDO> items = builder.buildPreReceiptItems(preReceipt);

        assertEquals(2, items.size());
        assertEquals("1002", items.get(0).getSubjectCode());
        assertEquals("2203", items.get(1).getSubjectCode());
        // partyType=2 → supplier
        assertEquals(ErpAuxiliaryTypeEnum.SUPPLIER.getType(), items.get(1).getAuxiliaryType());
    }

    @Test
    @DisplayName("buildPrePaymentItems：借 1123 预付账款 / 贷 1002 银行 + partyType=3 → person")
    public void testBuildPrePaymentItems_personType() {
        mockAllSubjects();
        ErpPrePaymentDO prePayment = new ErpPrePaymentDO()
                .setPartyType(3).setPartyId(500L).setPartyName("员工E")
                .setActualAmount(new BigDecimal("200.00"));

        List<ErpVoucherItemDO> items = builder.buildPrePaymentItems(prePayment);

        assertEquals(2, items.size());
        assertEquals("1123", items.get(0).getSubjectCode());
        assertEquals(ErpAuxiliaryTypeEnum.PERSON.getType(), items.get(0).getAuxiliaryType());
    }

    @Test
    @DisplayName("buildPreReceivableItems：借 1002 银行 / 贷 2204 预收账款(另)")
    public void testBuildPreReceivableItems_normal() {
        mockAllSubjects();
        ErpPreReceivableDO preReceivable = new ErpPreReceivableDO()
                .setPartyType(1).setPartyId(600L).setPartyName("客户F")
                .setActualAmount(new BigDecimal("300.00"));

        List<ErpVoucherItemDO> items = builder.buildPreReceivableItems(preReceivable);

        assertEquals(2, items.size());
        assertEquals("1002", items.get(0).getSubjectCode());
        assertEquals("2204", items.get(1).getSubjectCode());
    }

    @Test
    @DisplayName("buildOtherPayableItems：借 2241 / 贷 1002")
    public void testBuildOtherPayableItems_normal() {
        mockAllSubjects();
        ErpOtherPayableDO payable = new ErpOtherPayableDO()
                .setPartyType(1).setPartyId(700L).setPartyName("客户G")
                .setActualAmount(new BigDecimal("400.00"));

        List<ErpVoucherItemDO> items = builder.buildOtherPayableItems(payable);

        assertEquals(2, items.size());
        assertEquals("2241", items.get(0).getSubjectCode());
        assertEquals("1002", items.get(1).getSubjectCode());
    }

    @Test
    @DisplayName("mapPartyTypeToAuxiliary：partyType 为 null 时辅助核算类型为 null（不报 NPE）")
    public void testBuildOtherPayableItems_nullPartyType() {
        mockAllSubjects();
        ErpOtherPayableDO payable = new ErpOtherPayableDO()
                .setPartyType(null).setPartyName("X")
                .setActualAmount(new BigDecimal("50.00"));

        List<ErpVoucherItemDO> items = builder.buildOtherPayableItems(payable);

        assertEquals(2, items.size());
        // 第一行（其他应付）的辅助核算类型应为 null
        assertEquals(null, items.get(0).getAuxiliaryType());
    }

    @Test
    @DisplayName("mapPartyTypeToAuxiliary：未知 partyType（如 99）映射为 null")
    public void testBuildPreReceiptItems_unknownPartyType() {
        mockAllSubjects();
        ErpPreReceiptDO preReceipt = new ErpPreReceiptDO()
                .setPartyType(99).setPartyName("X")
                .setActualAmount(new BigDecimal("50.00"));

        List<ErpVoucherItemDO> items = builder.buildPreReceiptItems(preReceipt);

        // 贷 2203 行 auxiliaryType 应为 null（未知类型映射失败，但不报错）
        assertEquals(null, items.get(1).getAuxiliaryType());
    }

    // ==================== buildLine 异常路径 ====================

    @Test
    @DisplayName("buildLine：科目编码不存在抛 VOUCHER_SUBJECT_CODE_MISSING")
    public void testBuildLine_subjectMissing() {
        // 不 mock subjectService，所有科目都返回 null
        ErpPurchaseInDO in = new ErpPurchaseInDO()
                .setSupplierId(100L)
                .setTotalProductPrice(new BigDecimal("100.00"))
                .setTotalTaxPrice(BigDecimal.ZERO)
                .setTotalPrice(new BigDecimal("100.00"));

        AssertUtils.assertServiceException(
                () -> builder.buildPurchaseInItems(in, "供应商A"),
                VOUCHER_SUBJECT_CODE_MISSING, "1405");
    }

    // ==================== scale 标准化 ====================

    @Test
    @DisplayName("buildLine：金额自动 setScale(2, HALF_UP) 标准化")
    public void testBuildLine_scale2HalfUp() {
        mockAllSubjects();
        ErpStockInDO stockIn = new ErpStockInDO().setNo("QTRK001");
        // 输入 3 位小数：123.456
        BigDecimal sumCost = new BigDecimal("123.456");

        List<ErpVoucherItemDO> items = builder.buildStockInItems(stockIn, sumCost);

        // 应该 setScale(2, HALF_UP) → 123.46
        assertThat(items.get(0).getDebitAmount()).isEqualByComparingTo("123.46");
        assertEquals(2, items.get(0).getDebitAmount().scale());
    }

    @Test
    @DisplayName("filterZeroLines：null 借贷视作 ZERO 一并过滤")
    public void testFilterZeroLines_nullAsZero() {
        mockAllSubjects();
        ErpPurchaseInDO in = new ErpPurchaseInDO()
                .setSupplierId(100L)
                // 全为 null：3 行都该被过滤
                .setTotalProductPrice(null)
                .setTotalTaxPrice(null)
                .setTotalPrice(null);

        List<ErpVoucherItemDO> items = builder.buildPurchaseInItems(in, "供应商A");

        assertEquals(0, items.size(), "全部为 null（兜底为 0）应被过滤为 0 行");
    }

}
