package cn.iocoder.yudao.module.erp.dal.dataobject.stock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 仓库分店关联 DO
 *
 * @author 芋道源码
 */
@TableName("erp_warehouse_branch")
@KeySequence("erp_warehouse_branch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpWarehouseBranchDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 仓库ID
     */
    private Long warehouseId;
    /**
     * 分店租户ID
     */
    private Long branchTenantId;

}
