package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.ErpReceivableAccountController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.*;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.*;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.*;
import cn.iocoder.yudao.module.erp.service.sale.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ErpReceivableDetailDisplayTest {
    private final ErpReceivableDetailDisplayService service = new ErpReceivableDetailDisplayService();
    private final LocalDateTime time = LocalDateTime.of(2026, 9, 27, 12, 34, 56);
    private final ErpReceivableDetailReqVO req = new ErpReceivableDetailReqVO();
    private MockedStatic<SecurityFrameworkUtils> security;

    @BeforeEach
    void setup() {
        for (Field field : ErpReceivableDetailDisplayService.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()))
                ReflectionTestUtils.setField(service, field.getName(), mock(field.getType()));
        }
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ErpVoucherDO.class);
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9L);
        req.setCustomerId(1L);
        when(dep("customerService", ErpCustomerService.class).getCustomer(1L))
                .thenReturn(ErpCustomerDO.builder().id(1L).name("测试客户").build());
    }

    @AfterEach
    void close() { security.close(); }

    @Test
    void mapsActualSaleFieldsAndCumulativeAmountsWithoutChangingLedgerValues() {
        ErpSaleOutDO sale = ErpSaleOutDO.builder().id(10L).auditorId(8L).approveTime(time)
                .deliveryMethod("配送").settleMethod("月结").billAmount(new BigDecimal("25"))
                .invoiceAmount(new BigDecimal("99")).billNo("票001、票002").remark("备注").internalNote("说明").build();
        when(dep("saleOutMapper", ErpSaleOutMapper.class).selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(sale));
        when(dep("userApi", AdminUserApi.class).getUserMap(anyCollection())).thenReturn(
                Collections.singletonMap(8L, new AdminUserRespDTO().setId(8L).setNickname("审核员")));
        ErpReceivableDetailRespVO row = row("销售出库", 10L, time, "100", "160");
        row.setAllocatedAmount(new BigDecimal("40"));
        List<ErpReceivableDetailRespVO> result = decorate(Collections.singletonList(row));
        assertThat(result.get(0).getAuditorName()).isEqualTo("审核员");
        assertThat(row.getApproveTime()).isEqualTo(time);
        assertThat(row.getDeliveryMethod()).isEqualTo("配送");
        assertThat(row.getSettleMethod()).isEqualTo("月结");
        assertThat(row.getBillAmount()).isEqualByComparingTo("25");
        assertThat(row.getBillNo()).isEqualTo("票001、票002");
        assertThat(row.getRemark()).isEqualTo("备注");
        assertThat(row.getInternalNote()).isEqualTo("说明");
        assertThat(row.getInvoiceStatus()).isNull();
        assertThat(row.getCustomerName()).isEqualTo("测试客户");
        assertThat(row.getReceivedAmount()).isEqualByComparingTo("40");
        assertThat(row.getReceivedWriteOffAmount()).isEqualByComparingTo("40");
        assertThat(row.getReceiptAmount()).isZero();
        assertThat(row.getBalance()).isEqualByComparingTo("160");
    }

    @Test
    void returnsAndAdjustmentsUseEffectiveAllocationWhileReceiptsRemainZero() {
        when(dep("saleReturnMapper", ErpSaleReturnMapper.class).selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(ErpSaleReturnDO.builder().id(11L).billNo("退票")
                        .deliveryMethod("自提").settleMethod("现结").remark("退货备注").build()));
        when(dep("priceAdjustMapper", ErpSalePriceAdjustMapper.class).selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(ErpSalePriceAdjustDO.builder().id(12L).remark("调价备注").build()));
        when(dep("receiptMapper", ErpFinanceReceiptMapper.class).selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(ErpFinanceReceiptDO.builder().id(13L).remark("收款备注").build()));
        when(dep("otherMapper", ErpReceivableOtherMapper.class).selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(new ErpReceivableOtherDO().setId(14L).setVoucherNo("自填凭证")));
        List<ErpReceivableDetailRespVO> rows = Arrays.asList(row("销售退货", 11L, time, "0", "1"),
                row("销售调价", 12L, time, "0", "1"), row("收款单", 13L, time, "0", "1"),
                row("应收调账", 14L, time, "0", "1"));
        for (ErpReceivableDetailRespVO row : rows) row.setAllocatedAmount(new BigDecimal("-12"));
        rows.get(2).setReceiptAmount(new BigDecimal("100"));
        decorate(rows);
        assertThat(rows.get(0).getReceivedAmount()).isEqualByComparingTo("12");
        assertThat(rows.get(1).getReceivedAmount()).isEqualByComparingTo("12");
        assertThat(rows.get(2).getReceivedAmount()).isZero();
        assertThat(rows.get(2).getReceiptAmount()).isEqualByComparingTo("100");
        assertThat(rows.get(3).getReceivedAmount()).isZero();
        assertThat(rows.get(3).getVoucherNo()).isEqualTo("自填凭证");
        assertThat(rows.get(0).getBillAmount()).isNull();
        assertThat(rows.get(1).getApproveTime()).isNull();
        assertThat(rows.get(2).getAuditorName()).isNull();
    }

    @Test
    void independentOtherReceivablesAreNotAddedToReceivableDetails() {
        ErpReceivableDetailRespVO sale = row("销售出库", 10L, time, "100", "160");
        List<ErpReceivableDetailRespVO> rows = decorate(Collections.singletonList(sale));
        assertThat(rows).containsExactly(sale);
        assertThat(rows).extracting(ErpReceivableDetailRespVO::getDocType).doesNotContain("其他应收");
        assertThat(sale.getPrevBalance()).isEqualByComparingTo("100");
        assertThat(sale.getBalance()).isEqualByComparingTo("160");
        assertThat(Arrays.stream(ErpReceivableDetailDisplayService.class.getDeclaredFields())
                .map(Field::getType)).doesNotContain(ErpReceivableMiscMapper.class);
    }

    @Test
    void hiddenVoucherNumberDoesNotFallBackToAssociatedVoucher() {
        when(dep("permissionApi", PermissionApi.class).hasAnyPermissions(9L, "erp:voucher:query")).thenReturn(true);
        when(dep("permissionApi", PermissionApi.class).getCurrentUserHiddenFields("erp_sale_out"))
                .thenReturn(Collections.singletonList("col_voucherNo"));
        ErpReceivableDetailRespVO row = row("销售出库", 10L, time, "0", "100");
        decorate(Collections.singletonList(row));
        assertThat(row.getVoucherNo()).isNull();
        verifyNoInteractions(dep("voucherMapper", ErpVoucherMapper.class));
    }

    @Test
    void metadataQueriesAreBatchedAndNotExecutedPerDocument() {
        List<ErpReceivableDetailRespVO> rows = new ArrayList<>();
        for (long id = 1; id <= 501; id++) rows.add(row("销售出库", id, time, "0", "100"));
        decorate(rows);
        org.mockito.ArgumentCaptor<Collection<Long>> ids = org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(dep("saleOutMapper", ErpSaleOutMapper.class), times(2)).selectBatchIds(ids.capture());
        assertThat(ids.getAllValues().get(0)).hasSize(500);
        assertThat(ids.getAllValues().get(1)).containsExactly(501L);
    }

    @Test
    void hiddenSourceFieldsStayEmptyAndCannotBeReconstructedFromAmounts() {
        ErpSaleOutDO sale = ErpSaleOutDO.builder().id(10L).billAmount(BigDecimal.TEN).billNo("秘密票号")
                .remark("秘密备注").internalNote("秘密说明").auditorId(8L).approveTime(time).build();
        when(dep("saleOutMapper", ErpSaleOutMapper.class).selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(sale));
        when(dep("saleFieldMasker", ErpSaleFieldPermissionMasker.class).getHiddenFieldSet(eq("erp_sale_out"), any(Object.class)))
                .thenReturn(new HashSet<>(Arrays.asList("billAmount", "billNo", "remark", "internalNote",
                        "auditorId", "approveTime", "receiptPrice")));
        ErpReceivableDetailRespVO row = row("销售出库", 10L, time, "0", "100");
        row.setAllocatedAmount(BigDecimal.TEN);
        decorate(Collections.singletonList(row));
        assertThat(row.getBillAmount()).isNull();
        assertThat(row.getBillNo()).isNull();
        assertThat(row.getRemark()).isNull();
        assertThat(row.getInternalNote()).isNull();
        assertThat(row.getAuditorName()).isNull();
        assertThat(row.getApproveTime()).isNull();
        assertThat(row.getReceivedAmount()).isNull();
        assertThat(row.getReceivedWriteOffAmount()).isNull();
        assertThat(row.getBalance()).isEqualByComparingTo("100");
    }

    @Test
    void vouchersUseExactTypeAndIdAndDeduplicateNumbers() {
        when(dep("permissionApi", PermissionApi.class).hasAnyPermissions(9L, "erp:voucher:query")).thenReturn(true);
        when(dep("voucherMapper", ErpVoucherMapper.class).selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            Wrapper<?> query = invocation.getArgument(0);
            assertThat(query.getSqlSegment()).contains("source_biz_type", "source_biz_id");
            return Arrays.asList(ErpVoucherDO.builder().sourceBizId(10L).voucherNo("记001").build(),
                    ErpVoucherDO.builder().sourceBizId(10L).voucherNo("记001").build(),
                    ErpVoucherDO.builder().sourceBizId(10L).voucherNo("记002").build());
        });
        ErpReceivableDetailRespVO row = row("销售出库", 10L, time, "0", "100");
        decorate(Collections.singletonList(row));
        assertThat(row.getVoucherNo()).isEqualTo("记001、记002");
    }

    @Test
    void exportHas23BusinessColumnsWithoutIndependentOtherReceivables() throws Exception {
        ErpReceivableDetailRespVO row = row("销售出库", 10L, time, "0", "100");
        row.setApproveTime(time);
        row.setBillNo("000123");
        ErpReceivableAccountService api = mock(ErpReceivableAccountService.class);
        when(api.getReceivableDetailList(req)).thenReturn(Collections.singletonList(row));
        ErpReceivableAccountController controller = new ErpReceivableAccountController();
        ReflectionTestUtils.setField(controller, "receivableAccountService", api);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportReceivableDetail(req, response);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            assertThat(sheet.getRow(0).getLastCellNum()).isEqualTo((short) 23);
            assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(5))).isEqualTo("收款金额");
            assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(18))).isEqualTo("已核销金额");
            assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(21))).isEqualTo("应收调账");
            List<String> headers = new ArrayList<>();
            sheet.getRow(0).forEach(cell -> headers.add(formatter.formatCellValue(cell)));
            assertThat(headers).doesNotContain("其他应收");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(1))).isEqualTo("2026-09-27 12:34:56");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(9))).isEqualTo("2026-09-27 12:34:56");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(11))).isEmpty();
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(13))).isEqualTo("000123");
        }
    }

    private ErpReceivableDetailRespVO row(String type, Long id, LocalDateTime date, String opening, String balance) {
        ErpReceivableDetailRespVO row = new ErpReceivableDetailRespVO();
        row.setDocType(type); row.setBizId(id); row.setDocNo(type + id); row.setDocDate(date);
        row.setPrevBalance(new BigDecimal(opening)); row.setBalance(new BigDecimal(balance));
        row.setReceiptAmount(BigDecimal.ZERO);
        return row;
    }

    private List<ErpReceivableDetailRespVO> decorate(List<ErpReceivableDetailRespVO> rows) {
        return service.decorate(req, rows);
    }

    private <T> T dep(String name, Class<T> type) {
        return type.cast(ReflectionTestUtils.getField(service, name));
    }
}
