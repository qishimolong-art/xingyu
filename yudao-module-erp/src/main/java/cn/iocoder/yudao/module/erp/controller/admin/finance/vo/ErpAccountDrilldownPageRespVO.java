package cn.iocoder.yudao.module.erp.controller.admin.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Schema(description = "ERP 往来账户金额列下钻分页 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpAccountDrilldownPageRespVO<T> {

    private List<T> list = Collections.emptyList();
    private Long total = 0L;
    private ErpAccountDrilldownSummaryRespVO summary;
}
