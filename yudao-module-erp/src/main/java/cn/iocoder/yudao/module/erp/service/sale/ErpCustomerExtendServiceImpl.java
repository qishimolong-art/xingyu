package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerExtendMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_EXTEND_NOT_EXISTS;

@Service
@Validated
public class ErpCustomerExtendServiceImpl implements ErpCustomerExtendService {

    @Resource
    private ErpCustomerExtendMapper extendMapper;
    @Resource
    private ErpCustomerService customerService;

    @Override
    public Long createExtend(ErpCustomerExtendSaveReqVO createReqVO) {
        customerService.validateCustomer(createReqVO.getCustomerId());
        ErpCustomerExtendDO extend = BeanUtils.toBean(createReqVO, ErpCustomerExtendDO.class);
        extendMapper.insert(extend);
        return extend.getId();
    }

    @Override
    public void updateExtend(ErpCustomerExtendSaveReqVO updateReqVO) {
        ErpCustomerExtendDO existing = validateExtendExists(updateReqVO.getId());
        if (!java.util.Objects.equals(existing.getCustomerId(), updateReqVO.getCustomerId())) {
            throw exception(CUSTOMER_EXTEND_NOT_EXISTS);
        }
        customerService.validateCustomer(updateReqVO.getCustomerId());
        extendMapper.updateById(BeanUtils.toBean(updateReqVO, ErpCustomerExtendDO.class));
    }

    @Override
    public void deleteExtend(Long id) {
        validateExtendExists(id);
        extendMapper.deleteById(id);
    }

    private ErpCustomerExtendDO validateExtendExists(Long id) {
        ErpCustomerExtendDO existing = extendMapper.selectById(id);
        if (existing == null) throw exception(CUSTOMER_EXTEND_NOT_EXISTS);
        validateVisibleCustomer(existing.getCustomerId());
        return existing;
    }

    private void validateVisibleCustomer(Long customerId) {
        if (customerId == null || customerService.getCustomer(customerId) == null) {
            throw exception(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS);
        }
    }

    @Override
    public ErpCustomerExtendDO getExtend(Long id) {
        ErpCustomerExtendDO existing = extendMapper.selectById(id);
        if (existing != null) validateVisibleCustomer(existing.getCustomerId());
        return existing;
    }

    @Override
    public PageResult<ErpCustomerExtendDO> getExtendPage(ErpCustomerExtendPageReqVO pageReqVO) {
        validateVisibleCustomer(pageReqVO.getCustomerId());
        if (pageReqVO.getPageSize() <= 0 || pageReqVO.getPageSize() > 100) pageReqVO.setPageSize(100);
        return extendMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCustomerExtendDO> getExtendListByCustomerId(Long customerId) {
        validateVisibleCustomer(customerId);
        return extendMapper.selectListByCustomerId(customerId);
    }

}
