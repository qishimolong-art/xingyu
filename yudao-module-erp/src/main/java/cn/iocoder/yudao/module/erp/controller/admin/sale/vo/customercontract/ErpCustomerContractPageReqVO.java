package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 客户合同分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCustomerContractPageReqVO extends PageParam {

    private Long customerId;
    private String contractNo;
    private String contractType;

}
