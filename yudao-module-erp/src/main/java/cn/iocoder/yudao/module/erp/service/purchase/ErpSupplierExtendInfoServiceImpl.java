package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextendinfo.ErpSupplierExtendInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierExtendInfoDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierExtendInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;

@Service
@Validated
public class ErpSupplierExtendInfoServiceImpl implements ErpSupplierExtendInfoService {

    @Resource
    private ErpSupplierExtendInfoMapper supplierExtendInfoMapper;
    @Resource
    private ErpSupplierService supplierService;

    @Override
    public ErpSupplierExtendInfoDO getSupplierExtendInfo(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierExtendInfoMapper.selectBySupplierId(supplierId);
    }

    @Override
    public Long saveSupplierExtendInfo(ErpSupplierExtendInfoSaveReqVO saveReqVO) {
        validateSupplierExists(saveReqVO.getSupplierId());
        ErpSupplierExtendInfoDO old = supplierExtendInfoMapper.selectBySupplierId(saveReqVO.getSupplierId());
        ErpSupplierExtendInfoDO saveObj = BeanUtils.toBean(saveReqVO, ErpSupplierExtendInfoDO.class);
        if (old == null) {
            supplierExtendInfoMapper.insert(saveObj);
            return saveObj.getId();
        }
        saveObj.setId(old.getId());
        supplierExtendInfoMapper.updateById(saveObj);
        return old.getId();
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

}
