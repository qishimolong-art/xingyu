package cn.iocoder.yudao.module.erp.dal.dataobject.config;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("erp_stock_select_price_config")
@KeySequence("erp_stock_select_price_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpStockSelectPriceConfigDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String fieldKey;

    private String bizType;

}
