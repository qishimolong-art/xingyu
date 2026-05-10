package cn.iocoder.yudao.module.erp.dal.dataobject.price;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 价格历史 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_price_history")
@KeySequence("erp_price_history_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPriceHistoryDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 产品编号
     */
    private Long productId;
    /**
     * 往来类型：1供应商 2客户
     */
    private Integer partnerType;
    /**
     * 往来对象ID
     */
    private Long partnerId;
    /**
     * 价格
     */
    private BigDecimal price;
    /**
     * 数量
     */
    private BigDecimal count;
    /**
     * 业务类型：1采购入库 2销售出库 3采购调价 4销售调价
     */
    private Integer bizType;
    /**
     * 业务单据ID
     */
    private Long bizId;
    /**
     * 业务单号
     */
    private String bizNo;
    /**
     * 价格时间
     */
    private LocalDateTime priceTime;

}
