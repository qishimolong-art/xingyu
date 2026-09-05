package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceTransferService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpFinanceTransferControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFinanceTransferController controller;

    @Mock
    private ErpFinanceTransferService financeTransferService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpVoucherMapper voucherMapper;

    @Test
    void getFinanceTransferPage_fillsHandlerAuditAndVoucherFields() {
        LocalDateTime auditTime = LocalDateTime.of(2026, 7, 20, 10, 30);
        ErpFinanceTransferDO transfer = new ErpFinanceTransferDO();
        transfer.setId(100L);
        transfer.setNo("YHZZ-100");
        transfer.setStatus(20);
        transfer.setFinanceUserId(9L);
        transfer.setUpdater("7");
        transfer.setUpdateTime(auditTime);

        ErpVoucherDO voucher = new ErpVoucherDO();
        voucher.setId(88L);
        voucher.setSourceBizType(18);
        voucher.setSourceBizId(100L);
        voucher.setVoucherNo("记-202607-000088");

        AdminUserRespDTO auditor = new AdminUserRespDTO();
        auditor.setId(7L);
        auditor.setNickname("审核员");
        AdminUserRespDTO handler = new AdminUserRespDTO();
        handler.setId(9L);
        handler.setNickname("经手人");
        Map<Long, AdminUserRespDTO> userMap = new HashMap<>();
        userMap.put(auditor.getId(), auditor);
        userMap.put(handler.getId(), handler);

        when(financeTransferService.getFinanceTransferPage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(transfer), 1L));
        when(adminUserApi.getUserMap(any())).thenReturn(userMap);
        when(voucherMapper.selectList(any())).thenReturn(Collections.singletonList(voucher));

        CommonResult<PageResult<ErpFinanceTransferRespVO>> result =
                controller.getFinanceTransferPage(new ErpFinanceTransferPageReqVO());

        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        ErpFinanceTransferRespVO row = result.getData().getList().get(0);
        assertEquals("经手人", row.getFinanceUserName());
        assertEquals("审核员", row.getAuditorName());
        assertEquals(auditTime, row.getAuditTime());
        assertEquals("记-202607-000088", row.getVoucherNo());
    }

    @Test
    void importFinanceTransfer_blankTransferTimeDefaultsToNow() throws Exception {
        when(financeTransferService.createFinanceTransfer(any())).thenReturn(1L);
        LocalDateTime before = LocalDateTime.now();

        CommonResult<ErpFinanceImportRespVO> result = controller.importFinanceTransfer(excelFile(
                new String[]{"转出账户ID", "转入账户ID", "转账金额", "汇率"},
                new Object[]{1L, 2L, new BigDecimal("100.00"), BigDecimal.ONE}));

        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<ErpFinanceTransferSaveReqVO> captor =
                ArgumentCaptor.forClass(ErpFinanceTransferSaveReqVO.class);
        verify(financeTransferService).createFinanceTransfer(captor.capture());
        assertEquals(1, result.getData().getSuccessCount());
        assertFalse(captor.getValue().getTransferTime().isBefore(before));
        assertFalse(captor.getValue().getTransferTime().isAfter(after));
    }

    private MockMultipartFile excelFile(String[] headers, Object[] values) throws Exception {
        try (Workbook workbook = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("数据");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            Row dataRow = sheet.createRow(1);
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                if (value instanceof Number) {
                    dataRow.createCell(i).setCellValue(((Number) value).doubleValue());
                } else if (value != null) {
                    dataRow.createCell(i).setCellValue(value.toString());
                }
            }
            workbook.write(out);
            return new MockMultipartFile("file", "finance-transfer.xls", "application/vnd.ms-excel",
                    out.toByteArray());
        }
    }

}
