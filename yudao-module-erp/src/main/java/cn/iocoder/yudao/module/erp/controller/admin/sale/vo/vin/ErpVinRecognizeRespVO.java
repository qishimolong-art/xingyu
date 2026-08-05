package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.vin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - ERP VIN 识别 Response VO")
@Data
public class ErpVinRecognizeRespVO {

    @Schema(description = "VIN 码")
    private String vin;

    @Schema(description = "接口是否返回成功")
    private Boolean success;

    @Schema(description = "接口消息")
    private String message;

    @Schema(description = "解析后的主要数据")
    private Map<String, Object> data;

    @Schema(description = "匹配车型列表，对应接口返回 model_list")
    private List<Map<String, Object>> modelList;

    @Schema(description = "接口原始返回")
    private Map<String, Object> raw;

}
