package cn.iocoder.yudao.module.erp.service.sale.bo;

import lombok.Data;

/**
 * ERP customer mini-app member authorization context.
 */
@Data
public class ErpCustomerMemberAuthBO {

    private Long id;

    private Long customerId;

    private String customerName;

    private Long memberUserId;

    private String mobile;

    private Integer status;

}
