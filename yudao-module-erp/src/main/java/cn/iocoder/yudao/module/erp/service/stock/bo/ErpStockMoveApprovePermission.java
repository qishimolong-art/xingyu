package cn.iocoder.yudao.module.erp.service.stock.bo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErpStockMoveApprovePermission {

    private Boolean approveAllowed;

    private String approveDisabledReason;

    public static ErpStockMoveApprovePermission allowed() {
        return new ErpStockMoveApprovePermission(true, null);
    }

    public static ErpStockMoveApprovePermission denied(String reason) {
        return new ErpStockMoveApprovePermission(false, reason);
    }

    public static ErpStockMoveApprovePermission of(ErpStockMoveOperationPermission permission) {
        return new ErpStockMoveApprovePermission(permission.getAllowed(), permission.getDisabledReason());
    }

}
