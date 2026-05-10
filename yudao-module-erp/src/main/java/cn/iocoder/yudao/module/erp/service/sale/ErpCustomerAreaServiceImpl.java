package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerAreaDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerAreaMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_AREA_NOT_EXISTS;

@Service
@Validated
public class ErpCustomerAreaServiceImpl implements ErpCustomerAreaService {

    @Resource
    private ErpCustomerAreaMapper areaMapper;
    @Resource
    private ErpCustomerService customerService;

    @Override
    public Long createArea(ErpCustomerAreaSaveReqVO createReqVO) {
        customerService.validateCustomer(createReqVO.getCustomerId());
        ErpCustomerAreaDO area = BeanUtils.toBean(createReqVO, ErpCustomerAreaDO.class);
        areaMapper.insert(area);
        return area.getId();
    }

    @Override
    public void updateArea(ErpCustomerAreaSaveReqVO updateReqVO) {
        validateAreaExists(updateReqVO.getId());
        customerService.validateCustomer(updateReqVO.getCustomerId());
        areaMapper.updateById(BeanUtils.toBean(updateReqVO, ErpCustomerAreaDO.class));
    }

    @Override
    public void deleteArea(Long id) {
        validateAreaExists(id);
        areaMapper.deleteById(id);
    }

    private void validateAreaExists(Long id) {
        if (areaMapper.selectById(id) == null) {
            throw exception(CUSTOMER_AREA_NOT_EXISTS);
        }
    }

    @Override
    public ErpCustomerAreaDO getArea(Long id) {
        return areaMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCustomerAreaDO> getAreaPage(ErpCustomerAreaPageReqVO pageReqVO) {
        return areaMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCustomerAreaDO> getAreaListByCustomerId(Long customerId) {
        customerService.validateCustomer(customerId);
        return areaMapper.selectListByCustomerId(customerId);
    }

}
