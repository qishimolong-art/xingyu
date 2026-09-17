package cn.iocoder.yudao.module.erp.service.finance.accounting;
import cn.iocoder.yudao.module.erp.service.finance.accounting.rule.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.math.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherSourceReader.*;

@ExtendWith(MockitoExtension.class) @MockitoSettings(strictness=Strictness.LENIENT)
class ErpVoucherRuleCalculatorTest {
    @InjectMocks ErpVoucherRuleCalculator calculator;
    @Mock ErpVoucherRuleStore store; @Mock ErpVoucherSourceReader reader; @Mock ErpVoucherAuxiliarySupport auxiliaries;
    Config config; Context context; Preview preview;
    @BeforeEach void setup() {
        config=new Config();config.setTaxpayer("GENERAL");
        long id=1;
        for(Template t:ErpVoucherTemplates.ALL) {
            config.getEnabledScenarios().add(t.getCode());
            for(String role:t.getRoles()){Mapping m=new Mapping();m.setScenario(t.getCode());m.setRole(role);m.setSubjectId(id++);config.getMappings().add(m);}
        }
        for(long account=1;account<=2;account++){AccountMapping a=new AccountMapping();a.setAccountId(account);a.setSubjectId(900+account);config.getAccounts().add(a);}
        when(store.subject(anyLong())).thenAnswer(i->new ErpAccountingSubjectDO().setId(i.getArgument(0)).setSubjectCode("S"+i.getArgument(0)).setSubjectName("测试科目").setIsLeaf(true).setEnable(true));
        when(reader.one(eq("erpAccountMapper"),anyLong())).thenAnswer(i->map("id",i.getArgument(1),"name","账户","status",0));
        context=new Context();context.setPriceBasis("INCLUSIVE");context.setInvoiceStatus("RECEIVED");context.setDeductionStatus("NOT_APPLICABLE");context.setTaxAmount(BigDecimal.ZERO);context.setRevenueConfirmed(true);
        preview=new Preview();preview.setVoucherDate(LocalDate.of(2026,9,30));
    }
    @ParameterizedTest @CsvSource({"2,SALE","3,SALE_RETURN","8,PURCHASE","9,PURCHASE_RETURN","4,OTHER_AR_CREATE","10,OTHER_AP_REPAY","6,RECEIPT","12,PAYMENT","16,USAGE","17,STOCK_GAIN","18,TRANSFER","21,PRE_RECEIPT","22,PRE_PAYMENT","23,PRE_RECEIVABLE"})
    void fourteenSourcesBalance(int type,String scenario) {
        Source source=source(type,"100","100");context.setScenario(scenario);
        if(type==18)context.setPriceBasis("CNY");
        List<ErpVoucherItemDO> items=calculator.calculate(source,context,config,preview);
        assertFalse(items.isEmpty());assertBalance(items);
    }
    @Test void purchaseDiscountAndFreightDoNotTouchBank() {
        Source s=source(8,"95","90");s.getHeader().put("discountPrice",bd("10"));s.getHeader().put("feeAmount",bd("5"));s.getHeader().put("supplierId",3L);
        context.setScenario("PURCHASE");context.setDiscountNature("COMMERCIAL");context.setFeeConfirmed(true);context.setFeeAlreadyPosted(false);context.setFeeNature("PROCUREMENT");context.setFeePartyType(2);context.setFeePartyId(3L);
        List<ErpVoucherItemDO> items=calculator.calculate(s,context,config,preview);assertBalance(items);
        assertEquals(0,items.stream().filter(i->i.getSubjectId()>=900).count());
        assertEquals(0,items.stream().map(ErpVoucherItemDO::getDebitAmount).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(bd("95")));
    }
    @Test void inputTaxAndSmallTaxpayerProduceDifferentInventoryBasis() {
        Source s=source(8,"113","100");s.getHeader().put("totalProductPrice",bd("113"));context.setScenario("PURCHASE");context.setTaxAmount(bd("13"));context.setDeductionStatus("DEDUCTIBLE");
        assertBalance(calculator.calculate(s,context,config,preview));
        config.setTaxpayer("SMALL");assertThrows(RuntimeException.class,()->calculator.calculate(s,context,config,preview));
        context.setDeductionStatus("NON_DEDUCTIBLE");assertThrows(RuntimeException.class,()->calculator.calculate(s,context,config,preview));
        s.getStocks().get(0).put("totalPrice",bd("113"));assertBalance(calculator.calculate(s,context,config,preview));
    }
    @Test void saleGiftStillCarriesCost() {
        Source s=source(2,"0","70");s.getHeader().put("totalProductPrice",BigDecimal.ZERO);s.getDetails().get(0).put("totalPrice",BigDecimal.ZERO);context.setScenario("SALE");
        List<ErpVoucherItemDO> items=calculator.calculate(s,context,config,preview);assertEquals(2,items.size());assertBalance(items);
    }
    @Test void cashDiscountUsesActualReceiptAndFinancialExpense() {
        Source s=source(6,"100","0");s.getHeader().put("receiptPrice",bd("90"));s.getHeader().put("discountPrice",bd("10"));context.setScenario("RECEIPT");context.setDiscountNature("CASH");
        List<ErpVoucherItemDO> items=calculator.calculate(s,context,config,preview);assertBalance(items);
        assertEquals(0,items.stream().filter(i->i.getSubjectId()==901L).findFirst().get().getDebitAmount().compareTo(bd("90")));
    }
    @Test void missingAndMismatchedStockCannotPass() {
        Source s=source(8,"100","99");context.setScenario("PURCHASE");assertThrows(RuntimeException.class,()->calculator.calculate(s,context,config,preview));
        s.getStocks().clear();assertThrows(RuntimeException.class,()->calculator.calculate(s,context,config,preview));
    }
    @Test void cancellationAmountsAreNettedInsteadOfAbsoluteSummation() {
        Source s=source(8,"100","100");
        s.getStocks().add(map("bizItemId",11L,"count",bd("-1"),"totalPrice",bd("-100")));
        s.getStocks().add(map("bizItemId",11L,"count",bd("1"),"totalPrice",bd("100")));
        assertEquals(0,ErpVoucherRuleCalculator.stockCosts(s).get(11L).compareTo(bd("100")));
    }
    @Test void nearestCategoryMappingOverridesDefault() {
        Source s=source(2,"100","70");s.getCategories().put(8L,20L);context.setScenario("SALE");
        when(reader.one("erpProductCategoryMapper",20L)).thenReturn(map("parentId",10L));
        when(reader.one("erpProductCategoryMapper",10L)).thenReturn(map("parentId",0L));
        Mapping m=new Mapping();m.setScenario("SALE");m.setRole("REVENUE");m.setCategoryId(10L);m.setSubjectId(777L);config.getMappings().add(m);
        assertTrue(calculator.calculate(s,context,config,preview).stream().anyMatch(i->i.getSubjectId()==777L));
    }
    @Test void provisionalRequiresMonthEndAndNoInputTax() {
        Source s=source(8,"100","100");context.setScenario("PROVISIONAL");context.setInvoiceStatus("NOT_RECEIVED");assertBalance(calculator.calculate(s,context,config,preview));
        preview.setVoucherDate(LocalDate.of(2026,9,29));assertThrows(RuntimeException.class,()->calculator.calculate(s,context,config,preview));
    }
    @Test void allocationNeverCreatesOppositeSignDueToRounding() {
        Map<Long,BigDecimal> weights=new LinkedHashMap<>();for(long i=1;i<=4;i++)weights.put(i,BigDecimal.ONE);
        Map<Long,BigDecimal> shares=ErpVoucherRuleCalculator.allocate(bd("0.02"),weights);
        assertTrue(shares.values().stream().allMatch(v->v.signum()>=0));assertEquals(0,shares.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(bd("0.02")));
    }
    @ParameterizedTest @CsvSource({"6,RECEIPT_ADVANCE","12,PAYMENT_ADVANCE","4,OTHER_AR_RECOVER","10,OTHER_AP_CREATE","10,EXPENSE_ACCRUAL","16,STOCK_LOSS","17,USAGE_RETURN"})
    void remainingControlledScenariosBalance(int type,String scenario) {
        context.setScenario(scenario);assertBalance(calculator.calculate(source(type,"100","100"),context,config,preview));
    }
    @Test void unsetTaxpayerBlocksTaxTemplatesButAllowsOrdinarySettlement() {
        config.setTaxpayer("UNSET");context.setScenario("SALE");assertThrows(RuntimeException.class,()->calculator.calculate(source(2,"100","80"),context,config,preview));
        context.setScenario("RECEIPT");assertBalance(calculator.calculate(source(6,"100","0"),context,config,preview));
    }
    @ParameterizedTest @CsvSource({"6,RECEIPT","12,PAYMENT"})
    void refundReversesOnlyActualFundAmount(int type,String scenario) {
        context.setScenario(scenario);List<ErpVoucherItemDO> items=calculator.calculate(source(type,"-40","0"),context,config,preview);assertBalance(items);
        ErpVoucherItemDO fund=items.stream().filter(i->i.getSubjectId()==901L).findFirst().get();assertEquals(0,(type==6?fund.getCreditAmount():fund.getDebitAmount()).compareTo(bd("40")));
    }
    @Test void bankTransferUsesTwoExplicitAccountsAndSeparatesFee() {
        context.setScenario("TRANSFER");context.setPriceBasis("CNY");Source s=source(18,"100","0");s.getHeader().put("feePrice",bd("2"));
        List<ErpVoucherItemDO> items=calculator.calculate(s,context,config,preview);assertBalance(items);assertEquals(3,items.size());
        assertEquals(0,items.stream().filter(i->i.getSubjectId()==901L).findFirst().get().getCreditAmount().compareTo(bd("102")));
        assertEquals(0,items.stream().filter(i->i.getSubjectId()==902L).findFirst().get().getDebitAmount().compareTo(bd("100")));
    }
    @ParameterizedTest @CsvSource({"100,100,0,0","105,100,0,5","90,90,10,0"})
    void purchaseDiscountAndFreightAlsoWorkIndependently(String amount,String cost,String discount,String fee) {
        Source s=source(8,amount,cost);s.getHeader().put("discountPrice",bd(discount));s.getHeader().put("feeAmount",bd(fee));s.getHeader().put("supplierId",3L);
        context.setScenario("PURCHASE");context.setDiscountNature("COMMERCIAL");context.setFeeConfirmed(true);context.setFeeAlreadyPosted(false);context.setFeeNature("PROCUREMENT");context.setFeePartyType(2);context.setFeePartyId(3L);
        List<ErpVoucherItemDO> items=calculator.calculate(s,context,config,preview);assertBalance(items);assertTrue(items.stream().noneMatch(i->i.getSubjectId()>=900));
    }
    static Source source(int type,String amount,String cost) {
        Source s=new Source();s.setType(type);s.setId(1L);
        Map<String,Object> h=map("no","TEST","status",20,"outTime",LocalDateTime.of(2026,9,1,0,0),"totalPrice",bd(amount),"totalProductPrice",bd("100"),"accountId",1L,"inAccountId",2L,"outAccountId",1L,"exchangeRate",BigDecimal.ONE);
        if(type==6)h.put("receiptPrice",bd(amount));if(type==12)h.put("paymentPrice",bd(amount));if(type==18)h.put("transferPrice",bd(amount));s.setHeader(h);
        if(Arrays.asList(2,3,8,9,16,17).contains(type)) {
            int sign=Arrays.asList(2,9,16).contains(type)?-1:1;
            s.getDetails().add(map("id",11L,"productId",8L,"count",BigDecimal.ONE,"totalPrice",bd(amount)));
            s.getStocks().add(map("bizItemId",11L,"count",BigDecimal.valueOf(sign),"totalPrice",bd(cost).multiply(BigDecimal.valueOf(sign))));
        }
        return s;
    }
    static Map<String,Object> map(Object... args){Map<String,Object> m=new TreeMap<>();for(int i=0;i<args.length;i+=2)m.put((String)args[i],args[i+1]);return m;}
    static BigDecimal bd(String n){return new BigDecimal(n);}
    static void assertBalance(List<ErpVoucherItemDO> items){assertEquals(0,items.stream().map(ErpVoucherItemDO::getDebitAmount).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(items.stream().map(ErpVoucherItemDO::getCreditAmount).reduce(BigDecimal.ZERO,BigDecimal::add)));}
}
