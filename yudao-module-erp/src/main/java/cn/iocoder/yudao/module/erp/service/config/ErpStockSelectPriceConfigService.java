package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpStockSelectPriceConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpStockSelectPriceConfigUpdateReqVO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ErpStockSelectPriceConfigService {

    String BIZ_TYPE_SALE = "sale";
    String BIZ_TYPE_PURCHASE = "purchase";

    ErpStockSelectPriceConfigRespVO getConfig();

    void updateConfig(@Valid ErpStockSelectPriceConfigUpdateReqVO reqVO);

    Set<String> getSceneHiddenPriceFields(String bizType);

    List<String> getEffectiveVisibleFields(String bizType, Long businessDeptId);

    void deleteByFieldKeys(Collection<String> fieldKeys);

}
