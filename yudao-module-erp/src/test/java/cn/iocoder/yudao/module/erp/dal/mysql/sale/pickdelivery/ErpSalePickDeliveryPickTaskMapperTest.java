package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryPickTaskDO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpSalePickDeliveryPickTaskMapperTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpSalePickDeliveryPickTaskMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpSalePickDeliveryPickTaskDO.class);
    }

    @Test
    void buildPickQuery_shouldFilterStructuredSourceType() {
        ErpSalePickPageReqVO reqVO = new ErpSalePickPageReqVO();
        reqVO.setSourceType(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType());

        LambdaQueryWrapper<ErpSalePickDeliveryPickTaskDO> wrapper =
                ErpSalePickDeliveryPickTaskMapper.buildPickQuery(reqVO);

        assertTrue(wrapper.getSqlSegment().toLowerCase(Locale.ROOT).contains("source_type"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(
                ErpSaleBizSourceTypeEnum.SALE_RETURN.getType()));
    }

    @Test
    void buildPickQuery_shouldTreatZeroAsLegacyDirectSaleOut() {
        ErpSalePickPageReqVO reqVO = new ErpSalePickPageReqVO();
        reqVO.setSourceType(ErpSaleBizSourceTypeEnum.DIRECT_SALE_OUT_TYPE);

        String sql = ErpSalePickDeliveryPickTaskMapper.buildPickQuery(reqVO)
                .getSqlSegment().toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("source_type"));
        assertTrue(sql.contains("is null"));
    }

    @Test
    void buildPickQuery_shouldKeepAllSourcesWhenFilterIsAbsent() {
        String sql = ErpSalePickDeliveryPickTaskMapper.buildPickQuery(new ErpSalePickPageReqVO())
                .getSqlSegment().toLowerCase(Locale.ROOT);

        assertFalse(sql.contains("source_type"));
    }

}
