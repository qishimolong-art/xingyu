package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.ErpReceivableAccountController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherService.RECEIPT_DISCOUNT_SOURCE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpReceivableDetailTimeTest {

    private final ErpReceivableAccountServiceImpl service = new ErpReceivableAccountServiceImpl();
    private final LocalDateTime created = LocalDateTime.of(2026, 9, 24, 14, 25, 36);

    @Test
    void ordinaryAndFreightAdjustmentsUseCreationTimeEvenWhenBusinessDateDiffers() {
        for (String sourceType : Arrays.asList(null, "调账", "销售手推车")) {
            ErpReceivableOtherDO item = adjustment();
            item.setSourceType(sourceType);
            item.setBizTime(LocalDate.of(2026, 9, 20));
            assertThat(resolve(item, Collections.emptyMap())).isEqualTo(created);
        }
    }

    @Test
    void laterEditsAndStatusChangesDoNotChangeDisplayedCreationTime() {
        ErpReceivableOtherDO item = adjustment();
        for (int status : new int[]{0, 10, 20}) {
            item.setStatus(status);
            item.setUpdateTime(created.plusDays(2));
            assertThat(resolve(item, Collections.emptyMap())).isEqualTo(created);
        }
    }

    @Test
    void receiptDiscountRetainsSourceReceiptTimePriority() {
        ErpReceivableOtherDO item = adjustment();
        item.setSourceType(RECEIPT_DISCOUNT_SOURCE_TYPE).setSourceId(9L);
        LocalDateTime receiptTime = created.minusHours(1);
        assertThat(resolve(item, Collections.singletonMap(9L, receiptTime))).isEqualTo(receiptTime);
    }

    @Test
    void missingReceiptTimeOrSourceIdFallsBackToCreationTime() {
        ErpReceivableOtherDO item = adjustment();
        item.setSourceType(RECEIPT_DISCOUNT_SOURCE_TYPE).setSourceId(9L);
        assertThat(resolve(item, Collections.emptyMap())).isEqualTo(created);
        item.setSourceId(null);
        assertThat(resolve(item, Collections.emptyMap())).isEqualTo(created);
    }

    @Test
    void legacyMissingCreationTimeRetainsBusinessDateFallbackAndHandlesNull() {
        ErpReceivableOtherDO item = adjustment();
        item.setCreateTime(null);
        assertThat(resolve(item, Collections.emptyMap())).isEqualTo(item.getBizTime().atStartOfDay());
        item.setBizTime(null);
        assertThat(resolve(item, Collections.emptyMap())).isNull();
    }

    @Test
    void detailOrdersByActualTimeAndCalculatesRunningBalances() throws Exception {
        List<ErpReceivableDetailRespVO> rows = mixedDetails();
        assertThat(rows).extracting(ErpReceivableDetailRespVO::getDocNo)
                .containsExactly("XSCK-1", "XSCK-2", "QTYS-1");
        assertThat(rows.get(2).getDocDate()).isEqualTo(created);
        assertThat(rows).extracting(ErpReceivableDetailRespVO::getPrevBalance)
                .containsExactly(new BigDecimal("0"), new BigDecimal("720"), new BigDecimal("741"));
        assertThat(rows).extracting(ErpReceivableDetailRespVO::getBalance)
                .containsExactly(new BigDecimal("720"), new BigDecimal("741"), new BigDecimal("841"));
    }

    @Test
    void detailAndExcelExportKeepTheSameTimestampToSeconds() throws Exception {
        List<ErpReceivableDetailRespVO> rows = mixedDetails();
        ErpReceivableAccountService apiService = mock(ErpReceivableAccountService.class);
        ErpReceivableDetailReqVO req = new ErpReceivableDetailReqVO();
        req.setCustomerId(1L);
        when(apiService.getReceivableDetailList(req)).thenReturn(rows);
        ErpReceivableAccountController controller = new ErpReceivableAccountController();
        ReflectionTestUtils.setField(controller, "receivableAccountService", apiService);
        assertThat(controller.getReceivableDetailList(req).getData().get(2).getDocDate()).isEqualTo(created);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportReceivableDetail(req, response);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertThat(new DataFormatter().formatCellValue(workbook.getSheetAt(0).getRow(3).getCell(1)))
                    .isEqualTo("2026-09-24 14:25:36");
        }
    }

    private LocalDateTime resolve(ErpReceivableOtherDO item, Map<Long, LocalDateTime> receipts) {
        return ReflectionTestUtils.invokeMethod(service, "resolveOtherReceivableDocDate", item, receipts);
    }

    private ErpReceivableOtherDO adjustment() {
        ErpReceivableOtherDO item = new ErpReceivableOtherDO().setId(3L).setNo("QTYS-1")
                .setBizTime(created.toLocalDate()).setReceivableAmount(new BigDecimal("100"));
        item.setCreateTime(created);
        return item;
    }

    private List<ErpReceivableDetailRespVO> mixedDetails() throws Exception {
        // 使用公开明细服务串联查询、排序、余额计算；数据库访问由 mock 隔离。
        for (Field field : ErpReceivableAccountServiceImpl.class.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                ReflectionTestUtils.setField(service, field.getName(), mock(field.getType()));
            }
        }
        ErpSaleOutMapper sales = (ErpSaleOutMapper) ReflectionTestUtils.getField(service, "saleOutMapper");
        ErpReceivableOtherMapper others = (ErpReceivableOtherMapper) ReflectionTestUtils.getField(service, "receivableOtherMapper");
        when(sales.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(Arrays.asList(
                ErpSaleOutDO.builder().id(2L).no("XSCK-2").outTime(created.minusMinutes(1))
                        .totalPrice(new BigDecimal("21")).build(),
                ErpSaleOutDO.builder().id(1L).no("XSCK-1").outTime(created.minusDays(10))
                        .totalPrice(new BigDecimal("720")).build()));
        when(others.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.singletonList(adjustment()));
        ErpReceivableDetailReqVO req = new ErpReceivableDetailReqVO();
        req.setCustomerId(1L);
        return service.getReceivableDetailList(req, new ErpFinanceVisibleScope(true, null, null));
    }
}
