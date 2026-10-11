package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryOperationItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliveryItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSaleDeliveryOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSaleDeliveryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySubmitRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryPickTaskDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliverySubmitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliverySubmitItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliveryItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliveryOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliveryPickTaskMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliverySubmitFileMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliverySubmitMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliverySubmitItemMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpSalePickDeliveryServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSalePickDeliveryServiceImpl service;

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private ErpSalePickDeliverySubmitMapper submitMapper;
    @Mock
    private ErpSalePickDeliveryItemMapper itemMapper;
    @Mock
    private ErpSalePickDeliveryOrderMapper orderMapper;
    @Mock
    private ErpSalePickDeliverySubmitItemMapper submitItemMapper;
    @Mock
    private ErpSalePickDeliveryPickTaskMapper pickTaskMapper;
    @Mock
    private ErpSalePickDeliverySubmitFileMapper submitFileMapper;

    @Test
    void deliveryNames_shouldKeepEarlierBatchUsersAndAppendLegacyUsers() {
        when(submitMapper.selectDeliveryUsersBySaleOutId(100L)).thenReturn(List.of(
                new ErpSalePickDeliverySubmitDO().setSubmitUserId(1L),
                new ErpSalePickDeliverySubmitDO().setSubmitUserId(2L)));
        // 明细只剩最后一次操作人，提交记录中的第一批人员仍应保留。
        when(itemMapper.selectDeliveryUsersBySaleOutId(100L)).thenReturn(List.of(
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(2L),
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(3L),
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(4L),
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(5L),
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(6L)));
        when(adminUserApi.getUserMap(Set.of(1L, 2L, 3L, 4L, 5L, 6L))).thenReturn(Map.of(
                1L, deliveryUser(1L, "  张三 "), 2L, deliveryUser(2L, "李四"),
                3L, deliveryUser(3L, "王五"), 4L, deliveryUser(4L, "张三"),
                5L, deliveryUser(5L, "  ")));
        assertEquals("张三、李四、王五", service.getSaleOutDeliveryUserNames(100L));
        verify(submitMapper).selectDeliveryUsersBySaleOutId(100L);
        verify(itemMapper).selectDeliveryUsersBySaleOutId(100L);
        verifyNoMoreInteractions(submitMapper, itemMapper);
    }

    @Test
    void deliveryNames_shouldSupportLegacyOnlyAndSkipMissingUsers() {
        when(itemMapper.selectDeliveryUsersBySaleOutId(101L)).thenReturn(List.of(
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(3L)));
        when(adminUserApi.getUserMap(Set.of(3L))).thenReturn(Map.of(3L, deliveryUser(3L, "王五")));
        assertEquals("王五", service.getSaleOutDeliveryUserNames(101L));
        when(adminUserApi.getUserMap(Set.of(3L))).thenReturn(Map.of());
        assertEquals("", service.getSaleOutDeliveryUserNames(101L));
    }

    @Test
    void deliveryNames_shouldNotReadOtherOrdersOrResolveUsersWhenEmpty() {
        assertEquals("", service.getSaleOutDeliveryUserNames(null));
        verifyNoInteractions(submitMapper, itemMapper, adminUserApi);
        assertEquals("", service.getSaleOutDeliveryUserNames(102L));
        verify(submitMapper).selectDeliveryUsersBySaleOutId(102L);
        verify(itemMapper).selectDeliveryUsersBySaleOutId(102L);
        verifyNoInteractions(adminUserApi);
    }

    private AdminUserRespDTO deliveryUser(Long id, String name) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname(name);
        return user;
    }

    @Test
    void buildItemRespList_shouldKeepPositionPackageAndCalculatePieceCount() {
        List<ErpSalePickDeliveryItemDO> items = Arrays.asList(
                new ErpSalePickDeliveryItemDO()
                        .setId(1L)
                        .setWarehousePosition("A-01")
                        .setPackageQty(4)
                        .setCount(new BigDecimal("10"))
                        .setPickedCount(new BigDecimal("6"))
                        .setDeliveredCount(new BigDecimal("4")),
                new ErpSalePickDeliveryItemDO()
                        .setId(2L)
                        .setWarehousePosition("B-02")
                        .setPackageQty(0)
                        .setCount(new BigDecimal("5")));

        List<ErpSalePickDeliveryItemRespVO> respList = ReflectionTestUtils.invokeMethod(
                service, "buildItemRespList", items);

        assertEquals("A-01", respList.get(0).getWarehousePosition());
        assertEquals(4, respList.get(0).getPackageQty());
        assertEquals(0, new BigDecimal("2.5").compareTo(respList.get(0).getPieceCount()));
        assertEquals(0, new BigDecimal("2").compareTo(respList.get(0).getAvailableDeliveryCount()));
        assertEquals(0, new BigDecimal("6").compareTo(respList.get(0).getRemainingDeliveryCount()));
        assertEquals("B-02", respList.get(1).getWarehousePosition());
        assertEquals(0, respList.get(1).getPackageQty());
        assertNull(respList.get(1).getPieceCount());
    }

    @Test
    void calculateTotalPieceCount_shouldSkipBlankPackageQty() {
        List<ErpSalePickDeliveryItemDO> items = Arrays.asList(
                new ErpSalePickDeliveryItemDO().setCount(new BigDecimal("10")).setPackageQty(4),
                new ErpSalePickDeliveryItemDO().setCount(new BigDecimal("3")).setPackageQty(2),
                new ErpSalePickDeliveryItemDO().setCount(new BigDecimal("9")).setPackageQty(null));

        BigDecimal totalPieceCount = ReflectionTestUtils.invokeMethod(service, "calculateTotalPieceCount", items);

        assertEquals(0, new BigDecimal("4").compareTo(totalPieceCount));
    }

    @Test
    void deliveryPage_shouldSplitSameOrderByWarehouseAndKeepTaskProgressIndependent() {
        ErpSaleDeliveryPageReqVO reqVO = new ErpSaleDeliveryPageReqVO();
        ErpSalePickDeliveryPickTaskDO firstTask = new ErpSalePickDeliveryPickTaskDO()
                .setId(21L).setOrderId(9L).setSaleOutNo("XS-001").setWarehouseId(101L)
                .setWarehouseName("一仓").setStatus(20).setTotalItemCount(2);
        ErpSalePickDeliveryPickTaskDO secondTask = new ErpSalePickDeliveryPickTaskDO()
                .setId(22L).setOrderId(9L).setSaleOutNo("XS-001").setWarehouseId(102L)
                .setWarehouseName("二仓").setStatus(10).setTotalItemCount(1);
        when(pickTaskMapper.selectDeliveryPage(reqVO)).thenReturn(
                new PageResult<>(List.of(firstTask, secondTask), 2L));
        when(orderMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                new ErpSalePickDeliveryOrderDO().setId(9L).setDeptId(7L)));
        when(itemMapper.selectListByPickTaskIds(anyCollection())).thenReturn(List.of(
                new ErpSalePickDeliveryItemDO().setId(1L).setPickTaskId(21L).setCount(BigDecimal.TEN)
                        .setPickedCount(new BigDecimal("6")).setDeliveredCount(new BigDecimal("4"))
                        .setPickStatus(20).setDeliveryStatus(20),
                new ErpSalePickDeliveryItemDO().setId(2L).setPickTaskId(21L).setCount(BigDecimal.TEN)
                        .setPickedCount(BigDecimal.ZERO).setDeliveredCount(BigDecimal.ZERO)
                        .setPickStatus(10).setDeliveryStatus(5),
                new ErpSalePickDeliveryItemDO().setId(3L).setPickTaskId(22L).setCount(BigDecimal.TEN)
                        .setPickedCount(BigDecimal.ZERO).setDeliveredCount(BigDecimal.ZERO)
                        .setPickStatus(10).setDeliveryStatus(5)));

        PageResult<ErpSaleDeliveryOrderRespVO> page = service.getDeliveryPage(reqVO, false);

        assertEquals(2L, page.getTotal());
        assertEquals(21L, page.getList().get(0).getId());
        assertEquals(9L, page.getList().get(0).getOrderId());
        assertEquals("一仓", page.getList().get(0).getWarehouseName());
        assertEquals("0/2", page.getList().get(0).getDeliveryProgress());
        assertEquals(20, page.getList().get(0).getDeliveryStatus());
        assertEquals(1, page.getList().get(0).getDeliverableItemCount());
        assertEquals(22L, page.getList().get(1).getId());
        assertEquals("二仓", page.getList().get(1).getWarehouseName());
        assertEquals(5, page.getList().get(1).getDeliveryStatus());
    }

    @Test
    void deliverySubmitPage_shouldReusePreciseLegacyBatchButCountOnlyCurrentWarehouseItems() {
        ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO().setId(21L).setOrderId(9L);
        ErpSalePickDeliverySubmitDO legacy = new ErpSalePickDeliverySubmitDO().setId(31L).setOrderId(9L)
                .setPickTaskId(null).setType(20).setSubmitUserId(1L).setItemCount(2).setQuantityDetails(true);
        when(pickTaskMapper.selectById(21L)).thenReturn(task);
        when(submitMapper.selectDeliveryPageByTaskId(any(PageParam.class), eq(21L), eq(9L)))
                .thenReturn(new PageResult<>(List.of(legacy), 1L));
        when(submitFileMapper.selectListBySubmitIds(anyCollection())).thenReturn(List.of());
        when(adminUserApi.getUserMap(Set.of(1L))).thenReturn(Map.of(1L, deliveryUser(1L, "甲")));
        when(submitItemMapper.selectCountBySubmitIdAndPickTaskId(31L, 21L)).thenReturn(1L);

        PageResult<ErpSalePickDeliverySubmitRespVO> page = service.getDeliverySubmitPage(21L, new PageParam(), false);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().get(0).getItemCount());
        assertEquals("甲", page.getList().get(0).getSubmitUserName());
    }

    @Test
    void deliveryDetail_shouldWarnWhenLegacyBatchCannotBeAssignedToWarehouse() {
        ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO()
                .setId(21L).setOrderId(9L).setWarehouseId(101L).setWarehouseName("一仓")
                .setStatus(10).setTotalItemCount(1);
        when(pickTaskMapper.selectById(21L)).thenReturn(task);
        when(orderMapper.selectById(9L)).thenReturn(new ErpSalePickDeliveryOrderDO().setId(9L));
        when(itemMapper.selectListByPickTaskId(21L)).thenReturn(List.of(
                new ErpSalePickDeliveryItemDO().setId(1L).setPickTaskId(21L).setCount(BigDecimal.TEN)
                        .setPickedCount(BigDecimal.ZERO).setDeliveredCount(BigDecimal.ZERO)
                        .setPickStatus(10).setDeliveryStatus(5)));
        when(submitMapper.selectUnassignedLegacyDeliveryCount(9L)).thenReturn(1L);

        ErpSaleDeliveryOrderRespVO detail = service.getDelivery(21L, false, false);

        assertTrue(detail.getLegacyHistoryWarning());
        assertEquals("一仓", detail.getWarehouseName());
        verify(submitMapper, never()).selectDeliveryPageByTaskId(any(), anyLong(), anyLong());
    }

    @Test
    void operationItemPage_shouldKeepEachBatchRowAndIgnoreLegacySubmits() {
        LocalDateTime firstTime = LocalDateTime.of(2026, 10, 9, 10, 0);
        LocalDateTime secondTime = LocalDateTime.of(2026, 10, 9, 11, 0);
        when(orderMapper.selectBySaleOutId(100L)).thenReturn(new ErpSalePickDeliveryOrderDO().setId(9L));
        when(submitMapper.selectListBySaleOutIdAndType(100L, 10)).thenReturn(List.of(
                new ErpSalePickDeliverySubmitDO().setId(12L).setType(10).setSubmitUserId(1L)
                        .setSubmitTime(secondTime).setQuantityDetails(true),
                new ErpSalePickDeliverySubmitDO().setId(11L).setType(10).setSubmitUserId(1L)
                        .setSubmitTime(firstTime).setQuantityDetails(true),
                new ErpSalePickDeliverySubmitDO().setId(10L).setType(10).setSubmitUserId(2L)
                        .setQuantityDetails(false)));
        when(submitItemMapper.selectPageBySubmitIds(any(PageParam.class), argThat(ids ->
                ids.size() == 2 && ids.containsAll(Set.of(11L, 12L))))).thenReturn(new PageResult<>(List.of(
                new ErpSalePickDeliverySubmitItemDO().setId(102L).setSubmitId(12L).setItemId(2L)
                        .setProductCode("P-2").setProductName("商品二").setQuantity(new BigDecimal("4")),
                new ErpSalePickDeliverySubmitItemDO().setId(101L).setSubmitId(11L).setItemId(1L)
                        .setProductCode("P-1").setProductName("商品一").setQuantity(new BigDecimal("6"))), 2L));
        when(itemMapper.selectBatchIds(argThat(ids -> ids.size() == 2 && ids.containsAll(Set.of(1L, 2L)))))
                .thenReturn(List.of(
                        new ErpSalePickDeliveryItemDO().setId(1L).setStandard("规格一")
                                .setWarehouseId(21L).setWarehouseName("一仓"),
                        new ErpSalePickDeliveryItemDO().setId(2L).setStandard("规格二")
                                .setWarehouseId(22L).setWarehouseName("二仓")));
        when(adminUserApi.getUserMap(Set.of(1L))).thenReturn(Map.of(1L, deliveryUser(1L, "甲")));

        PageResult<ErpSaleOutPickDeliveryOperationItemRespVO> page =
                service.getSaleOutPickDeliveryOperationItemPage(100L, 10, new PageParam());

        assertEquals(2L, page.getTotal());
        assertEquals(2, page.getList().size());
        assertEquals(12L, page.getList().get(0).getSubmitId());
        assertEquals("甲", page.getList().get(0).getSubmitUserName());
        assertEquals("规格二", page.getList().get(0).getStandard());
        assertEquals("二仓", page.getList().get(0).getWarehouseName());
        assertEquals(0, new BigDecimal("4").compareTo(page.getList().get(0).getQuantity()));
        assertEquals(11L, page.getList().get(1).getSubmitId());
        assertEquals(firstTime, page.getList().get(1).getSubmitTime());
    }

    @Test
    void operationItemPage_shouldRejectUnsupportedTypeWithoutQuerying() {
        PageResult<ErpSaleOutPickDeliveryOperationItemRespVO> page =
                service.getSaleOutPickDeliveryOperationItemPage(100L, 30, new PageParam());

        assertTrue(page.getList().isEmpty());
        assertEquals(0L, page.getTotal());
        verifyNoInteractions(orderMapper, submitItemMapper);
    }

    @Test
    void sourceContract_shouldCopySnapshotFieldsAndOrderByWarehousePosition() throws IOException {
        String serviceSource = readProjectFile(
                "src/main/java/cn/iocoder/yudao/module/erp/service/sale/ErpSalePickDeliveryServiceImpl.java");
        String mapperSource = readProjectFile(
                "src/main/java/cn/iocoder/yudao/module/erp/dal/mysql/sale/pickdelivery/ErpSalePickDeliveryItemMapper.java");

        assertTrue(serviceSource.contains(".setWarehousePosition(saleOutItem.getWarehousePosition())"));
        assertTrue(serviceSource.contains(".setPackageQty(saleOutItem.getPackageQty())"));
        assertTrue(serviceSource.contains("submitMapper.selectListByOrderId(order.getId()).forEach"));
        assertTrue(mapperSource.contains("warehouse_position IS NULL OR warehouse_position = ''"));
        assertTrue(mapperSource.contains("orderByAsc(\"warehouse_position\")"));
        assertTrue(mapperSource.contains("orderByAsc(\"id\")"));
    }

    private String readProjectFile(String modulePath) throws IOException {
        Path path = Path.of(modulePath);
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        return Files.readString(Path.of("code/yudao-module-erp").resolve(modulePath));
    }

}
