package cn.iocoder.yudao.module.erp.service.product;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ERP_ITEM_BATCH_NO_REQUIRED;

/**
 * 配件批次号必填校验。
 */
@Component
public class ErpProductBatchNoValidator {

    public <T> void validateBatchNoRequired(List<T> items,
                                            Map<Long, ErpProductDO> productMap,
                                            Function<T, Long> productIdGetter,
                                            Function<T, String> batchNoGetter) {
        if (CollUtil.isEmpty(items) || CollUtil.isEmpty(productMap)) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            Long productId = productIdGetter.apply(item);
            ErpProductDO product = productMap.get(productId);
            if (product == null || !Boolean.TRUE.equals(product.getBatchNoEnabled())) {
                continue;
            }
            if (StrUtil.isBlank(batchNoGetter.apply(item))) {
                throw exception(ERP_ITEM_BATCH_NO_REQUIRED, i + 1);
            }
        }
    }

}
