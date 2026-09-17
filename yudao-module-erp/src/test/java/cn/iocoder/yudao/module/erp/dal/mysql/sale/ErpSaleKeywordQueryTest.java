package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpSaleKeywordQueryTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpSaleKeywordQueryTest.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpSaleOutDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpSaleOutItemDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpSalePriceAdjustDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpProductDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpProductUnitDO.class);
    }

    @Test
    void appendSaleCustomerProductItemTokens_includesProductUnitAndTokenValues() {
        MPJLambdaWrapperX<ErpSaleOutDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndSaleCustomerAndProductItemTokens(wrapper,
                "极护SP新包装/桶", "erp_sale_out_items", "out_id",
                ErpSaleOutDO::getNo, ErpSaleOutDO::getRemark);

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("erp_sale_out_items"), sql);
        assertTrue(sql.contains("erp_customer"), sql);
        assertTrue(sql.contains("erp_product_unit"), sql);
        assertTrue(sql.contains("u.name LIKE"), sql);
        assertTrue(values.contains("%极护SP新包装/桶%"), values.toString());
        assertTrue(values.contains("%极护SP新包装%"), values.toString());
        assertTrue(values.contains("%桶%"), values.toString());
    }

    @Test
    void appendSaleCustomerProductItemTokens_keepsSpaceWildcardAndAddsTokens() {
        MPJLambdaWrapperX<ErpSaleOutDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndSaleCustomerAndProductItemTokens(wrapper,
                "奔驰 刹车片", "erp_sale_out_items", "out_id",
                ErpSaleOutDO::getNo, ErpSaleOutDO::getRemark);

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%奔驰%刹车片%"), values.toString());
        assertTrue(values.contains("%奔驰%"), values.toString());
        assertTrue(values.contains("%刹车片%"), values.toString());
    }

    @Test
    void appendProductKeyword_supportsProductNameAndUnitTokens() {
        MPJLambdaWrapperX<ErpSaleOutDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendProductKeyword(wrapper, "钻石205/根");

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("name LIKE"), sql);
        assertTrue(values.contains("%钻石205%"), values.toString());
        assertTrue(values.contains("%根%"), values.toString());
    }

    @Test
    void appendSalePriceAdjustProductItemTokens_usesProductUnitWhenItemHasNoUnitId() {
        MPJLambdaWrapperX<ErpSalePriceAdjustDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndSaleCustomerAndProductItemTokensByProductUnit(wrapper,
                "钻石205/根", "erp_sale_price_adjust_item", "adjust_id",
                ErpSalePriceAdjustDO::getNo, ErpSalePriceAdjustDO::getRemark);

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("erp_sale_price_adjust_item"), sql);
        assertTrue(sql.contains("erp_product_unit"), sql);
        assertTrue(sql.contains("u.id = p.unit_id"), sql);
        assertTrue(values.contains("%钻石205%"), values.toString());
        assertTrue(values.contains("%根%"), values.toString());
    }

}
