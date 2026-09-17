package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPickupReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInBillItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInBillMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInBillPickupRecordMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_BILL_PICKUP_COUNT_EXCEED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpStockInBillServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockInBillServiceImpl stockInBillService;

    @Mock
    private ErpStockInBillMapper stockInBillMapper;
    @Mock
    private ErpStockInBillItemMapper stockInBillItemMapper;
    @Mock
    private ErpStockInBillPickupRecordMapper pickupRecordMapper;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private AdminUserApi adminUserApi;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(stockInBillService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260629000001";
            }
        });
    }

    @Test
    public void testCreateFromPurchaseIn_cancelled_rejectsWithoutWriting() {
        cn.iocoder.yudao.framework.common.exception.ServiceException error = org.junit.jupiter.api.Assertions.assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> stockInBillService.createFromPurchaseIn(new ErpPurchaseInDO().setId(10L),
                        Collections.singletonList(new ErpPurchaseInItemDO().setId(100L))));
        assertEquals(409, error.getCode());
        org.mockito.Mockito.verifyNoInteractions(stockInBillMapper, stockInBillItemMapper, stockRecordService);
    }

    @Test
    public void testPickup_cancelled_rejectsWithoutLookingUpOrWritingHistory() {
        ErpStockInBillPickupReqVO request = new ErpStockInBillPickupReqVO();
        request.setId(1L);
        cn.iocoder.yudao.framework.common.exception.ServiceException error = org.junit.jupiter.api.Assertions.assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> stockInBillService.pickup(request));
        assertEquals(409, error.getCode());
        org.mockito.Mockito.verifyNoInteractions(stockInBillMapper, stockInBillItemMapper, pickupRecordMapper, stockRecordService);
    }

    @Test
    public void testPickup_cancelled_malformedRequestCannotBypassGuard() {
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> stockInBillService.pickup(null));
        org.mockito.Mockito.verifyNoInteractions(stockInBillMapper, stockInBillItemMapper, pickupRecordMapper, stockRecordService);
    }
}