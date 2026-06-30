package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 客户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCustomerPageReqVO extends PageParam {

    @Schema(description = "客户名称", example = "张三")
    private String name;

    @Schema(description = "客户编码")
    private String code;

    @Schema(description = "联系人")
    private String contact;

    @Schema(description = "手机号码", example = "15601691300")
    private String mobile;

    @Schema(description = "联系电话", example = "15601691300")
    private String telephone;

    @Schema(description = "客户类型")
    private Integer customerType;

    @Schema(description = "区域编号")
    private Long areaId;

    @Schema(description = "线路编号")
    private Long routeId;

    @Schema(description = "所属业务员")
    private Long saleUserId;

    @Schema(description = "Sort field, supports: code, name, enterpriseMatchStatus, status, settleMethod, areaId, routeId, saleUserId, customerType, detailAddress, address, remark, creator, createTime, mobile")
    private String orderField;

    @Schema(description = "Sort direction: asc or desc")
    private String orderDirection;

    @Schema(description = "客户编号数组")
    private List<Long> ids;

}
