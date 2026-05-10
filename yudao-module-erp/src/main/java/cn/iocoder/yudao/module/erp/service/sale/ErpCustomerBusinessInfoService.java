package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerBusinessInfoDO;

import java.util.List;

public interface ErpCustomerBusinessInfoService {

    Long createBusinessInfo(ErpCustomerBusinessInfoSaveReqVO createReqVO);
    void updateBusinessInfo(ErpCustomerBusinessInfoSaveReqVO updateReqVO);
    void deleteBusinessInfo(Long id);
    ErpCustomerBusinessInfoDO getBusinessInfo(Long id);
    PageResult<ErpCustomerBusinessInfoDO> getBusinessInfoPage(ErpCustomerBusinessInfoPageReqVO pageReqVO);
    List<ErpCustomerBusinessInfoDO> getBusinessInfoListByCustomerId(Long customerId);

}
