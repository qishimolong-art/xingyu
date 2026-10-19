package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleItemMatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnItemPageReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.constraints.Max;
import javax.validation.constraints.Size;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ErpSaleItemPagingContractTest {

    @Test
    void itemPageKeepsGetAndAddsPermissionCompatiblePostAndMatch() throws Exception {
        assertControllerContract(ErpSaleQuoteController.class, ErpSaleQuoteItemPageReqVO.class,
                "getSaleQuoteItemPage", "getSaleQuoteEditItemPage", "matchSaleQuoteItems",
                "erp:sale-quote:query");
        assertControllerContract(ErpSaleOrderController.class, ErpSaleOrderItemPageReqVO.class,
                "getSaleOrderItemPage", "getSaleOrderEditItemPage", "matchSaleOrderItems",
                "erp:sale-order:query");
        assertControllerContract(ErpSaleReturnController.class, ErpSaleReturnItemPageReqVO.class,
                "getSaleReturnItemPage", "getSaleReturnEditItemPage", "matchSaleReturnItems",
                "erp:sale-return:query");
        assertControllerContract(ErpSalePriceAdjustController.class, ErpSalePriceAdjustItemPageReqVO.class,
                "getSalePriceAdjustItemPage", "getSalePriceAdjustEditItemPage", "matchSalePriceAdjustItems",
                "erp:sale-price-adjust:query");
    }

    @Test
    void requestContractsExposeDeleteExclusionsAndBoundMatchBatchSize() throws Exception {
        for (Class<?> pageReqClass : List.of(ErpSaleQuoteItemPageReqVO.class, ErpSaleOrderItemPageReqVO.class,
                ErpSaleReturnItemPageReqVO.class, ErpSalePriceAdjustItemPageReqVO.class)) {
            assertEquals(List.class, pageReqClass.getDeclaredField("excludedItemIds").getType());
        }
        Field keys = ErpSaleItemMatchReqVO.class.getDeclaredField("keys");
        assertEquals(200, keys.getAnnotation(Size.class).max());
        Field pageSize = ErpSaleItemMatchReqVO.class.getDeclaredField("pageSize");
        assertEquals(200, pageSize.getAnnotation(Max.class).value());
    }

    private static void assertControllerContract(Class<?> controllerClass, Class<?> pageReqClass,
            String getMethodName, String postMethodName, String matchMethodName, String permission) throws Exception {
        Method getMethod = controllerClass.getMethod(getMethodName, pageReqClass);
        Method postMethod = controllerClass.getMethod(postMethodName, pageReqClass);
        Method matchMethod = controllerClass.getMethod(matchMethodName, ErpSaleItemMatchReqVO.class);
        assertArrayEquals(new String[]{"/item-page"}, getMethod.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[]{"/item-page"}, postMethod.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/item-match"}, matchMethod.getAnnotation(PostMapping.class).value());
        assertPermission(postMethod, permission);
        assertPermission(matchMethod, permission);
    }

    private static void assertPermission(Method method, String permission) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('" + permission + "')", preAuthorize.value());
    }

}
