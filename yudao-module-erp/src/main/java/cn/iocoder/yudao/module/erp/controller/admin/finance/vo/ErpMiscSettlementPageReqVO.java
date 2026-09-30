package cn.iocoder.yudao.module.erp.controller.admin.finance.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErpMiscSettlementPageReqVO extends PageParam {
    @NotNull
    private Long id;
    public ErpMiscSettlementPageReqVO() { setPageSize(20); }
}
