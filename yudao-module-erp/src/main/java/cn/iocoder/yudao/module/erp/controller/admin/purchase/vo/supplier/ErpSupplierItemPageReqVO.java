package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/** Bounded supplier child collections. Legacy list endpoints remain compatible. */
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpSupplierItemPageReqVO extends PageParam {
    @NotNull @Positive
    private Long supplierId;

    public void boundPageSize() {
        if (getPageSize() == null || getPageSize() <= 0 || getPageSize() > 100) setPageSize(100);
    }
}
