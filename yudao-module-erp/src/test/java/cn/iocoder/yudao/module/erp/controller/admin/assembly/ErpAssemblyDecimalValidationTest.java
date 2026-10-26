package cn.iocoder.yudao.module.erp.controller.admin.assembly;

import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order.ErpAssemblyOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipeSaveReqVO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpAssemblyDecimalValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void recipeDecimalsAcceptSixPlacesAndRejectSevenPlaces() {
        ErpAssemblyRecipeSaveReqVO request = validRecipeRequest();
        assertFalse(hasViolation(validator.validate(request), "laborCostPerSet"));
        assertFalse(hasViolation(validator.validate(request), "items[0].quantityPerSet"));

        request.setLaborCostPerSet(new BigDecimal("1.1234567"));
        request.getItems().get(0).setQuantityPerSet(new BigDecimal("2.1234567"));
        Set<ConstraintViolation<ErpAssemblyRecipeSaveReqVO>> violations = validator.validate(request);
        assertTrue(hasViolation(violations, "laborCostPerSet"));
        assertTrue(hasViolation(violations, "items[0].quantityPerSet"));
    }

    @Test
    void recipeLaborCostIsRequired() {
        ErpAssemblyRecipeSaveReqVO request = validRecipeRequest();
        request.setLaborCostPerSet(null);

        Set<ConstraintViolation<ErpAssemblyRecipeSaveReqVO>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "laborCostPerSet"));
    }

    @Test
    void orderDecimalsAcceptSixPlacesAndRejectSevenPlaces() {
        ErpAssemblyOrderSaveReqVO request = validOrderRequest();
        assertFalse(hasViolation(validator.validate(request), "otherCost"));
        assertFalse(hasViolation(validator.validate(request), "items[0].consumeQuantity"));

        request.setOtherCost(new BigDecimal("1.1234567"));
        request.getItems().get(0).setConsumeQuantity(new BigDecimal("2.1234567"));
        Set<ConstraintViolation<ErpAssemblyOrderSaveReqVO>> violations = validator.validate(request);
        assertTrue(hasViolation(violations, "otherCost"));
        assertTrue(hasViolation(violations, "items[0].consumeQuantity"));
    }

    private static ErpAssemblyRecipeSaveReqVO validRecipeRequest() {
        ErpAssemblyRecipeSaveReqVO.Item item = new ErpAssemblyRecipeSaveReqVO.Item();
        item.setProductId(2L);
        item.setQuantityPerSet(new BigDecimal("2.123456"));
        ErpAssemblyRecipeSaveReqVO request = new ErpAssemblyRecipeSaveReqVO();
        request.setProductId(1L);
        request.setLaborCostPerSet(new BigDecimal("1.123456"));
        request.setItems(Collections.singletonList(item));
        return request;
    }

    private static ErpAssemblyOrderSaveReqVO validOrderRequest() {
        ErpAssemblyOrderSaveReqVO.Item item = new ErpAssemblyOrderSaveReqVO.Item();
        item.setRecipeItemId(2L);
        item.setConsumeQuantity(new BigDecimal("2.123456"));
        ErpAssemblyOrderSaveReqVO request = new ErpAssemblyOrderSaveReqVO();
        request.setRecipeId(1L);
        request.setSetCount(1);
        request.setWarehouseId(3L);
        request.setOtherCost(new BigDecimal("1.123456"));
        request.setItems(Collections.singletonList(item));
        return request;
    }

    private static boolean hasViolation(Set<? extends ConstraintViolation<?>> violations, String propertyPath) {
        return violations.stream().anyMatch(violation ->
                propertyPath.equals(violation.getPropertyPath().toString()));
    }

}
