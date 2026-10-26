package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseDataService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierBusinessInfoMapper;
import cn.iocoder.yudao.module.erp.service.common.ErpExportCaptchaService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierQueryService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ErpSupplierUnifiedExportTest extends BaseMockitoUnitTest {
    @InjectMocks private ErpSupplierController controller;
    @Mock private ErpSupplierService supplierService;
    @Mock private DeptApi deptApi;
    @Mock private AdminUserApi adminUserApi;
    @Mock private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Mock private ErpSupplierBusinessInfoMapper supplierBusinessInfoMapper;
    @Mock private ErpExportCaptchaService exportCaptchaService;
    @Mock private ErpImportExportRecordService importExportRecordService;
    @Mock private ErpBaseDataService baseDataService;
    @Mock private ErpFinanceFieldPermissionMasker baseDataFieldPermissionMasker;

    @BeforeEach void setUpSupplierQueryService() {
        ErpSupplierQueryService queryService = new ErpSupplierQueryService();
        ReflectionTestUtils.setField(queryService, "supplierService", supplierService);
        ReflectionTestUtils.setField(queryService, "deptApi", deptApi);
        ReflectionTestUtils.setField(queryService, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(controller, "supplierQueryService", queryService);
        lenient().when(supplierService.getSupplierDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
        lenient().when(adminUserApi.getUserMap(anyCollection())).thenReturn(Collections.emptyMap());
        lenient().when(deptApi.getDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
    }

    @AfterEach void clearContext() { ExcelUtils.clearLastOperation(); }

    @Test void selectedExportUsesVisiblePagesIgnoresFiltersAndWrites501RowsInTwoBatches() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        when(supplierService.getSupplierPage(any())).thenAnswer(invocation -> {
            ErpSupplierPageReqVO query = invocation.getArgument(0);
            assertEquals(500, query.getPageSize());
            assertEquals(calls.incrementAndGet(), query.getPageNo());
            assertEquals(Arrays.asList(1L, 2L), query.getIds());
            assertNull(query.getName());
            assertEquals("name", query.getOrderField());
            List<ErpSupplierDO> rows = IntStream.range(0, calls.get() == 1 ? 500 : 1)
                    .mapToObj(i -> new ErpSupplierDO().setId((long) i + 1).setName("Supplier").setCode("00001234"))
                    .collect(Collectors.toList());
            return new PageResult<>(rows, 501L);
        });
        ErpSupplierPageReqVO req = new ErpSupplierPageReqVO();
        req.setIds(Arrays.asList(1L, 2L));
        req.setName("conflicting filter");
        req.setOrderField("name");
        req.setOrderDirection("asc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportSupplierExcel(req, "code,name", "123456", null, response);
        verify(exportCaptchaService).validate("123456", null);
        assertEquals(2, calls.get());
        try (org.apache.poi.ss.usermodel.Workbook book = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals(501, book.getSheetAt(0).getLastRowNum());
            assertEquals(2, book.getSheetAt(0).getRow(0).getLastCellNum());
            boolean codeFound = false;
            for (org.apache.poi.ss.usermodel.Cell cell : book.getSheetAt(0).getRow(1)) {
                if ("00001234".equals(cell.getStringCellValue())) { codeFound = true; assertEquals(CellType.STRING, cell.getCellType()); }
            }
            assertTrue(codeFound);
        }
        assertEquals(501, ExcelUtils.getLastOperation().getDataCount());
        assertTrue(response.getHeader("Content-Disposition").contains(".xlsx"));
    }

    @Test void invalidCaptchaStopsBeforeAnyDataQuery() {
        doThrow(new IllegalArgumentException("captcha")).when(exportCaptchaService).validate("bad", null);
        assertThrows(IllegalArgumentException.class, () -> controller.exportSupplierExcel(new ErpSupplierPageReqVO(), "code", "bad", null, new MockHttpServletResponse()));
        verifyNoInteractions(supplierService);
    }

    @Test void unknownAndHiddenFieldsStopBeforeAnyDataQuery() {
        when(fieldPermissionMasker.getHiddenFieldSet("erp_supplier")).thenReturn(Collections.singleton("bankAccount"));
        for (String fields : Arrays.asList("notAColumn", "bankAccount")) {
            assertThrows(Exception.class, () -> controller.exportSupplierExcel(new ErpSupplierPageReqVO(), fields, "ok", null, new MockHttpServletResponse()));
        }
        verifyNoInteractions(supplierService);
    }

    @Test void errorDownloadUsesSupplierScopedOwnershipValidation() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadImportFailureDetails(17L, response);
        verify(importExportRecordService).downloadOwnImportFailureDetails(17L, "erp_supplier", response);
    }

    @Test void defaultWorkbookKeepsLegacyFieldsAndEmptyResultStillHasHeaders() throws Exception {
        when(supplierService.getSupplierPage(any())).thenReturn(PageResult.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportSupplierExcel(new ErpSupplierPageReqVO(), null, "ok", null, response);
        long count = Arrays.stream(ErpSupplierRespVO.class.getDeclaredFields()).filter(field -> field.isAnnotationPresent(cn.idev.excel.annotation.ExcelProperty.class)).count();
        try (org.apache.poi.ss.usermodel.Workbook book = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals(count, book.getSheetAt(0).getRow(0).getLastCellNum());
            assertEquals(0, book.getSheetAt(0).getLastRowNum());
        }
        assertEquals(0, ExcelUtils.getLastOperation().getDataCount());
    }

    @Test void baseOptionsKeepEnabledScopeAndBoundedNameSearchWithoutBaseQueryPermission() throws Exception {
        when(baseDataService.getBaseDataPage(any())).thenAnswer(invocation -> {
            ErpBaseDataPageReqVO query = invocation.getArgument(0);
            assertEquals("settle_method", query.getType());
            assertEquals(0, query.getStatus());
            assertEquals("cash", query.getName());
            assertNull(query.getKeyword());
            assertEquals(100, query.getPageSize());
            return new PageResult<>(Collections.singletonList(new ErpBaseDataDO().setId(1L).setName("cash").setCode("cash")), 1L);
        });
        PageParam req = new PageParam();
        req.setPageSize(-1);
        req.setKeyword("cash");
        assertEquals("cash", controller.getBaseOptionsPage(req, "settle_method").getData().getList().get(0).getCode());
        String access = ErpSupplierController.class.getMethod("getBaseOptionsPage", PageParam.class, String.class)
                .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value();
        assertTrue(access.contains("erp:supplier:create"));
        assertFalse(access.contains("erp:base-data:query"));
    }
}
