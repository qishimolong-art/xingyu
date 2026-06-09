package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Defaults sales document metadata used by ERP data permissions and audit display.
 */
@Service
public class ErpSaleDocumentDefaultService {

    @Resource
    private AdminUserApi adminUserApi;

    public void fillCreateDefaults(ErpCustomerDO document) {
        Long loginUserId = getLoginUserId();
        if (document.getSaleUserId() == null) {
            document.setSaleUserId(loginUserId);
        }
        fillDeptId(document);
        fillCreateAuditDefaults(document);
    }

    public void fillCreateDefaults(ErpSaleQuoteDO document) {
        Long loginUserId = getLoginUserId();
        if (document.getSaleUserId() == null) {
            document.setSaleUserId(loginUserId);
        }
        fillDeptId(document);
        fillCreateAuditDefaults(document);
    }

    public void fillCreateDefaults(ErpSaleCartDO document) {
        Long loginUserId = getLoginUserId();
        if (document.getSaleUserId() == null) {
            document.setSaleUserId(loginUserId);
        }
        fillDeptId(document);
        fillCreateAuditDefaults(document);
    }

    public void fillCreateDefaults(ErpSaleOrderDO document) {
        Long loginUserId = getLoginUserId();
        if (document.getSaleUserId() == null) {
            document.setSaleUserId(loginUserId);
        }
        fillDeptId(document);
        fillCreateAuditDefaults(document);
    }

    public void fillCreateDefaults(ErpSaleOutDO document) {
        Long loginUserId = getLoginUserId();
        if (document.getSaleUserId() == null) {
            document.setSaleUserId(loginUserId);
        }
        fillDeptId(document);
        fillCreateAuditDefaults(document);
    }

    public void fillCreateDefaults(ErpSaleReturnDO document) {
        Long loginUserId = getLoginUserId();
        if (document.getSaleUserId() == null) {
            document.setSaleUserId(loginUserId);
        }
        fillDeptId(document);
        fillCreateAuditDefaults(document);
    }

    public void fillCreateDefaults(ErpSalePriceAdjustDO document) {
        Long loginUserId = getLoginUserId();
        if (document.getAdjustUserId() == null) {
            document.setAdjustUserId(loginUserId);
        }
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

    private void fillDeptId(ErpCustomerDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillDeptId(ErpSaleQuoteDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillDeptId(ErpSaleCartDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillDeptId(ErpSaleOrderDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillDeptId(ErpSaleOutDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillDeptId(ErpSaleReturnDO document) {
        if (document.getDeptId() == null) {
            document.setDeptId(getLoginUserDeptId());
        }
    }

    private void fillDeptId(ErpSalePriceAdjustDO document) {
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
