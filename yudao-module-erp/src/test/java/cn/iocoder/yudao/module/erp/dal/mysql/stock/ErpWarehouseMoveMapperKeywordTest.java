package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpWarehouseMoveMapperKeywordTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpWarehouseMoveMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpWarehouseMoveDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpWarehouseMoveItemDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpProductDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpProductUnitDO.class);
    }

    @Test
    void appendWarehouseMoveKeyword_supportsProductNameAndUnitTokens() {
        MPJLambdaWrapperX<ErpWarehouseMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpWarehouseMoveMapper.appendWarehouseMoveKeyword(wrapper, "极护SP新包装，桶");

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("erp_warehouse_move_item"), sql);
        assertTrue(sql.contains("erp_product_unit"), sql);
        assertTrue(sql.contains("p.name LIKE"), sql);
        assertTrue(sql.contains("u.name LIKE"), sql);
        assertTrue(sql.contains("AND"), sql);
        assertTrue(values.contains("%极护SP新包装，桶%"), values.toString());
        assertTrue(values.contains("%极护SP新包装%"), values.toString());
        assertTrue(values.contains("%桶%"), values.toString());
    }

    @Test
    void appendWarehouseMoveKeyword_keepsSingleKeywordDocumentFields() {
        MPJLambdaWrapperX<ErpWarehouseMoveDO> wrapper = new MPJLambdaWrapperX<>();

        ErpWarehouseMoveMapper.appendWarehouseMoveKeyword(wrapper, "YH202609");

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("no"), sql);
        assertTrue(sql.contains("source_no"), sql);
        assertTrue(sql.contains("remark"), sql);
        assertTrue(sql.contains("system_dept"), sql);
        assertTrue(sql.contains("DATE_FORMAT"), sql);
        assertFalse(wrapper.getParamNameValuePairs().values().contains("%YH202609%桶%"));
    }

}
