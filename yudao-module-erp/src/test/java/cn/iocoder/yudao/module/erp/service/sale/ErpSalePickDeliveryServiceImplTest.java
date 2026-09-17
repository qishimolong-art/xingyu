package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliveryItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryItemDO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpSalePickDeliveryServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSalePickDeliveryServiceImpl service;

    @Mock
    private AdminUserApi adminUserApi;

    @Test
    void buildItemRespList_shouldKeepPositionPackageAndCalculatePieceCount() {
        List<ErpSalePickDeliveryItemDO> items = Arrays.asList(
                new ErpSalePickDeliveryItemDO()
                        .setId(1L)
                        .setWarehousePosition("A-01")
                        .setPackageQty(4)
                        .setCount(new BigDecimal("10")),
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
    void sourceContract_shouldCopySnapshotFieldsAndOrderByWarehousePosition() throws IOException {
        String serviceSource = readProjectFile(
                "src/main/java/cn/iocoder/yudao/module/erp/service/sale/ErpSalePickDeliveryServiceImpl.java");
        String mapperSource = readProjectFile(
                "src/main/java/cn/iocoder/yudao/module/erp/dal/mysql/sale/pickdelivery/ErpSalePickDeliveryItemMapper.java");

        assertTrue(serviceSource.contains(".setWarehousePosition(saleOutItem.getWarehousePosition())"));
        assertTrue(serviceSource.contains(".setPackageQty(saleOutItem.getPackageQty())"));
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
