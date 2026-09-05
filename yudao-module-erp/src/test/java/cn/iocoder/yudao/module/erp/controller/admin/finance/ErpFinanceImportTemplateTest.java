package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.ErpPayableExpenseController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.ErpPayableOtherController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.ErpReceivableOtherController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.ErpReceivableOtherIncomeController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferImportExcelVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ErpFinanceImportTemplateTest {

    @Test
    void paymentTemplate_usesNamesAndHidesLegacyIds() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeImportTemplate(response, "付款单导入模板.xls", "付款单",
                ErpFinancePaymentImportExcelVO.class,
                Collections.singletonList(new ErpFinancePaymentImportExcelVO()),
                ErpFinancePaymentController.PAYMENT_IMPORT_TEMPLATE_FIELDS);

        Set<String> headers = readFirstRowHeaders(response.getContentAsByteArray());
        assertThat(headers).contains("财务人员", "所属部门", "供应商", "付款账户",
                "业务类型", "业务单号", "本次付款");
        assertThat(headers).doesNotContain("付款时间", "* 付款时间", "* 业务类型", "* 业务单号", "* 本次付款");
        assertThat(headers).doesNotContain("供应商ID", "付款账户ID", "财务人员ID", "所属部门ID", "业务ID");
    }

    @Test
    void receiptTemplate_usesNamesAndHidesLegacyIds() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeImportTemplate(response, "收款单导入模板.xls", "收款单",
                ErpFinanceReceiptImportExcelVO.class,
                Collections.singletonList(new ErpFinanceReceiptImportExcelVO()),
                ErpFinanceReceiptController.RECEIPT_IMPORT_TEMPLATE_FIELDS);

        Set<String> headers = readFirstRowHeaders(response.getContentAsByteArray());
        assertThat(headers).contains("财务人员", "所属部门", "客户", "收款账户",
                "业务类型", "业务单号", "本次收款");
        assertThat(headers).doesNotContain("收款时间", "* 收款时间", "* 业务类型", "* 业务单号", "* 本次收款");
        assertThat(headers).doesNotContain("客户ID", "收款账户ID", "财务人员ID", "所属部门ID", "业务ID");
    }

    @Test
    void transferTemplate_hidesMainDocumentTime() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeImportTemplate(response, "银行转账导入模板.xls", "银行转账",
                ErpFinanceTransferImportExcelVO.class,
                Collections.singletonList(new ErpFinanceTransferImportExcelVO()),
                ErpFinanceTransferController.TRANSFER_IMPORT_TEMPLATE_FIELDS);

        Set<String> headers = readFirstRowHeaders(response.getContentAsByteArray());
        assertThat(headers).contains("转出账户ID", "转入账户ID", "转账金额", "汇率");
        assertThat(headers).doesNotContain("转账时间", "* 转账时间");
    }

    @Test
    void receivableOtherTemplate_hidesMainDocumentDate() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeImportTemplate(response, "其他应收导入模板.xls", "其他应收",
                ErpReceivableOtherImportExcelVO.class,
                Collections.singletonList(new ErpReceivableOtherImportExcelVO()),
                ErpReceivableOtherController.RECEIVABLE_OTHER_IMPORT_TEMPLATE_FIELDS);

        Set<String> headers = readFirstRowHeaders(response.getContentAsByteArray());
        assertThat(headers).contains("客户ID", "应收金额", "调账原因备注");
        assertThat(headers).doesNotContain("业务日期", "* 业务日期");
    }

    @Test
    void payableOtherTemplate_hidesMainDocumentDate() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeImportTemplate(response, "其他应付导入模板.xls", "其他应付",
                ErpPayableOtherImportExcelVO.class,
                Collections.singletonList(new ErpPayableOtherImportExcelVO()),
                ErpPayableOtherController.PAYABLE_OTHER_IMPORT_TEMPLATE_FIELDS);

        Set<String> headers = readFirstRowHeaders(response.getContentAsByteArray());
        assertThat(headers).contains("供应商ID", "应付金额", "调账原因备注");
        assertThat(headers).doesNotContain("业务日期", "* 业务日期");
    }

    @Test
    void payableExpenseTemplate_hidesMainDateAndKeepsOptionalItemDate() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeImportTemplate(response, "费用支付导入模板.xls", "费用支付",
                ErpPayableExpenseImportExcelVO.class,
                Collections.singletonList(new ErpPayableExpenseImportExcelVO()),
                ErpPayableExpenseController.PAYABLE_EXPENSE_IMPORT_TEMPLATE_FIELDS);

        Set<String> headers = readFirstRowHeaders(response.getContentAsByteArray());
        assertThat(headers).contains("结算方式", "结算账户ID", "费用项目", "金额", "明细业务日期");
        assertThat(headers).doesNotContain("单据日期", "* 单据日期");
    }

    @Test
    void otherIncomeTemplate_hidesMainTimeAndKeepsOptionalItemTime() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeImportTemplate(response, "其他收入导入模板.xls", "其他收入",
                ErpReceivableOtherIncomeImportExcelVO.class,
                Collections.singletonList(new ErpReceivableOtherIncomeImportExcelVO()),
                ErpReceivableOtherIncomeController.OTHER_INCOME_IMPORT_TEMPLATE_FIELDS);

        Set<String> headers = readFirstRowHeaders(response.getContentAsByteArray());
        assertThat(headers).contains("结算方式", "结算账户ID", "收入项目", "金额", "明细客户ID", "明细业务时间");
        assertThat(headers).doesNotContain("业务时间", "* 业务时间");
    }

    @Test
    void mainDocumentDates_areNotExcelRequiredForOldTemplateCompatibility() throws Exception {
        assertNotExcelRequired(ErpFinancePaymentImportExcelVO.class, "paymentTime");
        assertNotExcelRequired(ErpFinanceReceiptImportExcelVO.class, "receiptTime");
        assertNotExcelRequired(ErpFinanceTransferImportExcelVO.class, "transferTime");
        assertNotExcelRequired(ErpReceivableOtherImportExcelVO.class, "bizTime");
        assertNotExcelRequired(ErpPayableOtherImportExcelVO.class, "bizTime");
        assertNotExcelRequired(ErpPayableExpenseImportExcelVO.class, "bizTime");
        assertNotExcelRequired(ErpReceivableOtherIncomeImportExcelVO.class, "bizTime");
    }

    private Set<String> readFirstRowHeaders(byte[] content) throws Exception {
        Set<String> headers = new LinkedHashSet<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
        }
        return headers;
    }

    private void assertNotExcelRequired(Class<?> excelClass, String fieldName) throws Exception {
        Field field = excelClass.getDeclaredField(fieldName);
        assertThat(field.getAnnotation(ExcelRequired.class)).isNull();
    }
}
