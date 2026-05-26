package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.extend.ErpCustomerExtendInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendInfoDO;

import javax.validation.Valid;

/**
 * ERP 客户拓展信息（结构化） Service 接口
 */
public interface ErpCustomerExtendInfoService {

    /**
     * 根据客户编号获取拓展信息（不存在则返回 null）
     */
    ErpCustomerExtendInfoDO getByCustomerId(Long customerId);

    /**
     * 保存客户拓展信息：存在则更新、不存在则新增，返回记录编号。
     */
    Long saveExtendInfo(@Valid ErpCustomerExtendInfoSaveReqVO reqVO);

}
