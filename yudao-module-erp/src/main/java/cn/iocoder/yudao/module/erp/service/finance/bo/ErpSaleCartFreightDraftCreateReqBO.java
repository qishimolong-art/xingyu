package cn.iocoder.yudao.module.erp.service.finance.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售手推车运费财务草稿创建参数。
 */
@Data
@Accessors(chain = true)
public class ErpSaleCartFreightDraftCreateReqBO {

    private Long cartId;
    private String cartNo;
    private LocalDate bizTime;
    private Long customerId;
    private String settleMethod;
    private Long accountId;
    private Long deptId;
    private Long handlerId;
    private String party;
    private BigDecimal amount;

}
