package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCustomerTaskPageReqVO extends PageParam {

    private Long customerId;
    private Integer year;
    private Integer month;
    private String taskLevel;

}
