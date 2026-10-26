package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpAccountBalanceBO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ErpAccountControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpAccountController controller;

    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @Test
    void getDefaultAccount_returnsOnlyTheBoundedDefaultAccountWithBalance() {
        ErpAccountDO account = new ErpAccountDO()
                .setId(1L)
                .setName("基本户")
                .setDefaultStatus(true);
        when(accountService.getDefaultAccount()).thenReturn(account);
        when(accountService.getAccountBalanceMap(Collections.singletonList(1L)))
                .thenReturn(Collections.singletonMap(1L,
                        new ErpAccountBalanceBO().setAccountId(1L).setCurrentBalance(new BigDecimal("12.30"))));

        CommonResult<ErpAccountRespVO> result = controller.getDefaultAccount();

        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId()).isEqualTo(1L);
        assertThat(result.getData().getCurrentBalance()).isEqualByComparingTo("12.30");
    }

    @Test
    void getExportFields_supportsAccountAndTransactionScopes() {
        when(fieldPermissionMasker.getHiddenFields("erp_account")).thenReturn(Collections.emptySet());

        List<ErpExportFieldRespVO> accountFields = controller.getExportFields("account").getData();
        List<ErpExportFieldRespVO> transactionFields = controller.getExportFields("transaction").getData();

        assertThat(accountFields).extracting(ErpExportFieldRespVO::getField)
                .contains("name", "currentBalance", "updateTime");
        assertThat(transactionFields).extracting(ErpExportFieldRespVO::getField)
                .containsExactly("no", "billTypeName", "amount", "balance", "transactionTime", "bizNo", "remark");
    }
}
