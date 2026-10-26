package cn.iocoder.yudao.module.erp.dal.mysql.assembly;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ErpAssemblyItemMapperGuardTest {

    @Test
    void recipeCount_skipsSqlForEmptyIds() {
        ErpAssemblyRecipeItemMapper mapper = mock(ErpAssemblyRecipeItemMapper.class, CALLS_REAL_METHODS);

        assertTrue(mapper.selectCountRowsByRecipeIds(Collections.emptyList()).isEmpty());

        verify(mapper, never()).selectCountRowsByRecipeIdsInternal(any());
    }

    @Test
    void orderCount_skipsSqlForEmptyIds() {
        ErpAssemblyOrderItemMapper mapper = mock(ErpAssemblyOrderItemMapper.class, CALLS_REAL_METHODS);

        assertTrue(mapper.selectCountRowsByOrderIds(Collections.emptyList()).isEmpty());

        verify(mapper, never()).selectCountRowsByOrderIdsInternal(any());
    }

}
