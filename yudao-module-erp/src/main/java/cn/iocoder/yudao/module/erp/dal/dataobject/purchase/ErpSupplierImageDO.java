package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("erp_supplier_image")
@KeySequence("erp_supplier_image_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSupplierImageDO extends BaseDO {

    @TableId
    private Long id;
    private Long supplierId;
    private String imageType;
    private String imageName;
    private String imageUrl;
    private Boolean defaulted;
    private Integer sort;
    private String remark;

}
