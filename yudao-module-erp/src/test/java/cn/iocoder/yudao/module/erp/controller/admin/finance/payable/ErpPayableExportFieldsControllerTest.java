package cn.iocoder.yudao.module.erp.controller.admin.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ErpPayableExportFieldsControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPayableAccountController payableAccountController;
    @InjectMocks
    private ErpPayableMiscController payableMiscController;

    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @Test
    void payableAccountExportFields_includeExtendedFieldsAndApplyPermission() {
        when(fieldPermissionMasker.getHiddenFields("erp_finance_payable_account"))
                .thenReturn(Collections.singleton("openingPayableBalance"));

        CommonResult<List<ErpExportFieldRespVO>> result = payableAccountController.getExportFields("overview");

        assertTrue(hasField(result.getData(), "otherPayableAmount"));
        assertFalse(hasField(result.getData(), "openingPayableBalance"));
    }

    @Test
    void payableMiscExportFields_includeTransferFieldsAndApplyPermission() {
        when(fieldPermissionMasker.getHiddenFields("erp_finance_payable_misc"))
                .thenReturn(Collections.singleton("generatedOffset"));

        CommonResult<List<ErpExportFieldRespVO>> result = payableMiscController.getExportFields();

        assertTrue(hasField(result.getData(), "pendingTransferAmount"));
        assertTrue(hasField(result.getData(), "transferAvailableAmount"));
        assertFalse(hasField(result.getData(), "generatedOffset"));
    }

    private static boolean hasField(List<ErpExportFieldRespVO> fields, String field) {
        return fields.stream().anyMatch(item -> field.equals(item.getField()));
    }
}
