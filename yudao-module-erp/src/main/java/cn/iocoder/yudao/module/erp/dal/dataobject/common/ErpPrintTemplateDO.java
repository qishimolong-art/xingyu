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

@TableName("erp_print_template")
@KeySequence("erp_print_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPrintTemplateDO extends BaseDO {

    @TableId
    private Long id;

    private String moduleKey;
    private String name;
    private Boolean defaulted;
    private Integer status;
    private String templateJson;
    private String paperConfig;

}
