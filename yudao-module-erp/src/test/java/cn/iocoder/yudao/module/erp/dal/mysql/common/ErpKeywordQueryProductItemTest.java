package cn.iocoder.yudao.module.erp.dal.mysql.common;

import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpKeywordQueryProductItemTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpKeywordQueryProductItemTest.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpStockInDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpStockInItemDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpProductDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpProductUnitDO.class);
    }

    @Test
    void appendProductKeyword_keepsSpaceWildcardAndAddsProductTokens() {
        MPJLambdaWrapperX<ErpStockInDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendProductKeyword(wrapper, "ILK 7B11");

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%ILK%7B11%"), values.toString());
        assertTrue(values.contains("%ILK%"), values.toString());
        assertTrue(values.contains("%7B11%"), values.toString());
    }

    @Test
    void appendProductKeyword_stripsTrailingExplicitDelimiter() {
        MPJLambdaWrapperX<ErpStockInDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendProductKeyword(wrapper, "极护SP新包装,");

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%极护SP新包装%"), values.toString());
    }

    @Test
    void appendPurchaseSupplierProductItemTokens_includesUnitName() {
        MPJLambdaWrapperX<ErpStockInDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndPurchaseSupplierAndProductItemTokens(wrapper,
                "极护SP新包装，桶", "erp_stock_in_item", "in_id",
                ErpStockInDO::getNo, ErpStockInDO::getRemark);

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("erp_stock_in_item"), sql);
        assertTrue(sql.contains("erp_product_unit"), sql);
        assertTrue(sql.contains("u.name LIKE"), sql);
        assertTrue(sql.contains("erp_supplier"), sql);
        assertTrue(values.contains("%极护SP新包装，桶%"), values.toString());
        assertTrue(values.contains("%极护SP新包装%"), values.toString());
        assertTrue(values.contains("%桶%"), values.toString());
    }

}
