package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendDO;

import java.util.List;

public interface ErpCustomerExtendService {

    Long createExtend(ErpCustomerExtendSaveReqVO createReqVO);
    void updateExtend(ErpCustomerExtendSaveReqVO updateReqVO);
    void deleteExtend(Long id);
    ErpCustomerExtendDO getExtend(Long id);
    PageResult<ErpCustomerExtendDO> getExtendPage(ErpCustomerExtendPageReqVO pageReqVO);
    List<ErpCustomerExtendDO> getExtendListByCustomerId(Long customerId);

}
