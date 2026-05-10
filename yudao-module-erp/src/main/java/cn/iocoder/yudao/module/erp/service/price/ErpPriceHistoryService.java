package cn.iocoder.yudao.module.erp.service.price;

import cn.iocoder.yudao.module.erp.dal.dataobject.price.ErpPriceHistoryDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 价格历史 Service 接口
 *
 * @author 汽配ERP
 */
public interface ErpPriceHistoryService {

    /**
     * 记录价格历史
     */
    void createPriceHistory(Long productId, Integer partnerType, Long partnerId,
                            BigDecimal price, BigDecimal count,
                            Integer bizType, Long bizId, String bizNo, LocalDateTime priceTime);

    /**
     * 获取最近成交价列表
     */
    List<ErpPriceHistoryDO> getPriceHistoryList(Long productId, Integer partnerType, Long partnerId);

    /**
     * 获取最近一次成交价
     */
    ErpPriceHistoryDO getLatestPrice(Long productId, Integer partnerType, Long partnerId);

}
