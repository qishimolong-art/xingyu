package cn.iocoder.yudao.module.erp.service.purchase.history;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history.ErpPurchaseHistoryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history.ErpPurchaseHistoryRespVO;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history.ErpPurchaseHistoryRespVO.TYPE_RETURN;

@Service
public class ErpPurchaseHistoryService {

    private static final String IN_PERMISSION = "erp:purchase-in:query";
    private static final String RETURN_PERMISSION = "erp:purchase-return:query";
    private static final String IN_MODULE = "erp_purchase_in";
    private static final String RETURN_MODULE = "erp_purchase_return";

    @Resource
    private ErpPurchaseHistoryRepository repository;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<ErpPurchaseHistoryRespVO> getPage(ErpPurchaseHistoryPageReqVO filter) {
        validate(filter);
        Long userId = getLoginUserId();
        if (userId == null) {
            return PageResult.empty();
        }
        boolean includePurchaseIn = permissionApi.hasAnyPermissions(userId, IN_PERMISSION);
        boolean includePurchaseReturn = permissionApi.hasAnyPermissions(userId, RETURN_PERMISSION);
        ErpFinanceVisibleScope purchaseInScope = includePurchaseIn ? resolveScope(userId, IN_MODULE) : null;
        ErpFinanceVisibleScope purchaseReturnScope = includePurchaseReturn ? resolveScope(userId, RETURN_MODULE) : null;
        includePurchaseIn = includePurchaseIn && hasAccess(purchaseInScope);
        includePurchaseReturn = includePurchaseReturn && hasAccess(purchaseReturnScope);
        if (!includePurchaseIn && !includePurchaseReturn) {
            return PageResult.empty();
        }
        ErpPurchaseHistoryRepository.Scope scope = new ErpPurchaseHistoryRepository.Scope(
                TenantContextHolder.getRequiredTenantId(), includePurchaseIn, includePurchaseReturn,
                purchaseInScope, purchaseReturnScope);
        long total = repository.count(filter, scope);
        if (total == 0) {
            return PageResult.empty();
        }
        long offset = (long) (filter.getPageNo() - 1) * filter.getPageSize();
        List<ErpPurchaseHistoryRespVO> rows = repository.page(filter, scope, offset, filter.getPageSize());
        maskProductPrices(rows);
        return new PageResult<>(rows, total);
    }

    private ErpFinanceVisibleScope resolveScope(Long userId, String module) {
        return ErpFinanceVisibleScope.from(permissionApi.getDeptDataPermission(userId, module), userId);
    }

    private boolean hasAccess(ErpFinanceVisibleScope scope) {
        return scope != null && scope.hasAccess();
    }

    private void maskProductPrices(List<ErpPurchaseHistoryRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<String> inHiddenFields = fieldPermissionMasker.getHiddenFieldSet(IN_MODULE);
        Set<String> returnHiddenFields = fieldPermissionMasker.getHiddenFieldSet(RETURN_MODULE);
        for (ErpPurchaseHistoryRespVO row : rows) {
            Set<String> hiddenFields = TYPE_RETURN.equals(row.getDocumentTypeCode())
                    ? returnHiddenFields : inHiddenFields;
            if (isProductPriceHidden(hiddenFields)) {
                row.setProductPrice(null);
            }
        }
    }

    private boolean isProductPriceHidden(Set<String> hiddenFields) {
        if (hiddenFields == null) {
            hiddenFields = Collections.emptySet();
        }
        return hiddenFields.contains("productPrice") || hiddenFields.contains("col_productPrice")
                || hiddenFields.contains("item_productPrice") || hiddenFields.contains("col_item_productPrice");
    }

    private void validate(ErpPurchaseHistoryPageReqVO filter) {
        if (filter.getBusinessTimeFrom() != null && filter.getBusinessTimeTo() != null
                && filter.getBusinessTimeFrom().isAfter(filter.getBusinessTimeTo())) {
            throw new ServiceException(400, "业务日期范围不正确");
        }
        if (filter.getBusinessTime() != null && filter.getBusinessTime().length > 2) {
            throw new ServiceException(400, "业务日期范围格式不正确");
        }
        if (filter.getOrderDirection() != null && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
    }
}
