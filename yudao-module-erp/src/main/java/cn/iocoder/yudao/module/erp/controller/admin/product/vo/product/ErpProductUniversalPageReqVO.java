package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Max;
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpProductUniversalPageReqVO extends PageParam {
    @NotNull private Long productId;
    @Override @Max(100) public Integer getPageSize() { return super.getPageSize(); }
}
