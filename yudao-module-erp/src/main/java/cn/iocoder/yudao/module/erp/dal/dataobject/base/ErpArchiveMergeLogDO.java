package cn.iocoder.yudao.module.erp.dal.dataobject.base;

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

@TableName("erp_archive_merge_log")
@KeySequence("erp_archive_merge_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpArchiveMergeLogDO extends BaseDO {

    @TableId
    private Long id;

    private String archiveType;

    private Long sourceId;

    private Long keepId;

    private String affectedTables;

    private Integer affectedRows;

    private String remark;

}
