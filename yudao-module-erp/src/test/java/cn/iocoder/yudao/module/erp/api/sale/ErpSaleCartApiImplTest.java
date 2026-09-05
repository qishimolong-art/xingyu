package cn.iocoder.yudao.module.erp.api.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpSaleCartDraftCreateReqDTO;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpSaleCartDraftCreateRespDTO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

class ErpSaleCartApiImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleCartApiImpl saleCartApi;

    @Mock
    private ErpSaleCartService saleCartService;

    @Test
    void createSaleCartDraft_shouldPreserveSourceAndReturnDraftMeta() {
        ErpSaleCartDraftCreateReqDTO reqDTO = new ErpSaleCartDraftCreateReqDTO()
                .setCustomerId(10L)
                .setDeptId(20L)
                .setSourceType(ErpSaleBizSourceTypeEnum.MALL_ORDER.getType())
                .setSourceId(30L)
                .setSourceNo("MO-001")
                .setRemark("remark")
                .setItems(Collections.singletonList(new ErpSaleCartDraftCreateReqDTO.Item()
                        .setProductId(40L)
                        .setWarehouseId(50L)
                        .setDeptId(20L)
                        .setCount(new BigDecimal("2"))
                        .setProductPrice(new BigDecimal("12.34"))));
        when(saleCartService.createSaleCartDraftFromSource(argThat(reqVO -> {
            assertEquals(10L, reqVO.getCustomerId());
            assertEquals(20L, reqVO.getDeptId());
            assertEquals(ErpSaleBizSourceTypeEnum.MALL_ORDER.getType(), reqVO.getSourceType());
            assertEquals(30L, reqVO.getSourceId());
            assertEquals("MO-001", reqVO.getSourceNo());
            assertEquals("remark", reqVO.getRemark());
            assertNull(reqVO.getSaleUserId());
            ErpSaleCartSaveReqVO.Item item = reqVO.getItems().get(0);
            assertEquals(40L, item.getProductId());
            assertEquals(50L, item.getWarehouseId());
            assertEquals(20L, item.getDeptId());
            assertEquals(new BigDecimal("2"), item.getCount());
            assertEquals(new BigDecimal("12.34"), item.getProductPrice());
            return true;
        }))).thenReturn(100L);
        when(saleCartService.getSaleCart(100L)).thenReturn(new ErpSaleCartDO()
                .setId(100L)
                .setNo("XSTC-001")
                .setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus()));

        ErpSaleCartDraftCreateRespDTO respDTO = saleCartApi.createSaleCartDraft(reqDTO);

        assertEquals(100L, respDTO.getId());
        assertEquals("XSTC-001", respDTO.getNo());
        assertEquals(ErpSaleCartStatusEnum.PROCESS.getStatus(), respDTO.getStatus());
    }

}
