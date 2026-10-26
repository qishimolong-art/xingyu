package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseQueryService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ErpWarehouseUnifiedControllerTest extends BaseMockitoUnitTest {
    @InjectMocks private ErpWarehouseController controller;
    @Mock private ErpWarehouseService warehouseService;
    @Mock private ErpWarehouseQueryService warehouseQueryService;
    @Mock private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock private PermissionApi permissionApi;
    @Mock private DeptApi deptApi;
    @Mock private AdminUserApi adminUserApi;

    @BeforeEach
    void mockWarehouseQueryAssembler() {
        lenient().when(warehouseQueryService.buildWarehouseVOList(anyList())).thenAnswer(invocation ->
                BeanUtils.toBean(invocation.<List<ErpWarehouseDO>>getArgument(0), ErpWarehouseRespVO.class));
    }

    @Test void exportSelectedUsesVisibleServiceAndPreservesNumericCells() throws Exception {
        ErpWarehousePageReqVO query = new ErpWarehousePageReqVO();
        query.setIds(Arrays.asList(1L, 999L));
        query.setName("不应带入勾选导出的筛选");
        query.setOrderField("name"); query.setOrderDirection("asc");
        when(warehouseService.getWarehousePage(any())).thenAnswer(invocation -> {
            ErpWarehousePageReqVO actual = invocation.getArgument(0);
            assertEquals(Arrays.asList(1L, 999L), actual.getIds());
            assertNull(actual.getName());
            assertEquals("name", actual.getOrderField());
            assertEquals(500, actual.getPageSize());
            // The visible service returns only the authorized row, never a raw get-list by requested ids.
            return new PageResult<>(Collections.singletonList(new ErpWarehouseDO().setId(1L).setName("可见仓库")
                    .setWarehouseCode("000123").setSort(7L).setLongitude(new BigDecimal("104.066801"))
                    .setWarehousePrice(new BigDecimal("12.30"))), 1L);
        });
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportWarehouseExcel(query, "name,sort,longitude,warehousePrice,warehouseCode", response);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Row head = workbook.getSheetAt(0).getRow(0), row = workbook.getSheetAt(0).getRow(1);
            assertEquals(5, head.getLastCellNum());
            for (int i = 0; i < head.getLastCellNum(); i++) {
                String label = head.getCell(i).getStringCellValue();
                if ("仓库编码".equals(label)) {
                    assertEquals(CellType.STRING, row.getCell(i).getCellType());
                    assertEquals("000123", row.getCell(i).getStringCellValue());
                } else if (!"仓库名称".equals(label)) {
                    assertEquals(CellType.NUMERIC, row.getCell(i).getCellType());
                    if (label.startsWith("仓储费")) assertTrue(
                            row.getCell(i).getCellStyle().getDataFormatString().startsWith("0.00"));
                    if ("经度".equals(label)) assertEquals(104.066801, row.getCell(i).getNumericCellValue(), 0.0000001);
                }
            }
        }
        verify(warehouseService, never()).getWarehouseList(query.getIds());
    }

    @Test void exportReadsBoundedBatchesAndKeepsLegacyDefaultColumns() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        when(warehouseService.getWarehousePage(any())).thenAnswer(invocation -> {
            ErpWarehousePageReqVO actual = invocation.getArgument(0);
            assertEquals(500, actual.getPageSize());
            assertEquals(calls.incrementAndGet(), actual.getPageNo());
            List<ErpWarehouseDO> rows = IntStream.range(0, actual.getPageNo() == 1 ? 500 : 1)
                    .mapToObj(i -> new ErpWarehouseDO().setId((long) i + 1).setName("仓库")).collect(Collectors.toList());
            return new PageResult<>(rows, 501L);
        });
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportWarehouseExcel(new ErpWarehousePageReqVO(), null, response);
        assertEquals(2, calls.get());
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals(501, workbook.getSheetAt(0).getLastRowNum());
            assertEquals(16, workbook.getSheetAt(0).getRow(0).getLastCellNum());
            assertEquals("仓库编号", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test void hiddenAndUnknownFieldsCannotBeExportedOrListed() throws Exception {
        when(permissionApi.getCurrentUserHiddenFields("erp_warehouse")).thenReturn(Arrays.asList("col_deptId", "creator", "warehousePrice"));
        assertFalse(controller.getWarehouseExportFields().getData().stream().anyMatch(field ->
                Arrays.asList("deptId", "deptName", "creatorName", "warehousePrice").contains(field.getField())));
        assertThrows(RuntimeException.class, () -> controller.exportWarehouseExcel(new ErpWarehousePageReqVO(),
                "deptName,creatorName,warehousePrice,notAField", new MockHttpServletResponse()));
        verify(warehouseService, never()).getWarehousePage(any());
    }

    @Test void personnelPageDoesNotLoadFullUserListAndSelectedIncludesDisabledUsers() {
        PageParam page = new PageParam(); page.setPageSize(20); page.setKeyword("张");
        when(adminUserApi.getUserSimplePage(0, "张", page)).thenReturn(PageResult.empty());
        controller.getWarehouseAssignableUsersPage(page);
        verify(adminUserApi).getUserSimplePage(0, "张", page);
        verify(adminUserApi, never()).getUserListByStatus(any());
        when(warehouseService.getWarehouseUserIds(1L)).thenReturn(Arrays.asList(10L, 20L));
        when(adminUserApi.getUserList(Arrays.asList(10L, 20L))).thenReturn(Arrays.asList(
                new AdminUserRespDTO().setId(10L).setNickname("正常账号").setStatus(0),
                new AdminUserRespDTO().setId(20L).setNickname("历史停用").setStatus(1)));
        assertEquals(2, controller.getWarehouseUserPermissions(1L).getData().getSelectedUsers().size());
        assertEquals(Arrays.asList(10L, 20L), controller.getWarehouseUserPermissions(1L).getData().getUserIds());
    }

    @Test void formDeptPagePreservesEnabledCandidateRange() {
        PageParam page = new PageParam();
        controller.getFormDeptSimplePage(page, null);
        verify(deptApi).getDeptSimplePage(0, null, null, page);
        controller.getFormDeptSimplePage(page, 123L);
        verify(deptApi).getDeptSimplePage(0, null, Collections.singleton(123L), page);
    }
}
