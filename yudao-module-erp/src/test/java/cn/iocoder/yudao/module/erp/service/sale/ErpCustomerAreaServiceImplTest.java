package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerAreaDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerAreaMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_AREA_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpCustomerAreaServiceImpl} 的单元测试
 */
public class ErpCustomerAreaServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerAreaServiceImpl areaService;

    @Mock
    private ErpCustomerAreaMapper areaMapper;
    @Mock
    private ErpCustomerService customerService;

    // ==================== createArea ====================

    @Test
    public void testCreateArea_normalCase_returnId() {
        // 准备
        ErpCustomerAreaSaveReqVO reqVO = new ErpCustomerAreaSaveReqVO();
        reqVO.setCustomerId(10L);
        reqVO.setLongitude(new BigDecimal("116.397428"));
        reqVO.setLatitude(new BigDecimal("39.90923"));
        reqVO.setMapAddress("北京市朝阳区");
        reqVO.setDetailAddress("xx 大厦 8 层");
        reqVO.setDefaulted(true);

        // mock：insert 时回填 id
        when(areaMapper.insert(ArgumentMatchers.<ErpCustomerAreaDO>any())).thenAnswer(invocation -> {
            ErpCustomerAreaDO area = invocation.getArgument(0);
            area.setId(100L);
            return 1;
        });

        // 执行
        Long resultId = areaService.createArea(reqVO);

        // 断言
        assertNotNull(resultId);
        assertEquals(100L, resultId);
        verify(customerService).validateCustomer(eq(10L));
        verify(areaMapper).insert(ArgumentMatchers.<ErpCustomerAreaDO>argThat(area ->
                Long.valueOf(10L).equals(area.getCustomerId())
                        && new BigDecimal("116.397428").compareTo(area.getLongitude()) == 0
                        && new BigDecimal("39.90923").compareTo(area.getLatitude()) == 0
                        && "北京市朝阳区".equals(area.getMapAddress())
                        && Boolean.TRUE.equals(area.getDefaulted())));
    }

    @Test
    public void testCreateArea_invalidCustomer_throwException() {
        // 准备
        ErpCustomerAreaSaveReqVO reqVO = new ErpCustomerAreaSaveReqVO();
        reqVO.setCustomerId(11L);
        reqVO.setMapAddress("上海市浦东新区");
        // mock：客户校验失败
        doThrow(new ServiceException(CUSTOMER_NOT_EXISTS))
                .when(customerService).validateCustomer(eq(11L));

        // 执行 & 断言
        assertServiceException(() -> areaService.createArea(reqVO), CUSTOMER_NOT_EXISTS);
        // 校验：未 insert
        verify(areaMapper, never()).insert(ArgumentMatchers.<ErpCustomerAreaDO>any());
    }

    // ==================== updateArea ====================

    @Test
    public void testUpdateArea_normalCase_success() {
        Long areaId = 20L;
        ErpCustomerAreaDO existArea = new ErpCustomerAreaDO()
                .setId(areaId)
                .setCustomerId(20L)
                .setMapAddress("旧地址");
        when(areaMapper.selectById(eq(areaId))).thenReturn(existArea);

        ErpCustomerAreaSaveReqVO reqVO = new ErpCustomerAreaSaveReqVO();
        reqVO.setId(areaId);
        reqVO.setCustomerId(20L);
        reqVO.setMapAddress("新地址");
        reqVO.setDefaulted(false);

        // 执行
        areaService.updateArea(reqVO);

        // 断言
        verify(customerService).validateCustomer(eq(20L));
        verify(areaMapper).updateById(ArgumentMatchers.<ErpCustomerAreaDO>argThat(update ->
                areaId.equals(update.getId())
                        && "新地址".equals(update.getMapAddress())
                        && Boolean.FALSE.equals(update.getDefaulted())));
    }

    @Test
    public void testUpdateArea_notExists_throwException() {
        Long areaId = 21L;
        when(areaMapper.selectById(eq(areaId))).thenReturn(null);
        ErpCustomerAreaSaveReqVO reqVO = new ErpCustomerAreaSaveReqVO();
        reqVO.setId(areaId);
        reqVO.setCustomerId(21L);

        // 执行 & 断言
        assertServiceException(() -> areaService.updateArea(reqVO), CUSTOMER_AREA_NOT_EXISTS);
        // 校验：未发生 customer 校验和 update
        verify(customerService, never()).validateCustomer(any());
        verify(areaMapper, never()).updateById(ArgumentMatchers.<ErpCustomerAreaDO>any());
    }

    // ==================== deleteArea ====================

    @Test
    public void testDeleteArea_normalCase_success() {
        Long areaId = 30L;
        ErpCustomerAreaDO existArea = new ErpCustomerAreaDO()
                .setId(areaId)
                .setCustomerId(30L);
        when(areaMapper.selectById(eq(areaId))).thenReturn(existArea);

        // 执行
        areaService.deleteArea(areaId);

        // 断言
        verify(areaMapper).deleteById(eq(areaId));
    }

    // ==================== getAreaListByCustomerId ====================

    @Test
    public void testGetAreaListByCustomerId_returnList() {
        Long customerId = 40L;
        ErpCustomerAreaDO area1 = new ErpCustomerAreaDO().setId(401L).setCustomerId(customerId).setMapAddress("地区1").setDefaulted(true);
        ErpCustomerAreaDO area2 = new ErpCustomerAreaDO().setId(402L).setCustomerId(customerId).setMapAddress("地区2").setDefaulted(false);
        when(areaMapper.selectListByCustomerId(eq(customerId)))
                .thenReturn(Arrays.asList(area1, area2));

        // 执行
        List<ErpCustomerAreaDO> result = areaService.getAreaListByCustomerId(customerId);

        // 断言
        assertEquals(2, result.size());
        assertEquals(401L, result.get(0).getId());
        assertEquals(402L, result.get(1).getId());
        // 校验：先 validateCustomer 再查询
        verify(customerService).validateCustomer(eq(customerId));
        verify(areaMapper).selectListByCustomerId(eq(customerId));
    }

}
