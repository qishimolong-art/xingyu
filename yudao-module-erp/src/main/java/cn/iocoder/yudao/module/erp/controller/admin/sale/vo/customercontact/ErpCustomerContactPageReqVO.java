package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 客户联系人分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCustomerContactPageReqVO extends PageParam {

    @Schema(description = "客户编号")
    private Long customerId;

    @Schema(description = "联系人姓名")
    private String name;

    @Schema(description = "手机号")
    private String mobile;

}
