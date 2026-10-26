package cn.iocoder.yudao.module.erp.dal.mysql.assistant;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AssistantEntitySqlTest {

    @Test void productIdentifierModesCannotSearchNonNameFields() {
        for(String mode:Arrays.asList("CODE","FACTORY_CODE","BAR_CODE","PINYIN")) {
            Map<String,Object> context=base("PRODUCT",mode);
            context.put("searchCode",true);context.put("searchFactoryCode",true);context.put("searchBarCode",true);
            context.put("searchPinyin",true);context.put("showCode",false);context.put("entityAll",true);

            AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();

            assertTrue(sql.contains(" AND (1=0) "),mode+" must fail closed");
            assertFalse(sql.contains("COALESCE(e.code,''))"),sql);
            assertFalse(sql.contains("COALESCE(e.factory_code,''))"),sql);
            assertFalse(sql.contains("COALESCE(e.bar_code,''))"),sql);
            assertFalse(sql.contains("COALESCE(e.pinyin_code,''))"),sql);
            assertFalse(query.getParameters().contains("X"),mode+" must not bind a non-name keyword");
        }
    }

    @Test void nonProductIdentifiersKeepExistingExactLookup() {
        Map<String,Object> warehouse=base("WAREHOUSE","CODE");
        warehouse.put("searchCode",true);warehouse.put("showCode",true);warehouse.put("entityAll",true);
        AssistantEntitySql.Query query=AssistantEntitySql.select(warehouse);String sql=query.getSql();
        assertTrue(sql.contains("COALESCE(e.warehouse_code,''))=UPPER(?)"),sql);
        assertEquals("X",query.getParameters().get(query.getParameters().size()-1));
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

    @Test void phoneticModeUsesBoundContainsMatchingAndStableRelevanceOrder() {
        Map<String,Object> context=base("CUSTOMER","PINYIN");
        context.put("searchPinyin",true);context.put("entityAll",true);context.put("entityKeyword","JDBX");

        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();

        assertTrue(sql.contains("pinyin_code,'')) LIKE CONCAT('%',UPPER(?),'%')"));
        assertTrue(sql.contains("CASE WHEN UPPER(COALESCE(e.pinyin_code,''))=UPPER(?) THEN 0"));
        assertTrue(sql.contains("LOCATE(UPPER(?),UPPER(COALESCE(e.pinyin_code,'')))"));
        assertTrue(sql.contains("CHAR_LENGTH(e.name),e.id"));
        assertFalse(sql.contains("JDBX"));
        assertEquals(4,Collections.frequency(query.getParameters(),"JDBX"));
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

    @Test void productSpaceKeywordPreservesCrossTokenOrderOnNameOnly() {
        Map<String,Object> context=base("PRODUCT","TOKEN_FUZZY");context.put("entityAll",true);
        context.put("entityKeyword","美孚 CF 4L");
        context.put("searchCode",true);context.put("searchFactoryCode",true);context.put("searchBarCode",true);
        context.put("searchPinyin",true);context.put("searchWubi",true);context.put("searchBrand",true);
        context.put("searchStandard",true);context.put("searchVehicleModel",true);context.put("searchDrawingNo",true);

        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();

        assertTrue(sql.contains("'TOKEN_FUZZY' matchedBy"));
        assertTrue(sql.contains("UPPER(COALESCE(e.name,'')) REGEXP UPPER(?)"));
        assertFalse(sql.contains("UPPER(COALESCE(e.drawing_no,'')) LIKE UPPER(?)"));
        assertFalse(sql.contains("UPPER(COALESCE(e.code,'')) LIKE UPPER(?)"));
        assertFalse(sql.contains("UPPER(COALESCE(e.pinyin_code,'')) LIKE UPPER(?)"));
        assertFalse(sql.contains("UPPER(COALESCE(e.wubi_code,'')) LIKE UPPER(?)"));
        assertFalse(sql.contains("UPPER(COALESCE(e.bar_code,'')) LIKE UPPER(?)"));
        assertFalse(sql.contains("UPPER(COALESCE(e.vehicle_model,'')) LIKE UPPER(?)"));
        assertFalse(sql.contains("UPPER(COALESCE(e.factory_code,'')) LIKE UPPER(?)"));
        assertFalse(sql.contains("UPPER(COALESCE(e.brand,'')) LIKE UPPER(?)"));
        assertFalse(sql.contains("UPPER(COALESCE(e.standard,'')) LIKE UPPER(?)"));
        assertTrue(hasRegex(query,"美孚", "CF", "4L"));
        assertTrue(hasRegex(query,"美[^[:space:]]*孚", "C[^[:space:]]*F", "4L"));
        assertTrue(hasRegex(query,"美孚", "CF", "4升"));
        assertTrue(hasRegex(query,"美[^[:space:]]*孚", "C[^[:space:]]*F", "4升"));
        assertFalse(parametersContain(query,"4[^[:space:]]*L"));
        assertFalse(query.getParameters().contains("%美孚%"));
        assertFalse(query.getParameters().contains("%CF%"));
        assertFalse(query.getParameters().contains("%4L%"));
    }

    @Test void productFuzzyKeywordAddsOrderedCharacterFallback() {
        Map<String,Object> context=base("PRODUCT","FUZZY");context.put("entityAll",true);
        context.put("entityKeyword","QTF60");context.put("searchCode",true);
        context.put("searchFactoryCode",true);context.put("searchBarCode",true);

        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();

        assertTrue(sql.contains("UPPER(COALESCE(e.name,'')) REGEXP UPPER(?)"),sql);
        assertFalse(sql.contains("UPPER(COALESCE(e.code,'')) LIKE UPPER(?)"),sql);
        assertFalse(sql.contains("UPPER(COALESCE(e.factory_code,'')) LIKE UPPER(?)"),sql);
        assertFalse(sql.contains("UPPER(COALESCE(e.bar_code,'')) LIKE UPPER(?)"),sql);
        assertTrue(query.getParameters().contains("QTF60"));
        assertTrue(parametersContain(query,"Q[^[:space:]]*T[^[:space:]]*F[^[:space:]]*6[^[:space:]]*0"));
        assertFalse(parametersContain(query,"6[^[:space:]]*0[^[:space:]]*Q"));
    }

    @Test void productTokenKeywordKeepsUnitAliasesInTheSameOrderedSequence() {
        Map<String,Object> context=base("PRODUCT","TOKEN_FUZZY");context.put("entityAll",true);
        context.put("entityKeyword","经典表现 530 4 升");
        context.put("searchCode",true);context.put("searchFactoryCode",true);

        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();

        assertTrue(hasRegex(query,"经典表现", "530", "4升"));
        assertTrue(hasRegex(query,"经[^[:space:]]*典[^[:space:]]*表[^[:space:]]*现",
                "5[^[:space:]]*3[^[:space:]]*0", "4升"));
        assertTrue(hasRegex(query,"经典表现", "530", "4L"));
        assertTrue(hasRegex(query,"经[^[:space:]]*典[^[:space:]]*表[^[:space:]]*现",
                "5[^[:space:]]*3[^[:space:]]*0", "4L"));
        assertFalse(parametersContain(query,"4[^[:space:]]*L"));
        assertFalse(query.getParameters().contains("%经典表现%"));
        assertFalse(query.getParameters().contains("%530%"));
        assertFalse(query.getParameters().contains("%4L%"));
        assertFalse(query.getParameters().contains("%4% %升%"));
        assertFalse(sql.contains("COALESCE(e.code,''))"),sql);
    }

    @Test void pureNumericProductKeywordKeepsOrderedCharactersOnNameOnly() {
        Map<String,Object> fuzzyContext=base("PRODUCT","FUZZY");fuzzyContext.put("entityAll",true);
        fuzzyContext.put("entityKeyword","145988");fuzzyContext.put("searchCode",true);
        AssistantEntitySql.Query fuzzyQuery=AssistantEntitySql.select(fuzzyContext);
        assertTrue(fuzzyQuery.getParameters().contains("145988"));
        assertTrue(parametersContain(fuzzyQuery,"1[^[:space:]]*4[^[:space:]]*5[^[:space:]]*9[^[:space:]]*8[^[:space:]]*8"));
        assertFalse(fuzzyQuery.getSql().contains("COALESCE(e.code,''))"));
        assertFalse(fuzzyQuery.getSql().contains("COALESCE(e.vehicle_model,''))"));

        Map<String,Object> tokenContext=base("PRODUCT","TOKEN_FUZZY");tokenContext.put("entityAll",true);
        tokenContext.put("entityKeyword","经典 145988");tokenContext.put("searchCode",true);
        AssistantEntitySql.Query tokenQuery=AssistantEntitySql.select(tokenContext);
        assertTrue(hasRegex(tokenQuery,"经典", "145988"));
        assertTrue(hasRegex(tokenQuery,"经[^[:space:]]*典",
                "1[^[:space:]]*4[^[:space:]]*5[^[:space:]]*9[^[:space:]]*8[^[:space:]]*8"));
        assertFalse(tokenQuery.getParameters().contains("%145988%"));
        assertFalse(tokenQuery.getSql().contains("COALESCE(e.code,''))"));
        assertFalse(tokenQuery.getSql().contains("COALESCE(e.vehicle_model,''))"));
    }

    @Test void explicitBrandModeSearchesProductNameOnly() {
        Map<String,Object> context=base("PRODUCT","BRAND");context.put("entityAll",true);
        context.put("entityKeyword","美孚");context.put("searchBrand",true);
        context.put("searchCode",true);context.put("searchVehicleModel",true);

        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();

        assertTrue(sql.contains("COALESCE(e.name,'')) LIKE CONCAT('%',UPPER(?),'%')"),sql);
        assertFalse(sql.contains("COALESCE(e.brand,'')) LIKE"),sql);
        assertFalse(sql.contains("COALESCE(e.code,'')) LIKE"),sql);
        assertFalse(sql.contains("COALESCE(e.vehicle_model,'')) LIKE"),sql);
        assertTrue(query.getParameters().contains("美孚"));
    }

    @Test void productFuzzyUnitAliasesRemainAssistantOnlyAndParameterized() {
        Map<String,Object> product=base("PRODUCT","FUZZY");product.put("entityAll",true);
        product.put("entityKeyword","20千克");product.put("searchCode",true);
        AssistantEntitySql.Query productQuery=AssistantEntitySql.select(product);
        assertTrue(productQuery.getParameters().contains("%20公斤%"));
        assertTrue(productQuery.getParameters().contains("%20kg%"));
        assertTrue(parametersContain(productQuery,"20千克"));
        assertFalse(parametersContain(productQuery,"2[^[:space:]]*0[^[:space:]]*千"));
        assertFalse(parametersContain(productQuery,"2[^[:space:]]*0[^[:space:]]*k"));
        assertFalse(productQuery.getSql().contains("20kg"),"Unit aliases must remain bound parameters");

        Map<String,Object> customer=base("CUSTOMER","FUZZY");customer.put("entityAll",true);
        customer.put("entityKeyword","20千克");
        AssistantEntitySql.Query customerQuery=AssistantEntitySql.select(customer);
        assertFalse(customerQuery.getParameters().contains("%20kg%"));
        assertEquals(1,Collections.frequency(customerQuery.getParameters(),"20千克"));
    }

    @Test void productExactIdentifierModesFailClosedInsteadOfBypassingNameSearch() {
        Map<String,Object> context=base("PRODUCT","CODE");context.put("entityAll",true);
        context.put("entityKeyword","4升");context.put("searchCode",true);

        AssistantEntitySql.Query query=AssistantEntitySql.select(context);

        assertTrue(query.getSql().contains(" AND (1=0) "));
        assertFalse(query.getParameters().contains("4升"));
        assertFalse(query.getParameters().contains("4L"));
        assertFalse(query.getParameters().contains("%4L%"));
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

    @Test void everyProductDelimiterPreservesCrossTokenOrderAndUnitAliases() {
        for(String delimiter:Arrays.asList(" ","/","\\",",","，",";","；","+","|")) {
            Map<String,Object> context=base("PRODUCT","TOKEN_FUZZY");context.put("entityAll",true);
            context.put("entityKeyword","k40"+delimiter+"18升");

            AssistantEntitySql.Query query=AssistantEntitySql.select(context);

            assertTrue(hasRegex(query,"k40", "18升"),delimiter);
            assertTrue(hasRegex(query,"k[^[:space:]]*4[^[:space:]]*0", "18升"),delimiter);
            assertTrue(hasRegex(query,"k40", "18L"),delimiter);
            assertTrue(hasRegex(query,"k[^[:space:]]*4[^[:space:]]*0", "18L"),delimiter);
            assertFalse(parametersContain(query,"1[^[:space:]]*8[^[:space:]]*L"),delimiter);
            assertFalse(query.getParameters().contains("%k40%"),delimiter);
            assertFalse(query.getParameters().contains("%18升%"),delimiter);
            assertFalse(query.getParameters().contains("%18L%"),delimiter);
            assertFalse(query.getParameters().contains("%1%8%L%k%4%0%"),delimiter);
        }
    }

    @Test void productTokenRegexUsesDistinctOrderedNameSegmentsAndEscapesUserRegex() {
        Map<String,Object> context=base("PRODUCT","TOKEN_FUZZY");context.put("entityAll",true);
        context.put("entityKeyword","美孚 530 1升");

        AssistantEntitySql.Query query=AssistantEntitySql.select(context);

        assertTrue(hasRegex(query,"美[^[:space:]]*孚", "5[^[:space:]]*3[^[:space:]]*0", "1L"));
        assertTrue(parametersContain(query,"[[:space:]]+([^[:space:]]+[[:space:]]+)*"));
        assertFalse(parametersContain(query,".*美孚.*530"));

        Map<String,Object> escaped=base("PRODUCT","TOKEN_FUZZY");escaped.put("entityAll",true);
        escaped.put("entityKeyword","A(1) B[2]");
        AssistantEntitySql.Query escapedQuery=AssistantEntitySql.select(escaped);
        assertTrue(parametersContain(escapedQuery,"A\\(1\\)"));
        assertTrue(parametersContain(escapedQuery,"B\\[2\\]"));
        assertFalse(escapedQuery.getSql().contains("A(1)"));
    }

    @Test void salespersonSearchUsesNicknameAndAuthorizedDepartmentScope() {
        Map<String,Object> context=base("SALESPERSON","FUZZY");context.put("entityAll",false);
        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();
        assertTrue(sql.contains("FROM system_users e"));assertTrue(sql.contains("e.nickname LIKE CONCAT('%',?,'%')"));
        assertTrue(sql.contains("e.dept_id IN"));assertTrue(sql.contains("e.id=?"));
        assertFalse(sql.contains("e.code"));
    }

    @Test void purchaserSearchUsesSystemNicknameWithoutUserManagementFields() {
        Map<String,Object> context=base("PURCHASER","FUZZY");context.put("entityAll",false);
        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();
        assertTrue(sql.contains("FROM system_users e"));
        assertTrue(sql.contains("e.nickname LIKE CONCAT('%',?,'%')"));
        assertTrue(sql.contains("'采购员' description"));
        assertFalse(sql.contains("e.mobile"));assertFalse(sql.contains("e.password"));
    }

    @Test void userSearchUsesNicknameAndBothUserDepartmentRelationsWithoutSensitiveFields() {
        Map<String,Object> context=base("USER","FUZZY");context.put("entityAll",false);
        context.put("searchCode",false);

        AssistantEntitySql.Query query=AssistantEntitySql.select(context);String sql=query.getSql();

        assertTrue(sql.contains("FROM system_users e"));
        assertTrue(sql.contains("e.nickname LIKE CONCAT('%',?,'%')"));
        assertTrue(sql.contains("e.dept_id IN"));
        assertTrue(sql.contains("FROM system_user_dept sud"));
        assertTrue(sql.contains("sud.dept_id IN"));
        assertTrue(sql.contains("e.id=?"));
        assertFalse(sql.contains("e.mobile"));
        assertFalse(sql.contains("e.avatar"));
        assertFalse(sql.contains("e.username"));
    }

    private static boolean hasRegex(AssistantEntitySql.Query query,String... fragments) {
        for(Object parameter:query.getParameters()) {
            String value=String.valueOf(parameter);
            boolean matches=true;
            for(String fragment:fragments) matches&=value.contains(fragment);
            if(matches) return true;
        }
        return false;
    }

    private static boolean parametersContain(AssistantEntitySql.Query query,String fragment) {
        return query.getParameters().stream().map(String::valueOf).anyMatch(value->value.contains(fragment));
    }

    private static Map<String,Object> base(String type,String match) {
        Map<String,Object> context=new HashMap<>();context.put("mode","entityCandidates");context.put("entityType",type);
        context.put("entityMatchMode",match);context.put("tenantId",1L);context.put("entityKeyword","X");context.put("rowLimit",21);
        context.put("entityDeptIds",Collections.singleton(1L));context.put("entityWarehouseIds",Collections.singleton(2L));
        context.put("entitySelfUserId","3");context.put("entitySelfDeptId",1L);return context;
    }
}
