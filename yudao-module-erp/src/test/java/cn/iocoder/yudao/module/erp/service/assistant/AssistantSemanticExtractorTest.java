package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssistantSemanticExtractorTest {

    @Test
    void extractsNaturalProductAndWarehouseWithoutDamagingSpecificationCharacters() {
        List<AssistantSemanticExtractor.Reference> references=AssistantSemanticExtractor.extract(
                "路通源化油器清洗剂450ml*24在大塘仓的出入库情况","query_stock_movements");

        assertReference(references,AssistantEntityResolver.EntityType.PRODUCT,"路通源化油器清洗剂450ml*24");
        assertReference(references,AssistantEntityResolver.EntityType.WAREHOUSE,"大塘仓");
    }

    @Test
    void extractsWarehouseFirstNaturalStockQuestion() {
        List<AssistantSemanticExtractor.Reference> references=AssistantSemanticExtractor.extract(
                "大塘仓里，路通源化油器清洗剂450ml*24的库存是多少","query_product_stock");

        assertReference(references,AssistantEntityResolver.EntityType.PRODUCT,"路通源化油器清洗剂450ml*24");
        assertReference(references,AssistantEntityResolver.EntityType.WAREHOUSE,"大塘仓");
    }

    @Test
    void keepsMeaningfulSpacesInNaturalStockProductName() {
        List<AssistantSemanticExtractor.Reference> references=AssistantSemanticExtractor.extract(
                "美孚DOT4刹车油 208L 库存","query_product_stock");

        assertReference(references,AssistantEntityResolver.EntityType.PRODUCT,"美孚DOT4刹车油 208L");
    }

    @Test
    void extractsExplicitProductBrandAsAnIndependentMatchScope() {
        for(String question:new String[]{"品牌为美孚的产品库存","美孚品牌产品有多少个","美孚牌配件价格"}) {
            List<AssistantSemanticExtractor.Reference> references=AssistantSemanticExtractor.extract(
                    question,"query_product_stock");
            AssistantSemanticExtractor.Reference product=references.stream()
                    .filter(item->item.type==AssistantEntityResolver.EntityType.PRODUCT)
                    .findFirst().orElseThrow(AssertionError::new);
            assertEquals("美孚",product.keyword,question);
            assertEquals("EXPLICIT_BRAND",product.matchType,question);
            assertEquals(AssistantEntityResolver.ProductMatchScope.BRAND,product.productMatchScope,question);
        }
    }

    @Test
    void explicitProductIdentifierStillWinsOverBrandText() {
        List<AssistantSemanticExtractor.Reference> references=AssistantSemanticExtractor.extract(
                "美孚品牌产品编码为145988的库存","query_product_stock");
        AssistantSemanticExtractor.Reference product=references.stream()
                .filter(item->item.type==AssistantEntityResolver.EntityType.PRODUCT)
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals("145988",product.keyword);
        assertEquals(AssistantEntityResolver.ProductMatchScope.KEYWORD,product.productMatchScope);
    }

    @Test
    void extractsCustomerSupplierAndLabelledProductRoles() {
        assertReference(AssistantSemanticExtractor.extract("不存在的客户本月收款情况","query_verified_metric"),
                AssistantEntityResolver.EntityType.CUSTOMER,"不存在");
        List<AssistantSemanticExtractor.Reference> references=AssistantSemanticExtractor.extract(
                "供应商A卖给我们多少个产品B","query_verified_metric");
        assertReference(references,AssistantEntityResolver.EntityType.SUPPLIER,"A");
        assertReference(references,AssistantEntityResolver.EntityType.PRODUCT,"B");
    }

    @Test
    void extractsUnlabelledFinancePartiesWithoutTreatingRankingsAsObjects() {
        assertReference(AssistantSemanticExtractor.extract("理塘众鑫进口汽修  欠款","query_verified_metric"),
                AssistantEntityResolver.EntityType.CUSTOMER,"理塘众鑫进口汽修");
        assertReference(AssistantSemanticExtractor.extract("请问 四川供应链公司 本月应付情况","query_verified_metric"),
                AssistantEntityResolver.EntityType.SUPPLIER,"四川供应链公司");
        assertReference(AssistantSemanticExtractor.extract("理塘众鑫进口汽修 本月收款情况","query_verified_metric"),
                AssistantEntityResolver.EntityType.CUSTOMER,"理塘众鑫进口汽修");
        assertTrue(AssistantSemanticExtractor.extract("哪个客户欠款最多","query_verified_metric").stream()
                .noneMatch(item->item.type==AssistantEntityResolver.EntityType.CUSTOMER));
    }

    @Test
    void genericRankingDimensionsAreNotConcreteEntities() {
        List<AssistantSemanticExtractor.Reference> product=AssistantSemanticExtractor.extract("哪个产品库存最多","query_verified_metric");
        assertTrue(product.stream().noneMatch(item->item.type==AssistantEntityResolver.EntityType.PRODUCT));
        List<AssistantSemanticExtractor.Reference> customer=AssistantSemanticExtractor.extract("哪个客户当前欠款最多","query_verified_metric");
        assertTrue(customer.stream().noneMatch(item->item.type==AssistantEntityResolver.EntityType.CUSTOMER));
    }

    @Test
    void genericInventoryAndPlaceholderProductPhrasesAreNotConcreteEntities() {
        for(String question:new String[]{"有库存的配件有多少种","哪些配件有库存","配件库存是多少",
                "某产品库存是多少","有没有货"})
            assertTrue(AssistantSemanticExtractor.extract(question,"query_product_stock").stream()
                    .noneMatch(item->item.type==AssistantEntityResolver.EntityType.PRODUCT),question);
    }

    @Test
    void extractsBranchAsDepartmentForReceiptQuestions() {
        for (String question : new String[]{"甘孜分公司本月收款金额是多少", "甘孜分公司本月收了多少钱回来"}) {
            assertReference(AssistantSemanticExtractor.extract(question,"query_verified_metric"),
                    AssistantEntityResolver.EntityType.DEPARTMENT,"甘孜分公司");
        }
    }

    @Test
    void extractsSalespersonWithoutTreatingAProductCategoryAsAPerson() {
        List<AssistantSemanticExtractor.Reference> references=AssistantSemanticExtractor.extract(
                "业务员郑智文8月份轮胎销量","query_verified_metric");
        assertReference(references,AssistantEntityResolver.EntityType.SALESPERSON,"郑智文");
        assertReference(AssistantSemanticExtractor.extract("张三本月销售额","query_verified_metric"),
                AssistantEntityResolver.EntityType.SALESPERSON,"张三");
        assertTrue(AssistantSemanticExtractor.extract("新津轮胎销量","query_verified_metric").stream()
                .noneMatch(item->item.type==AssistantEntityResolver.EntityType.SALESPERSON));
    }

    @Test
    void extractsPurchaserButNotGenericPurchaserDimensions() {
        assertReference(AssistantSemanticExtractor.extract("采购员李四本月采购订单","query_purchase_documents"),
                AssistantEntityResolver.EntityType.PURCHASER,"李四");
        assertReference(AssistantSemanticExtractor.extract("李四经手的采购入库单","query_stage5_purchase"),
                AssistantEntityResolver.EntityType.PURCHASER,"李四");
        assertTrue(AssistantSemanticExtractor.extract("各采购员采购订单金额排名","query_purchase_documents").stream()
                .noneMatch(item->item.type==AssistantEntityResolver.EntityType.PURCHASER));
    }

    @Test
    void previousWeekAliasesAreConditionsAndNamedBusinessObjectsRemainExtractable() {
        for(String question:new String[]{"上个星期的采购额","上个星期的收款情况","上个星期的付款情况"})
            assertTrue(AssistantSemanticExtractor.extract(question,"query_verified_metric").isEmpty(),question);
        assertReference(AssistantSemanticExtractor.extract("成都路通源客户上个星期的销售情况","query_verified_metric"),
                AssistantEntityResolver.EntityType.CUSTOMER,"成都路通源");
        assertReference(AssistantSemanticExtractor.extract("销售部上个星期的销售情况","query_verified_metric"),
                AssistantEntityResolver.EntityType.DEPARTMENT,"销售部");
        assertReference(AssistantSemanticExtractor.extract("业务员张三上个星期的销售情况","query_verified_metric"),
                AssistantEntityResolver.EntityType.SALESPERSON,"张三");
    }

    @Test
    void extractsLabelledSystemUserWithoutTreatingEmployeeCountsAsAName() {
        assertReference(AssistantSemanticExtractor.extract("用户张三的所属部门","resolve_business_entity"),
                AssistantEntityResolver.EntityType.USER,"张三");
        assertReference(AssistantSemanticExtractor.extract("查询不存在的用户“黄金基线用户不存在”","resolve_business_entity"),
                AssistantEntityResolver.EntityType.USER,"黄金基线用户不存在");
        assertTrue(AssistantSemanticExtractor.extract("当前启用员工数量","query_master_data_stats").stream()
                .noneMatch(item->item.type==AssistantEntityResolver.EntityType.USER));
    }

    private static void assertReference(List<AssistantSemanticExtractor.Reference> references,
                                        AssistantEntityResolver.EntityType type,String keyword) {
        assertTrue(references.stream().anyMatch(item->item.type==type && keyword.equals(item.keyword)),
                ()->"missing "+type+":"+keyword);
    }
}
