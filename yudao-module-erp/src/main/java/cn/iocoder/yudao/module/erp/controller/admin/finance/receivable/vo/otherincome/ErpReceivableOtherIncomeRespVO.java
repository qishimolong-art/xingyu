package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErpReceivableOtherIncomeRespVO {

    private Long id;
    private String no;
    private Integer status;
    private LocalDateTime bizTime;
    private String settleMethod;
    private Long accountId;
    private String accountName;
    private String voucherNo;
    private String incomeType;
    private BigDecimal totalAmount;
    private Long deptId;
    private String deptName;
    private Long handlerId;
    private String handlerName;
    private String party;
    private String relatedBiz;
    private String docType;
    private String remark;
    private String fileUrl;
    private String creator;
    private String creatorName;
    private LocalDateTime createTime;
    private String updater;
    private String updaterName;
    private LocalDateTime updateTime;
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private String itemName;
        private BigDecimal amount;
        private String invoiceNo;
        private String party;
        private Long deptId;
        private String deptName;
        private LocalDateTime bizDate;
        private Long handlerId;
        private String handlerName;
        private Integer qty;
        private String freightType;
        private String remark;
        private String fileUrl;
    }
}
