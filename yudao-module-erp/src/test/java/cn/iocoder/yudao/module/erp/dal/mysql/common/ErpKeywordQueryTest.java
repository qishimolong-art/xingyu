package cn.iocoder.yudao.module.erp.dal.mysql.common;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpPurchaseSuggestionDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.chain.ErpChainOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpKeywordQueryTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpSaleOrderMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpSaleOrderDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpReceivableOtherDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpPrePaymentDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpChainOrderDO.class);
        TableInfoHelper.initTableInfo(assistant, ErpPurchaseSuggestionDO.class);
    }

    @Test
    void appendWithDeptNameAndSaleCustomerAndProductItems_matchesCustomerAndProductMnemonicCodes() {
        MPJLambdaWrapperX<ErpSaleOrderDO> wrapper = new MPJLambdaWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndSaleCustomerAndProductItems(wrapper, " WZ JH ",
                "erp_sale_order_items", "order_id",
                ErpSaleOrderDO::getNo, ErpSaleOrderDO::getRemark);

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("erp_customer"), sql);
        assertTrue(sql.contains("member_code"), sql);
        assertTrue(sql.contains("platform_code"), sql);
        assertTrue(sql.contains("erp_sale_order_items"), sql);
        assertTrue(sql.contains("p.pinyin_code"), sql);
        assertTrue(sql.contains("p.wubi_code"), sql);
    }

    @Test
    void appendWithDeptNameAndCustomer_matchesCustomerMnemonicCodes() {
        LambdaQueryWrapperX<ErpReceivableOtherDO> wrapper = new LambdaQueryWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndCustomer(wrapper, "KH",
                ErpReceivableOtherDO::getNo, ErpReceivableOtherDO::getRemark);

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("erp_customer"), sql);
        assertTrue(sql.contains("pinyin_code"), sql);
        assertTrue(sql.contains("wubi_code"), sql);
    }

    @Test
    void appendWithDeptNameAndParty_matchesCustomerAndSupplierMnemonicCodes() {
        LambdaQueryWrapperX<ErpPrePaymentDO> wrapper = new LambdaQueryWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndParty(wrapper, "GYS",
                ErpPrePaymentDO::getNo, ErpPrePaymentDO::getPartyName);

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("party_type = 1"), sql);
        assertTrue(sql.contains("erp_customer"), sql);
        assertTrue(sql.contains("party_type = 2"), sql);
        assertTrue(sql.contains("erp_supplier"), sql);
        assertTrue(sql.contains("pinyin_code"), sql);
        assertTrue(sql.contains("wubi_code"), sql);
    }

    @Test
    void appendWithDeptNameAndCustomerAndProductItems_matchesLambdaCustomerAndProductItems() {
        LambdaQueryWrapperX<ErpChainOrderDO> wrapper = new LambdaQueryWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndCustomerAndProductItems(wrapper, "JH",
                "erp_chain_order", "erp_chain_order_item", "chain_order_id",
                ErpChainOrderDO::getNo, ErpChainOrderDO::getRemark);

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("erp_customer"), sql);
        assertTrue(sql.contains("erp_chain_order_item"), sql);
        assertTrue(sql.contains("i.chain_order_id = erp_chain_order.id"), sql);
        assertTrue(sql.contains("p.pinyin_code"), sql);
        assertTrue(sql.contains("p.wubi_code"), sql);
    }

    @Test
    void appendWithDeptNameAndProductItemsAndItemSupplier_matchesItemSupplierMnemonicCodes() {
        LambdaQueryWrapperX<ErpPurchaseSuggestionDO> wrapper = new LambdaQueryWrapperX<>();

        ErpKeywordQuery.appendWithDeptNameAndProductItemsAndItemSupplier(wrapper, "GYS",
                "erp_purchase_suggestion", "erp_purchase_suggestion_item", "suggestion_id",
                ErpPurchaseSuggestionDO::getNo);

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("erp_purchase_suggestion_item"), sql);
        assertTrue(sql.contains("i.suggestion_id = erp_purchase_suggestion.id"), sql);
        assertTrue(sql.contains("erp_supplier"), sql);
        assertTrue(sql.contains("s.pinyin_code"), sql);
        assertTrue(sql.contains("s.wubi_code"), sql);
    }

    @Test
    void appendWithDeptNameAndProductItems_rejectsUnsafeIdentifiers() {
        MPJLambdaWrapperX<ErpSaleOrderDO> wrapper = new MPJLambdaWrapperX<>();

        assertThrows(IllegalArgumentException.class, () ->
                ErpKeywordQuery.appendWithDeptNameAndProductItems(wrapper, "JH",
                        "erp_sale_order_items where 1=1", "order_id", ErpSaleOrderDO::getNo));
    }

}
