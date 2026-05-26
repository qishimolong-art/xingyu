package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierbusinessinfo.ErpSupplierBusinessInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierBusinessInfoDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierBusinessInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpSupplierBusinessInfoServiceImpl implements ErpSupplierBusinessInfoService {

    @Resource
    private ErpSupplierBusinessInfoMapper supplierBusinessInfoMapper;
    @Resource
    private ErpSupplierService supplierService;

    @Override
    public Long createSupplierBusinessInfo(ErpSupplierBusinessInfoSaveReqVO createReqVO) {
        validateSupplierExists(createReqVO.getSupplierId());
        ErpSupplierBusinessInfoDO businessInfo = BeanUtils.toBean(createReqVO, ErpSupplierBusinessInfoDO.class);
        supplierBusinessInfoMapper.insert(businessInfo);
        return businessInfo.getId();
    }

    @Override
    public void updateSupplierBusinessInfo(ErpSupplierBusinessInfoSaveReqVO updateReqVO) {
        validateSupplierExists(updateReqVO.getSupplierId());
        validateSupplierBusinessInfoExists(updateReqVO.getId());
        supplierBusinessInfoMapper.updateById(BeanUtils.toBean(updateReqVO, ErpSupplierBusinessInfoDO.class));
    }

    @Override
    public void deleteSupplierBusinessInfo(Long id) {
        validateSupplierBusinessInfoExists(id);
        supplierBusinessInfoMapper.deleteById(id);
    }

    private void validateSupplierBusinessInfoExists(Long id) {
        if (supplierBusinessInfoMapper.selectById(id) == null) {
            throw exception(SUPPLIER_BUSINESS_INFO_NOT_EXISTS);
        }
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierBusinessInfoDO getSupplierBusinessInfo(Long id) {
        return supplierBusinessInfoMapper.selectById(id);
    }

    @Override
    public List<ErpSupplierBusinessInfoDO> getSupplierBusinessInfoList(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierBusinessInfoMapper.selectListBySupplierId(supplierId);
    }

}
