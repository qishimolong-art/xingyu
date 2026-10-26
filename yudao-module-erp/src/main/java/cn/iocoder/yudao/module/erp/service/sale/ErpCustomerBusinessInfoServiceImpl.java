package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerBusinessInfoDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerBusinessInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_BUSINESS_INFO_NOT_EXISTS;

/**
 * ERP 客户工商信息 ServiceImpl
 *
 * 说明：当前阶段不接第三方工商 API（如天眼查、企查查），仅维护本地人工录入字段。
 * 后续如需接入，在此类扩展 fetchFromThirdParty 方法即可。
 */
@Service
@Validated
public class ErpCustomerBusinessInfoServiceImpl implements ErpCustomerBusinessInfoService {

    @Resource
    private ErpCustomerBusinessInfoMapper businessInfoMapper;
    @Resource
    private ErpCustomerService customerService;

    @Override
    public Long createBusinessInfo(ErpCustomerBusinessInfoSaveReqVO createReqVO) {
        customerService.validateCustomer(createReqVO.getCustomerId());
        ErpCustomerBusinessInfoDO businessInfo = BeanUtils.toBean(createReqVO, ErpCustomerBusinessInfoDO.class);
        businessInfoMapper.insert(businessInfo);
        return businessInfo.getId();
    }

    @Override
    public void updateBusinessInfo(ErpCustomerBusinessInfoSaveReqVO updateReqVO) {
        ErpCustomerBusinessInfoDO existing = validateBusinessInfoExists(updateReqVO.getId());
        if (!java.util.Objects.equals(existing.getCustomerId(), updateReqVO.getCustomerId())) {
            throw exception(CUSTOMER_BUSINESS_INFO_NOT_EXISTS);
        }
        customerService.validateCustomer(updateReqVO.getCustomerId());
        businessInfoMapper.updateById(BeanUtils.toBean(updateReqVO, ErpCustomerBusinessInfoDO.class));
    }

    @Override
    public void deleteBusinessInfo(Long id) {
        validateBusinessInfoExists(id);
        businessInfoMapper.deleteById(id);
    }

    private ErpCustomerBusinessInfoDO validateBusinessInfoExists(Long id) {
        ErpCustomerBusinessInfoDO existing = businessInfoMapper.selectById(id);
        if (existing == null) throw exception(CUSTOMER_BUSINESS_INFO_NOT_EXISTS);
        validateVisibleCustomer(existing.getCustomerId());
        return existing;
    }

    private void validateVisibleCustomer(Long customerId) {
        if (customerId == null || customerService.getCustomer(customerId) == null) {
            throw exception(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS);
        }
    }

    @Override
    public ErpCustomerBusinessInfoDO getBusinessInfo(Long id) {
        ErpCustomerBusinessInfoDO existing = businessInfoMapper.selectById(id);
        if (existing != null) validateVisibleCustomer(existing.getCustomerId());
        return existing;
    }

    @Override
    public PageResult<ErpCustomerBusinessInfoDO> getBusinessInfoPage(ErpCustomerBusinessInfoPageReqVO pageReqVO) {
        validateVisibleCustomer(pageReqVO.getCustomerId());
        if (pageReqVO.getPageSize() <= 0 || pageReqVO.getPageSize() > 100) pageReqVO.setPageSize(100);
        return businessInfoMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCustomerBusinessInfoDO> getBusinessInfoListByCustomerId(Long customerId) {
        validateVisibleCustomer(customerId);
        return businessInfoMapper.selectListByCustomerId(customerId);
    }

}
