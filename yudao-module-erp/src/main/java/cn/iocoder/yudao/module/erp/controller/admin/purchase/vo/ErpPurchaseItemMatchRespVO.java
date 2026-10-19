package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购单据整单明细匹配 Response VO")
@Data
public class ErpPurchaseItemMatchRespVO {

    private List<Match> matches = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Match {
        @Schema(description = "请求键下标")
        private Integer keyIndex;
        @Schema(description = "已存在明细编号")
        private Long itemId;
        @Schema(description = "默认排序下所在页码")
        private Integer pageNo;
    }
}
