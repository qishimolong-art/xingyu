package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售手推车库存明细匹配 Request VO")
@Data
public class ErpSaleCartItemMatchReqVO {

    @NotNull(message = "销售手推车编号不能为空")
    private Long cartId;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须大于 0")
    @Max(value = 200, message = "每页条数不能超过 200")
    private Integer pageSize;

    @Valid
    @NotEmpty(message = "库存键不能为空")
    @Size(max = 200, message = "单次最多匹配 200 个库存键")
    private List<Key> keys;

    @Data
    public static class Key {

        @NotNull(message = "商品编号不能为空")
        private Long productId;

        @NotNull(message = "仓库编号不能为空")
        private Long warehouseId;

        private String batchNo;

        private Boolean giftFlag;
    }
}
