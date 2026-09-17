package cn.iocoder.yudao.module.erp.controller.admin;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchasePriceAdjustTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockCheckTypeEnum;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpIncrementalItemDeleteValidationTest {

    private static final Validator VALIDATOR;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();
    }

    @Test
    void saleCartUpdateAllowsDeleteOnlyItem() {
        ErpSaleCartSaveReqVO reqVO = new ErpSaleCartSaveReqVO();
        reqVO.setCustomerId(1L);
        ErpSaleCartSaveReqVO.Item item = new ErpSaleCartSaveReqVO.Item();
        item.setId(10L);
        item.setOperation("delete");
        reqVO.setItems(Collections.singletonList(item));

        assertNoItemFieldViolation(reqVO);
    }

    @Test
    void saleQuoteUpdateAllowsDeleteOnlyItem() {
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setCustomerId(1L);
        ErpSaleQuoteSaveReqVO.Item item = new ErpSaleQuoteSaveReqVO.Item();
        item.setId(10L);
        item.setOperation("delete");
        reqVO.setItems(Collections.singletonList(item));

        assertNoItemFieldViolation(reqVO);
    }

    @Test
    void purchasePriceAdjustUpdateAllowsDeleteOnlyItem() {
        ErpPurchasePriceAdjustSaveReqVO reqVO = new ErpPurchasePriceAdjustSaveReqVO();
        reqVO.setSupplierId(1L);
        reqVO.setAdjustType(ErpPurchasePriceAdjustTypeEnum.BY_ITEM.getType());
        ErpPurchasePriceAdjustSaveReqVO.Item item = new ErpPurchasePriceAdjustSaveReqVO.Item();
        item.setId(10L);
        item.setOperation("delete");
        reqVO.setItems(Collections.singletonList(item));

        assertNoItemFieldViolation(reqVO);
    }

    @Test
    void stockInUpdateAllowsDeleteOnlyItem() {
        ErpStockInSaveReqVO reqVO = new ErpStockInSaveReqVO();
        reqVO.setInTime(LocalDateTime.now());
        ErpStockInSaveReqVO.Item item = new ErpStockInSaveReqVO.Item();
        item.setId(10L);
        item.setOperation("delete");
        reqVO.setItems(Collections.singletonList(item));

        assertNoItemFieldViolation(reqVO);
    }

    @Test
    void stockOutUpdateAllowsDeleteOnlyItem() {
        ErpStockOutSaveReqVO reqVO = new ErpStockOutSaveReqVO();
        reqVO.setOutTime(LocalDateTime.now());
        ErpStockOutSaveReqVO.Item item = new ErpStockOutSaveReqVO.Item();
        item.setId(10L);
        item.setOperation("delete");
        reqVO.setItems(Collections.singletonList(item));

        assertNoItemFieldViolation(reqVO);
    }

    @Test
    void stockMoveUpdateAllowsDeleteOnlyItem() {
        ErpStockMoveSaveReqVO reqVO = new ErpStockMoveSaveReqVO();
        reqVO.setMoveTime(LocalDateTime.now());
        ErpStockMoveSaveReqVO.Item item = new ErpStockMoveSaveReqVO.Item();
        item.setId(10L);
        item.setOperation("delete");
        reqVO.setItems(Collections.singletonList(item));

        assertNoItemFieldViolation(reqVO);
    }

    @Test
    void stockCheckUpdateAllowsDeleteOnlyItem() {
        ErpStockCheckSaveReqVO reqVO = new ErpStockCheckSaveReqVO();
        reqVO.setCheckTime(LocalDateTime.now());
        reqVO.setCheckType(ErpStockCheckTypeEnum.COUNT.getType());
        ErpStockCheckSaveReqVO.Item item = new ErpStockCheckSaveReqVO.Item();
        item.setId(10L);
        item.setOperation("delete");
        reqVO.setItems(Collections.singletonList(item));

        assertNoItemFieldViolation(reqVO);
    }

    @Test
    void warehouseMoveUpdateAllowsDeleteOnlyItem() {
        ErpWarehouseMoveSaveReqVO reqVO = new ErpWarehouseMoveSaveReqVO();
        reqVO.setMoveTime(LocalDateTime.now());
        reqVO.setFromWarehouseId(1L);
        reqVO.setToWarehouseId(2L);
        ErpWarehouseMoveSaveReqVO.Item item = new ErpWarehouseMoveSaveReqVO.Item();
        item.setId(10L);
        item.setOperation("delete");
        reqVO.setItems(Collections.singletonList(item));

        assertNoItemFieldViolation(reqVO);
    }

    private void assertNoItemFieldViolation(Object reqVO) {
        Set<String> itemViolations = VALIDATOR.validate(reqVO).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .filter(path -> path.startsWith("items[0]."))
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(itemViolations.isEmpty(), "Unexpected item field violations: " + itemViolations);
    }

}
