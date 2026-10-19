package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 付款单候选业务单据分页 Response VO")
@Data
public class ErpFinancePaymentFormCandidatePageRespVO {

    private List<ErpFinancePaymentFormCandidateRespVO> list;

    private Long total;

    private Integer autoWriteOffCount;

    private BigDecimal autoWriteOffAmount;

    private List<ErpFinancePaymentFormCandidatePageReqVO.SelectedItem> unavailableSelectedItems;
}
