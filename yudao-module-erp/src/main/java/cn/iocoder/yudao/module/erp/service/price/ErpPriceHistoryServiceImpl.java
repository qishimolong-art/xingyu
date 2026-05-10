package cn.iocoder.yudao.module.erp.service.price;

import cn.iocoder.yudao.module.erp.dal.dataobject.price.ErpPriceHistoryDO;
import cn.iocoder.yudao.module.erp.dal.mysql.price.ErpPriceHistoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 价格历史 Service 实现类
 *
 * @author 汽配ERP
 */
@Service
@Validated
public class ErpPriceHistoryServiceImpl implements ErpPriceHistoryService {

    @Resource
    private ErpPriceHistoryMapper priceHistoryMapper;

    @Override
    public void createPriceHistory(Long productId, Integer partnerType, Long partnerId,
                                   BigDecimal price, BigDecimal count,
                                   Integer bizType, Long bizId, String bizNo, LocalDateTime priceTime) {
        ErpPriceHistoryDO historyDO = ErpPriceHistoryDO.builder()
                .productId(productId)
                .partnerType(partnerType)
                .partnerId(partnerId)
                .price(price)
                .count(count)
                .bizType(bizType)
                .bizId(bizId)
                .bizNo(bizNo)
                .priceTime(priceTime != null ? priceTime : LocalDateTime.now())
                .build();
        priceHistoryMapper.insert(historyDO);
    }

    @Override
    public List<ErpPriceHistoryDO> getPriceHistoryList(Long productId, Integer partnerType, Long partnerId) {
        return priceHistoryMapper.selectListByProductAndPartner(productId, partnerType, partnerId);
    }

    @Override
    public ErpPriceHistoryDO getLatestPrice(Long productId, Integer partnerType, Long partnerId) {
        return priceHistoryMapper.selectLatest(productId, partnerType, partnerId);
    }

}
