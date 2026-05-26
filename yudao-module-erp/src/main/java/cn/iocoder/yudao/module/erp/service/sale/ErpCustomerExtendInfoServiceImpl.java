package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.extend.ErpCustomerExtendInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendInfoDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerExtendInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * ERP 客户拓展信息（结构化） ServiceImpl
 */
@Service
@Validated
public class ErpCustomerExtendInfoServiceImpl implements ErpCustomerExtendInfoService {

    @Resource
    private ErpCustomerExtendInfoMapper extendInfoMapper;
    @Resource
    private ErpCustomerService customerService;

    @Override
    public ErpCustomerExtendInfoDO getByCustomerId(Long customerId) {
        return extendInfoMapper.selectByCustomerId(customerId);
    }

    @Override
    public Long saveExtendInfo(ErpCustomerExtendInfoSaveReqVO reqVO) {
        customerService.validateCustomer(reqVO.getCustomerId());
        ErpCustomerExtendInfoDO existing = extendInfoMapper.selectByCustomerId(reqVO.getCustomerId());
        ErpCustomerExtendInfoDO toSave = BeanUtils.toBean(reqVO, ErpCustomerExtendInfoDO.class);
        if (existing != null) {
            toSave.setId(existing.getId());
            extendInfoMapper.updateById(toSave);
            return existing.getId();
        }
        toSave.setId(null);
        extendInfoMapper.insert(toSave);
        return toSave.getId();
    }

}
