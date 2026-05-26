package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConfigDO;

import javax.validation.Valid;
import java.util.List;

public interface ErpSaleConfigService {

    Long createSaleConfig(@Valid ErpSaleConfigSaveReqVO createReqVO);

    void updateSaleConfig(@Valid ErpSaleConfigSaveReqVO updateReqVO);

    void deleteSaleConfig(List<Long> ids);

    ErpSaleConfigDO getSaleConfig(Long id);

    PageResult<ErpSaleConfigDO> getSaleConfigPage(ErpSaleConfigPageReqVO pageReqVO);

    List<ErpSaleConfigDO> getSaleConfigSimpleList(String configType, Integer status);

}
