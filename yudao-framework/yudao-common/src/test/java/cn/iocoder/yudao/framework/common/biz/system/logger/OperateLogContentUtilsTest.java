package cn.iocoder.yudao.framework.common.biz.system.logger;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OperateLogContentUtilsTest {

    @Data
    @AllArgsConstructor
    private static class FormRecord {

        private Long id;
        private String name;
        private String code;
        private BigDecimal price;

    }

    @Test
    public void testBuildErpFormCreateAction() {
        FormRecord record = new FormRecord(10L, "A", "C001", new BigDecimal("1.00"));

        String action = OperateLogContentUtils.buildErpFormCreateAction(record, record.getId(), record.getCode());

        assertEquals("数据库编号：10\n名称：A\n编码：C001", action);
    }

    @Test
    public void testBuildErpFormUpdateAction() {
        FormRecord oldRecord = new FormRecord(10L, "A", "C001", new BigDecimal("1.00"));
        FormRecord newRecord = new FormRecord(10L, "B", "C001", new BigDecimal("2.00"));

        String action = OperateLogContentUtils.buildErpFormUpdateAction(oldRecord, newRecord,
                newRecord.getId(), newRecord.getCode());

        assertEquals("数据库编号：10\n名称：B\n编码：C001\n变更明细：\nname：A->B；\nprice：1.00->2.00。", action);
    }

    @Test
    public void testBuildErpFormAction_convertExistingAction() {
        String oldAction = "修改配件信息；编号：10；名称：机油；编码：P001；变更明细：名称：旧->新；编码：A->B。";

        String action = OperateLogContentUtils.buildErpFormAction("修改", null, 10L, "P001", oldAction);

        assertEquals("数据库编号：10\n名称：机油\n编码：P001\n变更明细：\n名称：旧->新；\n编码：A->B。", action);
    }

}
