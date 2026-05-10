package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerAreaDO;

import java.util.List;

public interface ErpCustomerAreaService {

    Long createArea(ErpCustomerAreaSaveReqVO createReqVO);
    void updateArea(ErpCustomerAreaSaveReqVO updateReqVO);
    void deleteArea(Long id);
    ErpCustomerAreaDO getArea(Long id);
    PageResult<ErpCustomerAreaDO> getAreaPage(ErpCustomerAreaPageReqVO pageReqVO);
    List<ErpCustomerAreaDO> getAreaListByCustomerId(Long customerId);

}
