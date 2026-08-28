package cn.iocoder.yudao.module.erp.api.sale.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * ERP sale cart draft create request DTO.
 */
@Data
public class ErpSaleCartDraftCreateReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @NotNull(message = "部门编号不能为空")
    private Long deptId;

    @NotNull(message = "来源类型不能为空")
    private Integer sourceType;

    @NotNull(message = "来源单据编号不能为空")
    private Long sourceId;

    @NotNull(message = "来源单据号不能为空")
    private String sourceNo;

    private String remark;

    @Valid
    @NotEmpty(message = "手推车明细不能为空")
    private List<Item> items;

    @Data
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotNull(message = "产品编号不能为空")
        private Long productId;

        @NotNull(message = "仓库编号不能为空")
        private Long warehouseId;

        @NotNull(message = "部门编号不能为空")
        private Long deptId;

        @NotNull(message = "产品数量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "产品数量必须大于 0")
        private BigDecimal count;

        @NotNull(message = "产品价格不能为空")
        @DecimalMin(value = "0", inclusive = true, message = "产品价格不能小于 0")
        private BigDecimal productPrice;

    }

}
