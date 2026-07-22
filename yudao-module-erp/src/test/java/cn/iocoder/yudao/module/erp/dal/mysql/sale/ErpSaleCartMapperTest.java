package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ErpSaleCartMapperTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpSaleCartMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpSaleCartDO.class);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void selectByIdForUpdate_locksSaleCartRow() {
        ErpSaleCartMapper mapper = mock(ErpSaleCartMapper.class, CALLS_REAL_METHODS);
        ArgumentCaptor<Wrapper<ErpSaleCartDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(173L);
        doReturn(cart).when(mapper).selectOne(wrapperCaptor.capture());

        ErpSaleCartDO result = mapper.selectByIdForUpdate(173L);

        assertEquals(cart, result);
        LambdaQueryWrapper<ErpSaleCartDO> wrapper = (LambdaQueryWrapper<ErpSaleCartDO>) wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("id"));
        assertTrue(wrapper.getSqlSegment().contains("FOR UPDATE"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(173L));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void cancelFirstApprove_usesStatusCasAndExplicitNullAssignments() {
        ErpSaleCartMapper mapper = mock(ErpSaleCartMapper.class, CALLS_REAL_METHODS);
        ArgumentCaptor<Wrapper<ErpSaleCartDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        doReturn(1).when(mapper).update(isNull(), wrapperCaptor.capture());

        int updateCount = mapper.cancelFirstApprove(173L);

        assertEquals(1, updateCount);
        verify(mapper).update(isNull(), wrapperCaptor.capture());
        LambdaUpdateWrapper<ErpSaleCartDO> wrapper = (LambdaUpdateWrapper<ErpSaleCartDO>) wrapperCaptor.getValue();
        String sqlSet = wrapper.getSqlSet();
        assertTrue(sqlSet.contains("status="));
        assertTrue(sqlSet.contains("first_audit_user_id="));
        assertTrue(sqlSet.contains("first_audit_time="));
        assertTrue(wrapper.getSqlSegment().contains("id"));
        assertTrue(wrapper.getSqlSegment().contains("status"));

        Collection<Object> parameterValues = ((Map<String, Object>) wrapper.getParamNameValuePairs()).values();
        assertTrue(parameterValues.contains(173L));
        assertTrue(parameterValues.contains(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()));
        assertTrue(parameterValues.contains(ErpSaleCartStatusEnum.SUBMITTED.getStatus()));
        assertEquals(2L, parameterValues.stream().filter(value -> value == null).count());
    }

}
