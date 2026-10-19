package cn.iocoder.yudao.module.erp.api.sale.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * ERP customer mini-app member authorization response DTO.
 */
@Data
public class ErpCustomerMemberAuthRespDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean authorized;

    private Long id;

    private Long customerId;

    private String customerName;

    /**
     * 客户价格级别，仅供后端建立小程序客户上下文使用。
     */
    private Integer priceLevel;

    private Long memberUserId;

    private String mobile;

    private Boolean priceVisible;

    private Boolean orderEnabled;

    public static ErpCustomerMemberAuthRespDTO unauthorized() {
        return new ErpCustomerMemberAuthRespDTO()
                .setAuthorized(false)
                .setPriceVisible(false)
                .setOrderEnabled(false);
    }

}
