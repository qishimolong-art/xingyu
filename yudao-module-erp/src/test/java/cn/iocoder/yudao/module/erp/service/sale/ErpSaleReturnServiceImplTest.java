package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleReturnModeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ErpSaleReturnServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleReturnServiceImpl saleReturnService;

    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpSaleOrderService saleOrderService;
    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private AdminUserApi adminUserApi;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(saleReturnService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260510000001";
            }
        });
    }

    @Test
    public void testCreateBySaleOut_exceedReturnableCount_throwException() {
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.BY_SALE_OUT.getMode());
        reqVO.setSourceOutId(10L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("6")).setSourceOutItemId(100L)));

        when(saleOutService.validateSaleOut(eq(10L))).thenReturn(new ErpSaleOutDO()
                .setId(10L).setNo("XSCK001").setCustomerId(20L));
        when(saleOutService.getSaleOutItemListByOutId(eq(10L))).thenReturn(Collections.singletonList(new ErpSaleOutItemDO()
                .setId(100L).setOutId(10L).setProductId(200L).setWarehouseId(300L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("5"))));
        assertThrows(ServiceException.class, () -> saleReturnService.createSaleReturn(reqVO));
        verify(saleReturnMapper, never()).insert(any(ErpSaleReturnDO.class));
    }

    @Test
    public void testCreateByStock_withoutSaleOrder_createReturn() {
        ErpSaleReturnSaveReqVO reqVO = buildBaseReq(ErpSaleReturnModeEnum.BY_STOCK.getMode());
        reqVO.setCustomerId(20L);
        reqVO.setItems(Collections.singletonList(buildItem(new BigDecimal("2"))));
        mockProduct();
        when(customerService.validateCustomer(eq(20L))).thenReturn(new ErpCustomerDO().setId(20L));
        when(accountService.validateAccount(eq(1L))).thenReturn(new ErpAccountDO());
        when(saleReturnMapper.selectByNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleReturnDO saleReturn = invocation.getArgument(0);
            saleReturn.setId(99L);
            return 1;
        }).when(saleReturnMapper).insert(any(ErpSaleReturnDO.class));

        saleReturnService.createSaleReturn(reqVO);

        verify(saleOrderService, never()).validateSaleOrder(anyLong());
        verify(saleReturnMapper).insert(argThat((ErpSaleReturnDO saleReturn) -> reqVO.getReturnMode().equals(saleReturn.getReturnMode())
                && reqVO.getCustomerId().equals(saleReturn.getCustomerId())
                && saleReturn.getOrderId() == null));
        verify(saleReturnItemMapper).insertBatch(argThat(items -> items.iterator().next().getOrderItemId() == null));
    }

    private ErpSaleReturnSaveReqVO buildBaseReq(Integer returnMode) {
        ErpSaleReturnSaveReqVO reqVO = new ErpSaleReturnSaveReqVO();
        reqVO.setReturnMode(returnMode);
        reqVO.setReturnTime(LocalDateTime.of(2026, 5, 10, 10, 0));
        reqVO.setAccountId(1L);
        reqVO.setDiscountPercent(BigDecimal.ZERO);
        reqVO.setOtherPrice(BigDecimal.ZERO);
        return reqVO;
    }

    private ErpSaleReturnSaveReqVO.Item buildItem(BigDecimal count) {
        ErpSaleReturnSaveReqVO.Item item = new ErpSaleReturnSaveReqVO.Item();
        item.setWarehouseId(300L);
        item.setProductId(200L);
        item.setProductUnitId(400L);
        item.setProductPrice(new BigDecimal("10"));
        item.setCount(count);
        item.setTaxPercent(BigDecimal.ZERO);
        return item;
    }

    private void mockProduct() {
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(new ErpProductDO()
                .setId(200L).setUnitId(400L)));
    }

}
