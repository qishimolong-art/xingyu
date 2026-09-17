package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "ERP 其他收入草稿新增/修改 Request VO")
@Data
@Accessors(chain = true)
public class ErpReceivableOtherIncomeDraftSaveReqVO {

    private Long id;
    private LocalDateTime bizTime;
    private String settleMethod;
    private Long accountId;
    private String voucherNo;
    private String incomeType;
    private Long deptId;
    private Long handlerId;
    private String party;
    private String relatedBiz;
    private String docType;
    private String remark;
    private String fileUrl;
    private List<Item> items;

    @Data
    @Accessors(chain = true)
    public static class Item {
        private Long id;
        private String operation;
        private String itemName;
        private BigDecimal amount;
        private String invoiceNo;
        private String party;
        private Long customerId;
        private Long deptId;
        private LocalDateTime bizDate;
        private Long handlerId;
        private Integer qty;
        private String freightType;
        private String remark;
        private String fileUrl;
    }
}
