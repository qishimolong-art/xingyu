package cn.iocoder.yudao.module.erp.service.stock.bo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErpStockMoveOperationPermission {

    private Boolean allowed;

    private String disabledReason;

    public static ErpStockMoveOperationPermission allowed() {
        return new ErpStockMoveOperationPermission(true, null);
    }

    public static ErpStockMoveOperationPermission denied(String reason) {
        return new ErpStockMoveOperationPermission(false, reason);
    }

}
