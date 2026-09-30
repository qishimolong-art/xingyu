package cn.iocoder.yudao.module.erp.controller.admin.finance;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class ErpMiscSettlementDisplayContractTest {

    @Test
    void receivableMiscDisplayUsesEffectiveReceiptSettlements() throws IOException {
        String controller = source("src/main/java/cn/iocoder/yudao/module/erp/controller/admin/finance/receivable/ErpReceivableMiscController.java");
        String respVo = source("src/main/java/cn/iocoder/yudao/module/erp/controller/admin/finance/receivable/vo/misc/ErpReceivableMiscRespVO.java");
        String exportVo = source("src/main/java/cn/iocoder/yudao/module/erp/controller/admin/finance/receivable/vo/misc/ErpReceivableMiscExportRespVO.java");
        String mapper = source("src/main/java/cn/iocoder/yudao/module/erp/dal/mysql/finance/receivable/ErpReceivableMiscMapper.java");
        String receiptService = source("src/main/java/cn/iocoder/yudao/module/erp/service/finance/ErpFinanceReceiptServiceImpl.java");

        assertThat(controller).contains(
                "selectSettlementAmountSumMapBySourceMiscIds",
                "ErpMiscTransferOffsetConstants.RECEIPT_OFFSET_SOURCE_TYPE",
                "vo.setBalanceAmount(originalAmount.subtract(settledAmount))",
                "vo.setGeneratedOffset(isGeneratedOffset(row))",
                "vo.setSettledAmount(null)",
                "vo.setBalanceAmount(null)");
        assertThat(respVo).contains("settledAmount", "balanceAmount", "sourceItemId", "sourceMiscId",
                "generatedOffset");
        assertThat(exportVo).contains("原金额", "已收金额", "剩余金额", "来源类型", "来源单号", "冲减原单号");
        assertThat(mapper).contains(
                "selectBySourceDocument",
                "selectSettlementTotals",
                "selectSettlementPage",
                "ErpMiscSettlementSql.");
        assertThat(receiptService).contains(
                "getSourceReceivableMiscId()",
                "validateAndFillSourceReceivableMisc(receipt, true)",
                "系统生成的其他应收冲减单不能转收款");
        assertThat(receiptService).doesNotContain(
                "createReceivableMiscOffset",
                "TRANSFER_RECEIVABLE_MISC_REMARK_PATTERN",
                "createReceivableMiscOffsetFromTransferRemark",
                "parseTransferReceivableMiscNo",
                "fallbackSourceItemId");
    }

    @Test
    void payableMiscDisplayUsesEffectivePaymentSettlements() throws IOException {
        String controller = source("src/main/java/cn/iocoder/yudao/module/erp/controller/admin/finance/payable/ErpPayableMiscController.java");
        String respVo = source("src/main/java/cn/iocoder/yudao/module/erp/controller/admin/finance/payable/vo/misc/ErpPayableMiscRespVO.java");
        String exportVo = source("src/main/java/cn/iocoder/yudao/module/erp/controller/admin/finance/payable/vo/misc/ErpPayableMiscExportRespVO.java");
        String mapper = source("src/main/java/cn/iocoder/yudao/module/erp/dal/mysql/finance/payable/ErpPayableMiscMapper.java");
        String paymentService = source("src/main/java/cn/iocoder/yudao/module/erp/service/finance/ErpFinancePaymentServiceImpl.java");

        assertThat(controller).contains(
                "selectSettlementAmountSumMapBySourceMiscIds",
                "ErpMiscTransferOffsetConstants.PAYMENT_OFFSET_SOURCE_TYPE",
                "vo.setBalanceAmount(originalAmount.subtract(settledAmount))",
                "vo.setGeneratedOffset(isGeneratedOffset(row))",
                "vo.setSettledAmount(null)",
                "vo.setBalanceAmount(null)");
        assertThat(respVo).contains("settledAmount", "balanceAmount", "sourceItemId", "sourceMiscId",
                "generatedOffset");
        assertThat(exportVo).contains("原金额", "已付金额", "剩余金额", "来源类型", "来源单号", "冲减原单号");
        assertThat(mapper).contains(
                "selectBySourceDocument",
                "selectSettlementTotals",
                "selectSettlementPage",
                "ErpMiscSettlementSql.");
        assertThat(paymentService).contains(
                "getSourcePayableMiscId()",
                "validateAndFillSourcePayableMisc(payment, true)",
                "系统生成的其他应付冲减单不能转付款");
        assertThat(paymentService).doesNotContain(
                "createPayableMiscOffset",
                "TRANSFER_PAYABLE_MISC_REMARK_PATTERN",
                "createPayableMiscOffsetFromTransferRemark",
                "parseTransferPayableMiscNo",
                "fallbackSourceItemId");
    }

    private String source(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

}
