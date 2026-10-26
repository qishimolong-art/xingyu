package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - ERP 来源单据拣货详情 Response VO")
@Data
public class ErpSalePickSourceDetailRespVO {

    private ErpSalePickDeliverySummaryRespVO summary;
    private List<ErpSalePickTaskRespVO> pickTasks;

}
