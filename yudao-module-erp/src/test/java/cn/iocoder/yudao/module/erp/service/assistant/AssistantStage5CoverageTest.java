package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AssistantStage5CoverageTest {

    @Test void everyStage5GoldenFamilyHasAMigratedCapabilityAndFourQuadrants() throws Exception {
        Map<String,Set<String>> quadrants=new LinkedHashMap<>();Map<String,String> batches=new LinkedHashMap<>();
        for(String line:lines("assistant/assistant_stage1_golden_v1.psv")) {
            String[] values=line.split("\\|",-1);if(values.length<5 || !values[3].matches("5[A-E]")) continue;
            quadrants.computeIfAbsent(values[1],ignored->new LinkedHashSet<>()).add(values[4]);batches.put(values[1],values[3]);
        }
        Map<String,String[]> coverage=new LinkedHashMap<>();
        for(String line:lines("assistant/assistant_stage5_coverage.psv")) {
            String[] values=line.split("\\|",-1);assertEquals(5,values.length,line);coverage.put(values[0],values);
        }
        assertEquals(33,quadrants.size());assertEquals(quadrants.keySet(),coverage.keySet());
        for(Map.Entry<String,Set<String>> item:quadrants.entrySet()) {
            assertEquals(new LinkedHashSet<>(Arrays.asList("POSITIVE","BOUNDARY","NO_PERMISSION","FAILURE")),item.getValue(),item.getKey());
            String[] migrated=coverage.get(item.getKey());assertEquals(batches.get(item.getKey()),migrated[1]);
            assertFalse(migrated[2].trim().isEmpty());assertEquals("MIGRATED",migrated[3]);assertFalse(migrated[4].trim().isEmpty());
        }
    }

    @Test void allStage5ToolsAreRegisteredInBothRegistries() {
        AssistantToolRegistry tools=new AssistantToolRegistry();
        for(String tool:AssistantStage5BusinessQuery.TOOLS) assertTrue(tools.contains(tool),tool);
        AssistantCapabilityRegistry capabilities=new AssistantCapabilityRegistry();
        org.springframework.test.util.ReflectionTestUtils.setField(capabilities,"tools",tools);capabilities.validateAgainstToolRegistry();
        for(String tool:AssistantStage5BusinessQuery.TOOLS) assertTrue(capabilities.find(tool).isPresent(),tool);
    }

    private static List<String> lines(String resource) throws Exception {
        InputStream input=AssistantStage5CoverageTest.class.getClassLoader().getResourceAsStream(resource);assertNotNull(input,resource);
        List<String> result=new ArrayList<>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))) {
            String line;while((line=reader.readLine())!=null) if(!line.trim().isEmpty()&&!line.startsWith("#")) result.add(line);
        }
        return result;
    }
}
