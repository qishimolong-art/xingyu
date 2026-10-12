package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import lombok.Data;

import java.util.List;

@Data
public class ErpSaleCartItemMatchRespVO {

    private List<Match> matches;

    @Data
    public static class Match {

        private Long productId;
        private Long warehouseId;
        private String batchNo;
        private Boolean giftFlag;
        private Long survivorId;
        private Integer targetPageNo;
        private List<ErpSaleCartRespVO.Item> items;
    }
}
