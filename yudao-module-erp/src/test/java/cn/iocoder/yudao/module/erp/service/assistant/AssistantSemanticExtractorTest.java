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
        assertTrue(AssistantSemanticExtractor.extract("新津轮胎销量","query_verified_metric").stream()
                .noneMatch(item->item.type==AssistantEntityResolver.EntityType.SALESPERSON));
    }

    private static void assertReference(List<AssistantSemanticExtractor.Reference> references,
                                        AssistantEntityResolver.EntityType type,String keyword) {
        assertTrue(references.stream().anyMatch(item->item.type==type && keyword.equals(item.keyword)),
                ()->"missing "+type+":"+keyword);
    }
}
