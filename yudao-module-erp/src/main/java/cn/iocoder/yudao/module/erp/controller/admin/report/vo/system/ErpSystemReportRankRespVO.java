package cn.iocoder.yudao.module.erp.controller.admin.report.vo.system;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class ErpSystemReportRankRespVO {

    private Long id;
    private String name;
    private String code;
    private String meta;
    private BigDecimal value;
    private BigDecimal inAmount;
    private BigDecimal saleAmount;
    private BigDecimal returnAmount;
    private BigDecimal netAmount;
    private BigDecimal pendingQty;
    private BigDecimal stockQty;
    private BigDecimal stockAmount;
    private Long count;

}
