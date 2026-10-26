package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDrilldownReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.ErpPayableAccountController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.ErpReceivableAccountController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDrilldownReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpAccountDrilldownSummaryRespVO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPayableDrilldownTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableDrilldownTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableAccountServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableAccountService;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableAccountServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableAccountService;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpAccountDrilldownContractTest {

    @Test
    void receivableTypesAreWhitelistedAndMiscPermissionFlagIsExact() {
        assertThat(Arrays.stream(ErpReceivableDrilldownTypeEnum.values()).map(Enum::name))
                .containsExactly("BALANCE", "OPENING_BALANCE", "MISC_RECEIVABLE", "RECEIPT", "SALE_OUT",
                        "SALE_RETURN");
        assertThat(ErpReceivableDrilldownTypeEnum.fromValue("erp_receivable_misc")).isNull();

        ErpReceivableDrilldownReqVO reqVO = new ErpReceivableDrilldownReqVO();
        reqVO.setDetailType("MISC_RECEIVABLE");
        assertThat(reqVO.isDetailTypeValid()).isTrue();
        assertThat(reqVO.isMiscDetail()).isTrue();
        reqVO.setDetailType("SALE_OUT");
        assertThat(reqVO.isMiscDetail()).isFalse();
        reqVO.setDetailType("DROP_TABLE");
        assertThat(reqVO.isDetailTypeValid()).isFalse();
    }

    @Test
    void payableTypesAreWhitelistedAndMiscPermissionFlagIsExact() {
        assertThat(Arrays.stream(ErpPayableDrilldownTypeEnum.values()).map(Enum::name))
                .containsExactly("BALANCE", "OPENING_BALANCE", "MISC_PAYABLE", "PURCHASE_IN", "PURCHASE_RETURN",
                        "PRICE_ADJUST", "PAYABLE_ADJUST", "PAYMENT", "WRITE_OFF");
        assertThat(ErpPayableDrilldownTypeEnum.fromValue("erp_payable_misc")).isNull();

        ErpPayableDrilldownReqVO reqVO = new ErpPayableDrilldownReqVO();
        reqVO.setDetailType("MISC_PAYABLE");
        assertThat(reqVO.isDetailTypeValid()).isTrue();
        assertThat(reqVO.isMiscDetail()).isTrue();
        reqVO.setDetailType("PAYMENT");
        assertThat(reqVO.isMiscDetail()).isFalse();
        reqVO.setDetailType("SELECT_ALL");
        assertThat(reqVO.isDetailTypeValid()).isFalse();
    }

    @Test
    void balanceSummaryKeepsOpeningAmountWhenTheSelectedPeriodHasNoRows() {
        ErpAccountDrilldownSummaryRespVO receivable = ReflectionTestUtils.invokeMethod(
                new ErpReceivableAccountServiceImpl(), "buildDrilldownSummary", Collections.emptyList(),
                ErpReceivableDrilldownTypeEnum.BALANCE, new BigDecimal("123.45"));
        assertThat(receivable.getOpeningAmount()).isEqualByComparingTo("123.45");
        assertThat(receivable.getTargetAmount()).isEqualByComparingTo("123.45");
        assertThat(receivable.getEndingAmount()).isEqualByComparingTo("123.45");

        ErpAccountDrilldownSummaryRespVO payable = ReflectionTestUtils.invokeMethod(
                new ErpPayableAccountServiceImpl(), "buildDrilldownSummary", Collections.emptyList(),
                ErpPayableDrilldownTypeEnum.BALANCE, new BigDecimal("88.00"));
        assertThat(payable.getOpeningAmount()).isEqualByComparingTo("88.00");
        assertThat(payable.getTargetAmount()).isEqualByComparingTo("88.00");
        assertThat(payable.getEndingAmount()).isEqualByComparingTo("88.00");
    }

    @Test
    void categorizedExportsContainTheDisplayedChangeAndCumulativeAmounts() throws Exception {
        ErpReceivableDetailRespVO receivableRow = new ErpReceivableDetailRespVO();
        receivableRow.setDocType("其他应收");
        receivableRow.setChangeAmount(new BigDecimal("-10000"));
        receivableRow.setPrevBalance(BigDecimal.ZERO);
        receivableRow.setBalance(new BigDecimal("-10000"));
        ErpReceivableAccountService receivableService = mock(ErpReceivableAccountService.class);
        ErpReceivableDrilldownReqVO receivableReq = new ErpReceivableDrilldownReqVO();
        receivableReq.setDetailType("MISC_RECEIVABLE");
        when(receivableService.getReceivableDrilldownList(receivableReq))
                .thenReturn(Collections.singletonList(receivableRow));
        ErpReceivableAccountController receivableController = new ErpReceivableAccountController();
        ReflectionTestUtils.setField(receivableController, "receivableAccountService", receivableService);
        ReflectionTestUtils.setField(receivableController, "fieldPermissionMasker", emptyFieldPermissionMasker());
        assertCategorizedExport(receivableController, receivableReq, true, "-10000.00");

        ErpPayableDetailRespVO payableRow = new ErpPayableDetailRespVO();
        payableRow.setDocType("其他应付");
        payableRow.setChangeAmount(new BigDecimal("-200"));
        payableRow.setPrevBalance(BigDecimal.ZERO);
        payableRow.setBalance(new BigDecimal("-200"));
        ErpPayableAccountService payableService = mock(ErpPayableAccountService.class);
        ErpPayableDrilldownReqVO payableReq = new ErpPayableDrilldownReqVO();
        payableReq.setDetailType("MISC_PAYABLE");
        when(payableService.getPayableDrilldownList(payableReq)).thenReturn(Collections.singletonList(payableRow));
        ErpPayableAccountController payableController = new ErpPayableAccountController();
        ReflectionTestUtils.setField(payableController, "payableAccountService", payableService);
        ReflectionTestUtils.setField(payableController, "fieldPermissionMasker", emptyFieldPermissionMasker());
        assertCategorizedExport(payableController, payableReq, false, "-200.00");
    }

    private ErpFinanceFieldPermissionMasker emptyFieldPermissionMasker() {
        ErpFinanceFieldPermissionMasker masker = mock(ErpFinanceFieldPermissionMasker.class);
        when(masker.getHiddenFields(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Collections.emptySet());
        return masker;
    }

    private void assertCategorizedExport(Object controller, Object reqVO, boolean receivable,
                                         String expectedChange) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        if (receivable) {
            ((ErpReceivableAccountController) controller).exportReceivableDrilldown(
                    (ErpReceivableDrilldownReqVO) reqVO, response);
        } else {
            ((ErpPayableAccountController) controller).exportPayableDrilldown(
                    (ErpPayableDrilldownReqVO) reqVO, response);
        }
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            DataFormatter formatter = new DataFormatter();
            assertThat(formatter.formatCellValue(workbook.getSheetAt(0).getRow(0).getCell(3))).isEqualTo("本笔变动");
            assertThat(formatter.formatCellValue(workbook.getSheetAt(0).getRow(1).getCell(3)))
                    .isEqualTo(expectedChange);
            assertThat(formatter.formatCellValue(workbook.getSheetAt(0).getRow(0).getCell(4))).isEqualTo("上笔累计");
            assertThat(formatter.formatCellValue(workbook.getSheetAt(0).getRow(0).getCell(5))).isEqualTo("累计金额");
        }
    }
}
