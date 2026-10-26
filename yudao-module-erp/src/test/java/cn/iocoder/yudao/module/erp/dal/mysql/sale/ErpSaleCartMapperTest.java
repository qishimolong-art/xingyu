package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpSaleCartMapperTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpSaleCartMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpSaleCartDO.class);
    }

    @Test
    void shouldWhitelistRestoredDeliveryAndVinSortFields() {
        assertNotNull(ErpSaleCartMapper.getOrderColumn("deliveryMethod"));
        assertNotNull(ErpSaleCartMapper.getOrderColumn("vin"));
    }

    @Test
    void shouldRejectUnknownOrUnsafeSortFields() {
        assertNull(ErpSaleCartMapper.getOrderColumn(null));
        assertNull(ErpSaleCartMapper.getOrderColumn("id desc; delete from erp_sale_cart"));
    }

    @Test
    void shouldFilterOnlineOrdersByMallSource() {
        ErpSaleCartPageReqVO reqVO = new ErpSaleCartPageReqVO();
        reqVO.setOnlineOrder(true);

        MPJLambdaWrapperX<ErpSaleCartDO> wrapper = ErpSaleCartMapper.buildQuery(reqVO);
        String sql = wrapper.getSqlSegment().toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("source_type"));
        assertTrue(sql.contains("="));
        assertFalse(sql.contains("remark"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(ErpSaleBizSourceTypeEnum.MALL_ORDER.getType()));
    }

    @Test
    void shouldFilterErpOrdersByExcludingMallSource() {
        ErpSaleCartPageReqVO reqVO = new ErpSaleCartPageReqVO();
        reqVO.setOnlineOrder(false);

        MPJLambdaWrapperX<ErpSaleCartDO> wrapper = ErpSaleCartMapper.buildQuery(reqVO);
        String sql = wrapper.getSqlSegment().toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("source_type"));
        assertTrue(sql.contains("is null"));
        assertTrue(sql.contains("<>"));
        assertTrue(sql.contains("or"));
        assertFalse(sql.contains("remark"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(ErpSaleBizSourceTypeEnum.MALL_ORDER.getType()));
    }

    @Test
    void shouldKeepLegacyUnfilteredBehaviorWhenOnlineOrderIsAbsent() {
        String sql = ErpSaleCartMapper.buildQuery(new ErpSaleCartPageReqVO())
                .getSqlSegment().toLowerCase(Locale.ROOT);

        assertFalse(sql.contains("source_type"));
        assertFalse(sql.contains("remark"));
    }

    @Test
    void shouldBuildLightweightLatestOnlineOrderQueryByMallSource() {
        LambdaQueryWrapper<ErpSaleCartDO> wrapper = ErpSaleCartMapper.buildLatestOnlineOrderQuery();
        String sql = wrapper.getSqlSegment().toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("source_type"));
        assertTrue(sql.contains("order by"));
        assertTrue(sql.contains("id") && sql.contains("desc"));
        assertTrue(sql.contains("limit 1"));
        assertFalse(sql.contains("remark"));
        assertFalse(sql.contains("status"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(ErpSaleBizSourceTypeEnum.MALL_ORDER.getType()));
    }
}
