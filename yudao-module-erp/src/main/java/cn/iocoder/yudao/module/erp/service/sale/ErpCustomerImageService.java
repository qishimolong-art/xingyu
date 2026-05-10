package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImagePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImageSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerImageDO;

import java.util.List;

public interface ErpCustomerImageService {

    Long createImage(ErpCustomerImageSaveReqVO createReqVO);
    void updateImage(ErpCustomerImageSaveReqVO updateReqVO);
    void deleteImage(Long id);
    ErpCustomerImageDO getImage(Long id);
    PageResult<ErpCustomerImageDO> getImagePage(ErpCustomerImagePageReqVO pageReqVO);
    List<ErpCustomerImageDO> getImageListByCustomerId(Long customerId);

}
