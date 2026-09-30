package cn.iocoder.yudao.module.erp.dal.mysql.assistant;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AssistantEntitySqlTest {

    @Test void productIdentifierUsesBoundExactFieldsAndPermissionScope() {
        Map<String,Object> context=base("PRODUCT","CODE");
        context.put("searchCode",true);context.put("searchFactoryCode",true);context.put("searchBarCode",true);
        context.put("showCode",true);context.put("entityAll",false);
        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();
        assertTrue(sql.contains("e.code,''))=UPPER(?)"));
        assertFalse(sql.contains("e.factory_code,''))=UPPER(?)"),"Business code has priority over auxiliary codes");
        assertTrue(sql.contains("erp_product_dept"));assertTrue(sql.contains("es.warehouse_id IN"));
        assertFalse(sql.contains("LIKE CONCAT('%',?,'%')"),"Business codes must not use contains matching");
        assertFalse(query.getParameters().contains("%X%"),"Wildcards must not be added to identifier parameters");
        assertEquals("X",query.getParameters().get(query.getParameters().size()-1));
        assertTrue(sql.contains("'CODE' matchedBy"));
    }

    @Test void auxiliaryProductIdentifiersAreQueriedSeparately() {
        Map<String,Object> factory=base("PRODUCT","FACTORY_CODE");
        factory.put("searchFactoryCode",true);factory.put("entityAll",true);
        String factorySql=AssistantEntitySql.select(factory).getSql();
        assertTrue(factorySql.contains("e.factory_code,''))=UPPER(?)"));
        assertTrue(factorySql.contains("CONCAT('厂家编码：',e.factory_code)"));
        assertFalse(factorySql.contains("e.bar_code,''))=UPPER(?)"));
        assertTrue(factorySql.contains("'FACTORY_CODE' matchedBy"));

        Map<String,Object> bar=base("PRODUCT","BAR_CODE");bar.put("searchBarCode",true);bar.put("entityAll",true);
        String barSql=AssistantEntitySql.select(bar).getSql();
        assertTrue(barSql.contains("e.bar_code,''))=UPPER(?)"));
        assertTrue(barSql.contains("CONCAT('条码：',e.bar_code)"));
        assertTrue(barSql.contains("'BAR_CODE' matchedBy"));
    }

    @Test void namesUseBoundLimitedFuzzyMatchingAndCustomerMasterScope() {
        Map<String,Object> context=base("CUSTOMER","FUZZY");
        context.put("searchShortName",true);context.put("searchPinyin",true);context.put("searchWubi",true);
        context.put("entityAll",false);context.put("showCode",false);
        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();
        assertTrue(sql.contains("e.name LIKE CONCAT('%',?,'%')"));
        assertTrue(sql.contains("e.pinyin_code LIKE CONCAT(?,'%')"));
        assertTrue(sql.contains("erp_customer_dept"));assertTrue(sql.contains("LIMIT 21"));
        assertTrue(query.getParameters().contains("X"));
    }

    @Test void userKeywordIsAlwaysAParameter() {
        Map<String,Object> context=base("WAREHOUSE","EXACT_NAME");
        context.put("searchCode",true);context.put("showCode",true);context.put("entityAll",true);
        context.put("entityKeyword","X' OR 1=1 --");
        AssistantEntitySql.Query query=AssistantEntitySql.select(context);
        assertFalse(query.getSql().contains("X' OR 1=1"));
        assertEquals("X' OR 1=1 --",query.getParameters().get(query.getParameters().size()-1));
    }

    @Test void hiddenProductCodesAreNotSelectedOrDisplayed() {
        Map<String,Object> context=base("PRODUCT","FUZZY");
        context.put("searchCode",false);context.put("searchFactoryCode",false);context.put("searchBarCode",false);
        context.put("searchPinyin",false);context.put("searchWubi",false);context.put("showCode",false);context.put("entityAll",true);
        String sql=AssistantEntitySql.select(context).getSql();
        assertTrue(sql.contains("e.name name,NULL code,NULL description"));
        assertFalse(sql.contains("CONCAT('厂家编码：',e.factory_code)"));
        assertFalse(sql.contains("e.code,''))=UPPER(?)"));
    }

    @Test void productSpaceKeywordUsesOrderedWildcardAndTokenAndMatching() {
        Map<String,Object> context=base("PRODUCT","TOKEN_FUZZY");context.put("entityAll",true);
        context.put("entityKeyword","美孚 CF 4L");
        context.put("searchCode",true);context.put("searchFactoryCode",true);context.put("searchBarCode",true);
        context.put("searchPinyin",true);context.put("searchWubi",true);context.put("searchBrand",true);
        context.put("searchStandard",true);context.put("searchVehicleModel",true);

        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();

        assertTrue(sql.contains("'TOKEN_FUZZY' matchedBy"));
        assertTrue(sql.contains("UPPER(COALESCE(e.name,'')) LIKE UPPER(?)"));
        assertTrue(sql.contains("UPPER(COALESCE(e.brand,'')) LIKE UPPER(?)"));
        assertTrue(sql.contains(" AND "),"All split tokens must match one visible product field");
        assertTrue(query.getParameters().contains("%美孚%CF%4L%"));
        assertTrue(query.getParameters().contains("%美孚%"));
        assertTrue(query.getParameters().contains("%CF%"));
        assertTrue(query.getParameters().contains("%4L%"));
    }

    @Test void productTokenSearchNeverUsesHiddenAttributeFields() {
        Map<String,Object> context=base("PRODUCT","TOKEN_FUZZY");context.put("entityAll",true);
        context.put("entityKeyword","美孚 CF 4L");
        context.put("searchCode",false);context.put("searchFactoryCode",false);context.put("searchBarCode",false);
        context.put("searchPinyin",false);context.put("searchWubi",false);context.put("searchBrand",false);
        context.put("searchStandard",false);context.put("searchVehicleModel",false);

        String sql=AssistantEntitySql.select(context).getSql();

        assertTrue(sql.contains("COALESCE(e.name,''"));
        assertFalse(sql.contains("COALESCE(e.code,''"));
        assertFalse(sql.contains("COALESCE(e.brand,''"));
        assertFalse(sql.contains("COALESCE(e.standard,''"));
        assertFalse(sql.contains("COALESCE(e.factory_code,''"));
    }

    @Test void explicitProductDelimiterUsesTokenAndWithoutUnitNormalization() {
        Map<String,Object> context=base("PRODUCT","TOKEN_FUZZY");context.put("entityAll",true);
        context.put("entityKeyword","WSC56208/WSC20856");

        AssistantEntitySql.Query query=AssistantEntitySql.select(context);

        assertFalse(query.getParameters().contains("%WSC56208/WSC20856%"));
        assertTrue(query.getParameters().contains("%WSC56208%"));
        assertTrue(query.getParameters().contains("%WSC20856%"));
    }

    @Test void salespersonSearchUsesNicknameAndAuthorizedDepartmentScope() {
        Map<String,Object> context=base("SALESPERSON","FUZZY");context.put("entityAll",false);
        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();
        assertTrue(sql.contains("FROM system_users e"));assertTrue(sql.contains("e.nickname LIKE CONCAT('%',?,'%')"));
        assertTrue(sql.contains("e.dept_id IN"));assertTrue(sql.contains("e.id=?"));
        assertFalse(sql.contains("e.code"));
    }

    private static Map<String,Object> base(String type,String match) {
        Map<String,Object> context=new HashMap<>();context.put("mode","entityCandidates");context.put("entityType",type);
        context.put("entityMatchMode",match);context.put("tenantId",1L);context.put("entityKeyword","X");context.put("rowLimit",21);
        context.put("entityDeptIds",Collections.singleton(1L));context.put("entityWarehouseIds",Collections.singleton(2L));
        context.put("entitySelfUserId","3");context.put("entitySelfDeptId",1L);return context;
    }
}
