package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 销售手推车分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSaleCartPageReqVO extends PageParam {

    private String no;
    private Long customerId;
    private Long saleUserId;
    private Long deptId;
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] cartTime;
    private String contactPerson;
    private Long productId;
    private Integer status;
    private Boolean includeCompleted;
    private String remark;
    private List<Long> ids;
    private String orderField;
    private String orderDirection;

}
