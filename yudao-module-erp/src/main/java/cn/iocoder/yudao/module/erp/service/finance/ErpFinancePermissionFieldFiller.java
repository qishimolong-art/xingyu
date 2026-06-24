package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * Fills columns used by ERP finance data-permission rules when creating records.
 */
@Component
public class ErpFinancePermissionFieldFiller {

    @Resource
    private AdminUserApi adminUserApi;

    public void fillCreateFields(ErpAccountDO account) {
        if (account.getDeptId() != null) {
            return;
        }
        account.setDeptId(getLoginUserDeptId());
    }

    public void fillCreateFields(ErpFinancePaymentDO payment) {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return;
        }
        if (payment.getFinanceUserId() == null) {
            payment.setFinanceUserId(loginUserId);
        }
        if (payment.getDeptId() == null) {
            payment.setDeptId(getUserDeptId(loginUserId));
        }
    }

    public void fillCreateFields(ErpFinanceReceiptDO receipt) {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return;
        }
        if (receipt.getFinanceUserId() == null) {
            receipt.setFinanceUserId(loginUserId);
        }
        if (receipt.getDeptId() == null) {
            receipt.setDeptId(getUserDeptId(loginUserId));
        }
    }

    public void fillCreateFields(ErpFinanceTransferDO transfer) {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return;
        }
        if (transfer.getFinanceUserId() == null) {
            transfer.setFinanceUserId(loginUserId);
        }
        if (transfer.getDeptId() == null) {
            transfer.setDeptId(getUserDeptId(loginUserId));
        }
    }

    private Long getLoginUserDeptId() {
        Long loginUserId = getLoginUserId();
        return loginUserId == null ? null : getUserDeptId(loginUserId);
    }

    private Long getUserDeptId(Long userId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user == null ? null : user.getDeptId();
    }

}
