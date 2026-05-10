package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCustomerAreaPageReqVO extends PageParam {

    private Long customerId;
    private String mapAddress;

}
