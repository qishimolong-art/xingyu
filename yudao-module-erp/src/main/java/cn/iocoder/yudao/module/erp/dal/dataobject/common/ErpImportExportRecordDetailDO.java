package cn.iocoder.yudao.module.erp.dal.dataobject.common;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ERP import/export detail record.
 */
@TableName("erp_import_export_record_detail")
@KeySequence("erp_import_export_record_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpImportExportRecordDetailDO extends BaseDO {

    @TableId
    private Long id;

    private Long recordId;
    private Integer rowNo;
    private String bizKey;
    private String bizName;
    private String failureReason;
    private String rawData;

}
