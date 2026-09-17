package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpPurchaseKeywordQueryTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpPurchaseKeywordQueryTest.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpPurchaseInDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpPurchaseInItemDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpPurchaseInvoiceDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpPurchasePriceAdjustDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpPurchasePriceAdjustItemDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpProductDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpProductUnitDO.class);
    }

    @Test
    void appendPurchaseSupplierProductItemTokens_includesProductUnitAndTokenValues() {
        MPJLambdaWrapperX<ErpPurchaseInDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndPurchaseSupplierAndProductItemTokens(wrapper,
                "极护SP新包装/桶", "erp_purchase_in_items", "in_id",
                ErpPurchaseInDO::getNo, ErpPurchaseInDO::getRemark);

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("erp_purchase_in_items"), sql);
        assertTrue(sql.contains("erp_supplier"), sql);
        assertTrue(sql.contains("erp_product_unit"), sql);
        assertTrue(sql.contains("u.name LIKE"), sql);
        assertTrue(values.contains("%极护SP新包装/桶%"), values.toString());
        assertTrue(values.contains("%极护SP新包装%"), values.toString());
        assertTrue(values.contains("%桶%"), values.toString());
    }

    @Test
    void appendPurchaseSupplierProductItemTokens_keepsSpaceWildcardAndAddsTokens() {
        MPJLambdaWrapperX<ErpPurchaseInDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndPurchaseSupplierAndProductItemTokens(wrapper,
                "奔驰 刹车片", "erp_purchase_in_items", "in_id",
                ErpPurchaseInDO::getNo, ErpPurchaseInDO::getRemark);

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%奔驰%刹车片%"), values.toString());
        assertTrue(values.contains("%奔驰%"), values.toString());
        assertTrue(values.contains("%刹车片%"), values.toString());
    }

    @Test
    void appendProductKeyword_supportsProductNameUnitAndSnapshotTokens() {
        MPJLambdaWrapperX<ErpPurchasePriceAdjustDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendProductKeyword(wrapper, "钻石205/桶",
                ErpKeywordQuery.productKeywordColumn(ErpPurchasePriceAdjustItemDO::getProductName),
                ErpKeywordQuery.productKeywordColumn(ErpPurchasePriceAdjustItemDO::getProductUnitName));

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("name LIKE"), sql);
        assertTrue(sql.contains("product_name LIKE"), sql);
        assertTrue(sql.contains("product_unit_name LIKE"), sql);
        assertTrue(values.contains("%钻石205%"), values.toString());
        assertTrue(values.contains("%桶%"), values.toString());
    }

    @Test
    void appendPurchaseProductItemTokensByProductUnit_usesProductUnitWhenItemHasNoUnitId() {
        MPJLambdaWrapperX<ErpPurchaseInvoiceDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndPurchaseSupplierAndProductItemTokensByProductUnit(wrapper,
                "钻石205/根", "erp_purchase_invoice_item", "invoice_id",
                ErpPurchaseInvoiceDO::getNo, ErpPurchaseInvoiceDO::getRemark);

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("erp_purchase_invoice_item"), sql);
        assertTrue(sql.contains("erp_product_unit"), sql);
        assertTrue(sql.contains("u.id = p.unit_id"), sql);
        assertFalse(sql.contains("i.product_unit_id"), sql);
        assertTrue(values.contains("%钻石205%"), values.toString());
        assertTrue(values.contains("%根%"), values.toString());
    }

}
