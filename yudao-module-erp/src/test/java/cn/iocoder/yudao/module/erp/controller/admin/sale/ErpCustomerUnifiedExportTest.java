package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseDataService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerBusinessInfoMapper;
import cn.iocoder.yudao.module.erp.service.common.ErpExportCaptchaService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerQueryService;
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

class ErpCustomerUnifiedExportTest extends BaseMockitoUnitTest {
    @InjectMocks private ErpCustomerController controller;
    @Mock private ErpCustomerService customerService;
    @Mock private DeptApi deptApi;
    @Mock private AdminUserApi adminUserApi;
    @Mock private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock private ErpCustomerBusinessInfoMapper customerBusinessInfoMapper;
    @Mock private ErpExportCaptchaService exportCaptchaService;
    @Mock private ErpImportExportRecordService importExportRecordService;
    @Mock private ErpBaseDataService baseDataService;
    @Mock private cn.iocoder.yudao.module.erp.dal.mysql.base.ErpBaseDataMapper baseDataMapper;
    @Mock private cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper saleOutMapper;

    @BeforeEach void setUpCustomerQueryService() {
        ErpCustomerQueryService queryService = new ErpCustomerQueryService();
        ReflectionTestUtils.setField(queryService, "customerService", customerService);
        ReflectionTestUtils.setField(queryService, "saleOutMapper", saleOutMapper);
        ReflectionTestUtils.setField(queryService, "customerBusinessInfoMapper", customerBusinessInfoMapper);
        ReflectionTestUtils.setField(queryService, "fieldPermissionMasker", fieldPermissionMasker);
        ReflectionTestUtils.setField(queryService, "baseDataMapper", baseDataMapper);
        ReflectionTestUtils.setField(queryService, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(queryService, "deptApi", deptApi);
        ReflectionTestUtils.setField(controller, "customerQueryService", queryService);
        lenient().when(customerService.getCustomerDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
    }

    @org.junit.jupiter.api.BeforeAll static void initOptionTable() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "customer-options-test"), ErpBaseDataDO.class);
    }

    @AfterEach void clearContext() { ExcelUtils.clearLastOperation(); }

    @Test void selectedExportUsesVisiblePagesIgnoresFiltersAndWrites501RowsInTwoBatches() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        when(customerService.getCustomerPage(any())).thenAnswer(invocation -> {
            ErpCustomerPageReqVO query = invocation.getArgument(0);
            assertEquals(500, query.getPageSize());
            assertEquals(calls.incrementAndGet(), query.getPageNo());
            assertEquals(Arrays.asList(1L, 2L), query.getIds());
            assertNull(query.getName());
            assertEquals("name", query.getOrderField());
            List<ErpCustomerDO> rows = IntStream.range(0, calls.get() == 1 ? 500 : 1)
                    .mapToObj(i -> new ErpCustomerDO().setId((long) i + 1).setName("Customer").setCode("00001234"))
                    .collect(Collectors.toList());
            return new PageResult<>(rows, 501L);
        });
        ErpCustomerPageReqVO req = new ErpCustomerPageReqVO();
        req.setIds(Arrays.asList(1L, 2L));
        req.setName("conflicting filter");
        req.setOrderField("name");
        req.setOrderDirection("asc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportCustomerExcel(req, "123456", null, "code,name", response);
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
        assertThrows(IllegalArgumentException.class, () -> controller.exportCustomerExcel(new ErpCustomerPageReqVO(), "bad", null, "code", new MockHttpServletResponse()));
        verifyNoInteractions(customerService);
    }

    @Test void unknownAndHiddenFieldsStopBeforeAnyDataQuery() {
        when(fieldPermissionMasker.getHiddenFieldSet("erp_customer")).thenReturn(Collections.singleton("bankAccount"));
        for (String fields : Arrays.asList("notAColumn", "bankAccount")) {
            assertThrows(Exception.class, () -> controller.exportCustomerExcel(new ErpCustomerPageReqVO(), "ok", null, fields, new MockHttpServletResponse()));
        }
        verifyNoInteractions(customerService);
    }

    @Test void errorDownloadUsesCustomerScopedOwnershipValidation() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadImportFailureDetails(17L, response);
        verify(importExportRecordService).downloadOwnImportFailureDetails(17L, "erp_customer", response);
    }

    @Test void defaultWorkbookKeepsLegacyFieldsAndEmptyResultStillHasHeaders() throws Exception {
        when(customerService.getCustomerPage(any())).thenReturn(PageResult.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportCustomerExcel(new ErpCustomerPageReqVO(), "ok", null, null, response);
        long count = Arrays.stream(ErpCustomerRespVO.class.getDeclaredFields()).filter(field -> field.isAnnotationPresent(cn.idev.excel.annotation.ExcelProperty.class)).count();
        try (org.apache.poi.ss.usermodel.Workbook book = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals(count, book.getSheetAt(0).getRow(0).getLastCellNum());
            assertEquals(0, book.getSheetAt(0).getLastRowNum());
        }
        assertEquals(0, ExcelUtils.getLastOperation().getDataCount());
    }

    @Test void baseOptionsKeepEnabledScopeAndBoundedNameSearchWithoutBaseQueryPermission() throws Exception {
        when(baseDataMapper.selectPage(any(PageParam.class), any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenAnswer(invocation -> {
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
        String access = ErpCustomerController.class.getMethod("getBaseOptionsPage", PageParam.class, String.class)
                .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value();
        assertTrue(access.contains("erp:customer:create"));
        assertFalse(access.contains("erp:base-data:query"));
    }
}
