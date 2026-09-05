package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - ERP 销售退货转单草稿创建 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpSaleReturnCreateTargetDraftRespVO {

    private Long id;

    private String no;

    private List<Long> ids;

    private List<String> nos;

    public static ErpSaleReturnCreateTargetDraftRespVO single(Long id, String no) {
        return new ErpSaleReturnCreateTargetDraftRespVO(id, no,
                id == null ? null : java.util.Collections.singletonList(id),
                no == null ? null : java.util.Collections.singletonList(no));
    }

    public static ErpSaleReturnCreateTargetDraftRespVO multiple(List<Long> ids, List<String> nos) {
        Long id = ids == null || ids.isEmpty() ? null : ids.get(0);
        String no = nos == null || nos.isEmpty() ? null : nos.get(0);
        return new ErpSaleReturnCreateTargetDraftRespVO(id, no, ids, nos);
    }

}
