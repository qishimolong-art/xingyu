package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Defaults purchase document metadata that is used for display/search and the existing user-column data scope.
 */
@Service
public class ErpPurchaseDocumentDefaultService {

    @Resource
    private AdminUserApi adminUserApi;

    public void fillCreateDefaults(ErpPurchaseOrderDO document) {
        Long loginUserId = getLoginUserId();
        if (document.getPurchaser() == null) {
            document.setPurchaser(loginUserId);
        }
        fillDeptId(document);
    }

    public void fillCreateDefaults(ErpPurchaseInDO document) {
        fillDeptId(document);
    }

    public void fillCreateDefaults(ErpPurchaseReturnDO document) {
        Long loginUserId = getLoginUserId();
        if (document.getHandler() == null) {
            document.setHandler(loginUserId);
        }
        fillDeptId(document);
    }

    public void fillCreateDefaults(ErpPurchaseInvoiceDO document) {
        Long loginUserId = getLoginUserId();
        if (document.getHandlerId() == null) {
            document.setHandlerId(loginUserId);
        }
        fillDeptId(document);
    }

    public void fillCreateDefaults(ErpPurchasePriceAdjustDO document) {
        Long loginUserId = getLoginUserId();
        if (document.getAdjuster() == null) {
            document.setAdjuster(loginUserId);
        }
        fillDeptId(document);
    }

    public void fillCreateDefaults(ErpSupplierDO document) {
        fillDeptId(document);
        fillCreateAuditDefaults(document);
    }

    public void fillCreateAuditDefaults(Collection<? extends BaseDO> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        LocalDateTime current = LocalDateTime.now();
        String loginUserId = getLoginUserIdString();
        documents.forEach(document -> fillCreateAuditDefaults(document, current, loginUserId));
    }

    public void fillCreateAuditDefaults(BaseDO document) {
        if (document == null) {
            return;
        }
        fillCreateAuditDefaults(document, LocalDateTime.now(), getLoginUserIdString());
    }

    private void fillDeptId(ErpPurchaseOrderDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillDeptId(ErpPurchaseInDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillDeptId(ErpPurchaseReturnDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillDeptId(ErpPurchaseInvoiceDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillDeptId(ErpPurchasePriceAdjustDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillDeptId(ErpSupplierDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillCreateAuditDefaults(BaseDO document, LocalDateTime current, String loginUserId) {
        if (document.getCreateTime() == null) {
            document.setCreateTime(current);
        }
        if (document.getUpdateTime() == null) {
            document.setUpdateTime(current);
        }
        if (loginUserId != null && document.getCreator() == null) {
            document.setCreator(loginUserId);
        }
        if (loginUserId != null && document.getUpdater() == null) {
            document.setUpdater(loginUserId);
        }
    }

    private Long getLoginUserDeptId() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(loginUserId);
        return user == null ? null : user.getDeptId();
    }

    private Long getLoginUserId() {
        return SecurityFrameworkUtils.getLoginUserId();
    }

    private String getLoginUserIdString() {
        Long loginUserId = getLoginUserId();
        return loginUserId == null ? null : loginUserId.toString();
    }

}
