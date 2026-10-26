package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailLegacyExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailLegacyExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailLegacyExportRespVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class ErpFinanceApprovalExportSchemaTest {

    @Test
    void approvalExports_putApproveTimeFirstAndDocDateLast() throws Exception {
        assertColumn(ErpReceivableDetailRespVO.class, "approveTime", "审批日期", 0);
        assertColumn(ErpReceivableDetailRespVO.class, "docDate", "单据时间", 22);
        assertColumn(ErpPayableDetailRespVO.class, "approveTime", "审批日期", 0);
        assertColumn(ErpPayableDetailRespVO.class, "docDate", "单据时间", 14);
        assertColumn(ErpSettlementOffsetDetailExportRespVO.class, "approveTime", "审批日期", 0);
        assertColumn(ErpSettlementOffsetDetailExportRespVO.class, "docDate", "单据时间", 9);
    }

    @Test
    void legacyExports_keepOriginalBusinessDateHeaders() throws Exception {
        assertColumn(ErpReceivableDetailLegacyExportRespVO.class, "docDate", "日期", 1);
        assertThat(column(ErpPayableDetailLegacyExportRespVO.class, "docDate").value()).containsExactly("单据日期");
        assertThat(column(ErpSettlementOffsetDetailLegacyExportRespVO.class, "docDate").value())
                .containsExactly("单据日期");
    }

    private static void assertColumn(Class<?> type, String fieldName, String title, int index) throws Exception {
        ExcelProperty property = column(type, fieldName);
        assertThat(property.value()).containsExactly(title);
        assertThat(property.index()).isEqualTo(index);
    }

    private static ExcelProperty column(Class<?> type, String fieldName) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        return field.getAnnotation(ExcelProperty.class);
    }
}
