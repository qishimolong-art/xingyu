package cn.iocoder.yudao.module.erp.api.sale.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * ERP sale cart draft create response DTO.
 */
@Data
public class ErpSaleCartDraftCreateRespDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String no;

    private Integer status;

}
