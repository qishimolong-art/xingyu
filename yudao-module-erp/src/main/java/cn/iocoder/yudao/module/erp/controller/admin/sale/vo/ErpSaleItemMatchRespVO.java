package cn.iocoder.yudao.module.erp.controller.admin.sale.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class ErpSaleItemMatchRespVO {

    private List<Match> matches = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Match {
        private Integer keyIndex;
        private Long itemId;
        private Integer pageNo;
    }
}
