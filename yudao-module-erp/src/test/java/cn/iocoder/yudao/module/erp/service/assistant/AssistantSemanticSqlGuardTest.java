package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AssistantSemanticSqlGuardTest {
    private AssistantSemanticSqlGuard guard;

    @BeforeEach
    void setUp() {
        AssistantProperties properties=new AssistantProperties();
        properties.getOrchestration().setPublishedDatasets(Arrays.asList("purchase_orders","inventory_current"));
        AssistantSemanticCatalog catalog=new AssistantSemanticCatalog();
        ReflectionTestUtils.setField(catalog,"properties",properties);
        guard=new AssistantSemanticSqlGuard();
        ReflectionTestUtils.setField(guard,"catalog",catalog);
    }

    @Test
    void acceptsOnePublishedDatasetNamedParametersAliasesAndBoundedLimit() {
        AssistantSemanticSqlGuard.Validated result=guard.validate(
                "SELECT p.supplier_name, COUNT(*) document_count FROM purchase_orders p "
                        +"WHERE p.order_time >= :p1 GROUP BY p.supplier_name ORDER BY document_count DESC LIMIT 10");
        assertEquals(1,result.getDatasets().size());
        assertTrue(result.getDatasets().contains("purchase_orders"));
        assertEquals(new java.util.LinkedHashSet<>(Arrays.asList("supplier_name","order_time")),result.getColumns());
        assertEquals(Collections.singleton("p1"),result.getParameters());
        assertEquals(64,result.getFingerprint().length());
    }

    @Test
    void appendsMaximumLimitWhenModelOmitsIt() {
        assertTrue(guard.validate("SELECT p.id FROM purchase_orders p").getSql().endsWith("LIMIT 200"));
    }

    @Test
    void rejectsUnsafeOrUnpublishedSql() {
        assertRejected("SELECT * FROM purchase_orders p");
        assertRejected("SELECT p.id FROM purchase_orders p UNION SELECT p.id FROM purchase_orders p");
        assertRejected("SELECT p.id FROM purchase_orders p WHERE p.id IN (SELECT x.id FROM inventory_current x)");
        assertRejected("SELECT p.id FROM purchase_orders p WHERE p.no='PO-1'");
        assertRejected("SELECT p.id FROM purchase_orders p WHERE p.id=:tenantId");
        assertRejected("SELECT p.id FROM purchase_orders p JOIN purchase_orders q ON q.id=p.id");
        assertRejected("SELECT p.remark FROM purchase_orders p");
        assertEquals("DATASET_UNPUBLISHED",assertThrows(AssistantFailure.class,
                ()->guard.validate("SELECT p.id FROM unknown_dataset p")).getCode());
        assertRejected("DELETE FROM purchase_orders");
        assertRejected("SELECT p.id FROM purchase_orders p; SELECT 1");
        assertRejected("SELECT p.id FROM purchase_orders p LIMIT 201");
        assertRejected("SELECT p.id FROM purchase_orders p LIMIT 10 OFFSET 10001");
    }

    private void assertRejected(String sql) {
        AssistantFailure failure=assertThrows(AssistantFailure.class,()->guard.validate(sql));
        assertEquals("SQL_REJECTED",failure.getCode());
    }
}
