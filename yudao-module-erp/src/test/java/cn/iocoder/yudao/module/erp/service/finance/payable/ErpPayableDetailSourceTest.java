package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.ErpPayableAccountController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableOtherService.PAYMENT_DISCOUNT_SOURCE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpPayableDetailSourceTest {

    private final ErpPayableAccountServiceImpl service = new ErpPayableAccountServiceImpl();
    private final LocalDateTime time = LocalDateTime.of(2026, 9, 27, 10, 0);

    @Test
    void detailsLinkToSourceIdsAndRetainBalances() {
        List<ErpPayableDetailRespVO> rows = details(81L, "CGDD-81", PAYMENT_DISCOUNT_SOURCE_TYPE, 91L, "FK-91");
        assertThat(rows).extracting(ErpPayableDetailRespVO::getDocNo)
                .containsExactly("QT-3", "RK-1", "TH-2");
        assertThat(rows).extracting(ErpPayableDetailRespVO::getSourceType)
                .containsExactly("FINANCE_PAYMENT", "PURCHASE_ORDER", "PURCHASE_ORDER");
        assertThat(rows).extracting(ErpPayableDetailRespVO::getSourceId).containsExactly(91L, 81L, 82L);
        assertThat(rows).extracting(ErpPayableDetailRespVO::getBalance)
                .containsExactly(new BigDecimal("-5"), new BigDecimal("95"), new BigDecimal("75"));
    }

    @Test
    void missingOrInvalidSourceIdsKeepTextWithoutLink() {
        for (Long id : Arrays.asList(null, 0L, -1L)) {
            List<ErpPayableDetailRespVO> rows = details(id, "CGDD-81", PAYMENT_DISCOUNT_SOURCE_TYPE, id, "FK-91");
            assertNoLink(rows.get(0));
            assertNoLink(rows.get(1));
            assertThat(rows.get(0).getSourceNo()).isEqualTo("FK-91");
            assertThat(rows.get(1).getSourceNo()).isEqualTo("CGDD-81");
        }
    }

    @Test
    void blankNumbersDoNotProduceLinks() {
        for (String no : Arrays.asList(null, "", "  ")) {
            List<ErpPayableDetailRespVO> rows = details(81L, no, PAYMENT_DISCOUNT_SOURCE_TYPE, 91L, no);
            assertNoLink(rows.get(0));
            assertNoLink(rows.get(1));
        }
    }

    @Test
    void unknownAdjustmentSourceNeverGuessesFromNumberOrId() {
        for (String type : Arrays.asList(null, "调账", "其他应付", "未知类型")) {
            List<ErpPayableDetailRespVO> rows = details(81L, "CGDD-81", type, 91L, "FK-91");
            assertNoLink(rows.get(0));
        }
    }

    @Test
    void exportKeepsOriginalColumnsAndSourceNumberText() throws Exception {
        List<ErpPayableDetailRespVO> rows = details(81L, "CGDD-81", PAYMENT_DISCOUNT_SOURCE_TYPE, 91L, "FK-91");
        ErpPayableAccountService apiService = mock(ErpPayableAccountService.class);
        ErpPayableDetailReqVO req = new ErpPayableDetailReqVO();
        when(apiService.getPayableDetailList(req)).thenReturn(rows);
        ErpPayableAccountController controller = new ErpPayableAccountController();
        ReflectionTestUtils.setField(controller, "payableAccountService", apiService);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportPayableDetail(req, response);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getLastCellNum()).isEqualTo((short) 15);
            assertThat(sheet.getRow(0).getCell(9).getStringCellValue()).isEqualTo("来源单号");
            assertThat(sheet.getRow(1).getCell(9).getStringCellValue()).isEqualTo("FK-91");
            assertThat(sheet.getRow(1).getCell(9).getHyperlink()).isNull();
        }
    }

    private void assertNoLink(ErpPayableDetailRespVO row) {
        assertThat(row.getSourceType()).isNull();
        assertThat(row.getSourceId()).isNull();
    }

    private List<ErpPayableDetailRespVO> details(Long orderId, String orderNo,
                                                String sourceType, Long sourceId, String sourceNo) {
        for (Field field : ErpPayableAccountServiceImpl.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                ReflectionTestUtils.setField(service, field.getName(), mock(field.getType()));
            }
        }
        ErpPurchaseInMapper ins = (ErpPurchaseInMapper) ReflectionTestUtils.getField(service, "purchaseInMapper");
        ErpPurchaseReturnMapper returns = (ErpPurchaseReturnMapper) ReflectionTestUtils.getField(service, "purchaseReturnMapper");
        ErpPayableOtherMapper others = (ErpPayableOtherMapper) ReflectionTestUtils.getField(service, "payableOtherMapper");
        when(ins.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(
                ErpPurchaseInDO.builder().id(1L).no("RK-1").inTime(time).totalPrice(new BigDecimal("100"))
                        .orderId(orderId).orderNo(orderNo).build()));
        when(returns.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(
                ErpPurchaseReturnDO.builder().id(2L).no("TH-2").returnTime(time.plusHours(1))
                        .totalPrice(new BigDecimal("20")).orderId(82L).orderNo("CGDD-82").build()));
        when(others.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(
                new ErpPayableOtherDO().setId(3L).setNo("QT-3").setBizTime(time.toLocalDate())
                        .setPayableAmount(new BigDecimal("-5")).setSourceType(sourceType)
                        .setSourceId(sourceId).setSourceNo(sourceNo)));
        ErpPayableDetailReqVO req = new ErpPayableDetailReqVO();
        req.setSupplierId(1L);
        return service.getPayableDetailList(req, new ErpFinanceVisibleScope(true, null, null));
    }
}
