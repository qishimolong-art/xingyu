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

import java.time.LocalDateTime;

@TableName("erp_print_record")
@KeySequence("erp_print_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPrintRecordDO extends BaseDO {

    @TableId
    private Long id;

    private String moduleKey;
    private Long businessId;
    private String businessNo;
    private Long templateId;
    private Long printerId;
    private String printerName;
    private LocalDateTime printTime;

}
