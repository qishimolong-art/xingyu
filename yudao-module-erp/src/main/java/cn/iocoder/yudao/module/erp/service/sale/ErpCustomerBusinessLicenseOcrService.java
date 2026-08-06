package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.businesslicense.ErpCustomerBusinessLicenseOcrReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.businesslicense.ErpCustomerBusinessLicenseOcrRespVO;

import javax.validation.Valid;

public interface ErpCustomerBusinessLicenseOcrService {

    ErpCustomerBusinessLicenseOcrRespVO recognize(@Valid ErpCustomerBusinessLicenseOcrReqVO reqVO);

}
