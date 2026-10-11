package cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ErpCloudPrintPreviewSaleOutRespVO {

    private String previewToken;
    private LocalDateTime expiresAt;
    private List<Item> items;

    @Data
    @Accessors(chain = true)
    public static class Item {
        private String previewItemId;
        private Long warehouseId;
        private String warehouseName;
        private Long deviceId;
        private String printerName;
        private Integer copies;
        private String fileName;
        private String pdfSha256;
    }
}
