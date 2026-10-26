package cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.AssertTrue;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErpAssemblyOrderItemPageReqVO extends PageParam {

    @NotNull(message = "组装单编号不能为空")
    private Long orderId;

    @AssertTrue(message = "单次明细最多查询100条")
    public boolean isBoundedPageSize() { return getPageSize() != null && getPageSize() > 0 && getPageSize() <= 100; }

}
