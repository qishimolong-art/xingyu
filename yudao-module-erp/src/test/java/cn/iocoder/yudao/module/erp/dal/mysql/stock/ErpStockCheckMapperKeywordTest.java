package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockCheckMapperKeywordTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpStockCheckMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpStockCheckDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpStockCheckItemDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpProductDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpProductUnitDO.class);
    }

    @Test
    void appendStockCheckKeyword_supportsProductNameAndUnitTokens() {
        MPJLambdaWrapperX<ErpStockCheckDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockCheckMapper.appendStockCheckKeyword(wrapper, "钻石205，根");

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("erp_stock_check_item"), sql);
        assertTrue(sql.contains("erp_product_unit"), sql);
        assertTrue(sql.contains("p.name LIKE"), sql);
        assertTrue(sql.contains("u.name LIKE"), sql);
        assertTrue(sql.contains("AND"), sql);
        assertTrue(values.contains("%钻石205，根%"), values.toString());
        assertTrue(values.contains("%钻石205%"), values.toString());
        assertTrue(values.contains("%根%"), values.toString());
    }

    @Test
    void appendStockCheckKeyword_keepsSingleKeywordDocumentFields() {
        MPJLambdaWrapperX<ErpStockCheckDO> wrapper = new MPJLambdaWrapperX<>();

        ErpStockCheckMapper.appendStockCheckKeyword(wrapper, "QCPD202609");

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("no"), sql);
        assertTrue(sql.contains("remark"), sql);
        assertTrue(sql.contains("system_dept"), sql);
        assertTrue(sql.contains("DATE_FORMAT"), sql);
        assertFalse(wrapper.getParamNameValuePairs().values().contains("%QCPD202609%根%"));
    }

}
