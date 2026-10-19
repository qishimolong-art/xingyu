package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockItemMatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveItemPageReqVO;
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

class ErpStockItemPagingContractTest {

    @Test
    void editableDocumentsKeepGetAndAddPermissionCompatiblePostAndMatch() throws Exception {
        assertControllerContract(ErpStockInController.class, ErpStockInItemPageReqVO.class,
                "getStockInItemPage", "getStockInEditItemPage", "matchStockInItems", "erp:stock-in:query");
        assertControllerContract(ErpStockOutController.class, ErpStockOutItemPageReqVO.class,
                "getStockOutItemPage", "getStockOutEditItemPage", "matchStockOutItems", "erp:stock-out:query");
        assertControllerContract(ErpStockMoveController.class, ErpStockMoveItemPageReqVO.class,
                "getStockMoveItemPage", "getStockMoveEditItemPage", "matchStockMoveItems", "erp:stock-move:query");
        assertControllerContract(ErpStockTransferOutController.class, ErpStockMoveItemPageReqVO.class,
                "getStockTransferOutItemPage", "getStockTransferOutEditItemPage", "matchStockTransferOutItems",
                "erp:stock-transfer-out:query");
        assertControllerContract(ErpStockCheckController.class, ErpStockCheckItemPageReqVO.class,
                "getStockCheckItemPage", "getStockCheckEditItemPage", "matchStockCheckItems", "erp:stock-check:query");
        assertControllerContract(ErpWarehouseMoveController.class, ErpWarehouseMoveItemPageReqVO.class,
                "getWarehouseMoveItemPage", "getWarehouseMoveEditItemPage", "matchWarehouseMoveItems",
                "erp:warehouse-move:query");
    }

    @Test
    void requestsExposeDeleteExclusionsAndBoundMatchBatchSize() throws Exception {
        for (Class<?> pageReqClass : List.of(ErpStockInItemPageReqVO.class, ErpStockOutItemPageReqVO.class,
                ErpStockMoveItemPageReqVO.class, ErpStockCheckItemPageReqVO.class,
                ErpWarehouseMoveItemPageReqVO.class)) {
            assertEquals(List.class, pageReqClass.getDeclaredField("excludedItemIds").getType());
        }
        Field keys = ErpStockItemMatchReqVO.class.getDeclaredField("keys");
        assertEquals(200, keys.getAnnotation(Size.class).max());
        Field pageSize = ErpStockItemMatchReqVO.class.getDeclaredField("pageSize");
        assertEquals(200, pageSize.getAnnotation(Max.class).value());
    }

    private static void assertControllerContract(Class<?> controllerClass, Class<?> pageReqClass,
            String getMethodName, String postMethodName, String matchMethodName, String permission) throws Exception {
        Method getMethod = controllerClass.getMethod(getMethodName, pageReqClass);
        Method postMethod = controllerClass.getMethod(postMethodName, pageReqClass);
        Method matchMethod = controllerClass.getMethod(matchMethodName, ErpStockItemMatchReqVO.class);
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
