package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ErpStockMoveMapperTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpStockMoveMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpStockMoveDO.class);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void selectListBySourceAndDirectionForUpdate_locksRowsAndIncludesLegacyTransferOut() {
        ErpStockMoveMapper mapper = mock(ErpStockMoveMapper.class, CALLS_REAL_METHODS);
        ArgumentCaptor<Wrapper<ErpStockMoveDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        doReturn(Collections.emptyList()).when(mapper).selectList(any(Wrapper.class));

        mapper.selectListBySourceAndDirectionForUpdate(
                ErpSaleBizSourceTypeEnum.CART.getType(), 177L, 10);

        verify(mapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapperX<ErpStockMoveDO> wrapper = (LambdaQueryWrapperX<ErpStockMoveDO>) wrapperCaptor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("source_type"));
        assertTrue(sqlSegment.contains("source_id"));
        assertTrue(sqlSegment.contains("transfer_direction"));
        assertTrue(sqlSegment.contains("IS NULL"));
        assertTrue(sqlSegment.contains("FOR UPDATE"));
    }

    @Test
    void applyTransferOutVisibleScope_departmentScope_requiresAllFromDepartments() {
        MPJLambdaWrapperX<ErpStockMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockMoveMapper.applyTransferOutVisibleScope(wrapper, Arrays.asList(10L, 20L), false);

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("from_dept_id"));
        assertTrue(sqlSegment.contains("NOT EXISTS"));
        assertTrue(sqlSegment.contains("erp_stock_move_item"));
        assertTrue(sqlSegment.contains("NOT IN"));
        assertFalse(sqlSegment.contains("t.dept_id"));
        assertFalse(sqlSegment.contains("creator"));
    }

    @Test
    void applyTransferOutVisibleScope_noDepartmentScope_deniesAllWithoutCreatorFallback() {
        MPJLambdaWrapperX<ErpStockMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockMoveMapper.applyTransferOutVisibleScope(wrapper, Collections.emptySet(), false);

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("1 = 0"));
        assertFalse(sqlSegment.contains("creator"));
        assertFalse(sqlSegment.contains("OR"));
    }

    @Test
    void applyTransferOutVisibleScope_allScope_addsNoFilter() {
        MPJLambdaWrapperX<ErpStockMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockMoveMapper.applyTransferOutVisibleScope(wrapper, Collections.singleton(10L), true);

        assertTrue(wrapper.getSqlSegment().isEmpty());
    }

    @Test
    void applyTransferInVisibleScope_departmentScope_requiresAllToDepartments() {
        MPJLambdaWrapperX<ErpStockMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockMoveMapper.applyTransferInVisibleScope(wrapper, Arrays.asList(10L, 20L), null, false);

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("to_dept_id"));
        assertTrue(sqlSegment.contains("NOT EXISTS"));
        assertTrue(sqlSegment.contains("erp_stock_move_item"));
        assertTrue(sqlSegment.contains("NOT IN"));
        assertFalse(sqlSegment.contains("t.dept_id"));
        assertFalse(sqlSegment.contains("from_dept_id"));
        assertFalse(sqlSegment.contains("creator"));
    }

    @Test
    void applyTransferInVisibleScope_creatorScope_usesCreator() {
        MPJLambdaWrapperX<ErpStockMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockMoveMapper.applyTransferInVisibleScope(wrapper, Collections.emptySet(), 88L, false);

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("1 = 0"));
        assertTrue(sqlSegment.contains("creator"));
        assertTrue(sqlSegment.contains("OR"));
    }

    @Test
    void applyTransferInVisibleScope_noScope_deniesAll() {
        MPJLambdaWrapperX<ErpStockMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockMoveMapper.applyTransferInVisibleScope(wrapper, Collections.emptySet(), null, false);

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("1 = 0"));
        assertFalse(sqlSegment.contains("creator"));
    }

    @Test
    void applyTransferInVisibleScope_allScope_addsNoFilter() {
        MPJLambdaWrapperX<ErpStockMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockMoveMapper.applyTransferInVisibleScope(wrapper, Collections.singleton(10L), 88L, true);

        assertTrue(wrapper.getSqlSegment().isEmpty());
    }

}
