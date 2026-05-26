package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierimage.ErpSupplierImageSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierImageDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierImageMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpSupplierImageServiceImpl implements ErpSupplierImageService {

    @Resource
    private ErpSupplierImageMapper supplierImageMapper;
    @Resource
    private ErpSupplierService supplierService;

    @Override
    public Long createSupplierImage(ErpSupplierImageSaveReqVO createReqVO) {
        validateSupplierExists(createReqVO.getSupplierId());
        ErpSupplierImageDO image = BeanUtils.toBean(createReqVO, ErpSupplierImageDO.class);
        supplierImageMapper.insert(image);
        return image.getId();
    }

    @Override
    public void updateSupplierImage(ErpSupplierImageSaveReqVO updateReqVO) {
        validateSupplierExists(updateReqVO.getSupplierId());
        validateSupplierImageExists(updateReqVO.getId());
        supplierImageMapper.updateById(BeanUtils.toBean(updateReqVO, ErpSupplierImageDO.class));
    }

    @Override
    public void deleteSupplierImage(Long id) {
        validateSupplierImageExists(id);
        supplierImageMapper.deleteById(id);
    }

    private void validateSupplierImageExists(Long id) {
        if (supplierImageMapper.selectById(id) == null) {
            throw exception(SUPPLIER_IMAGE_NOT_EXISTS);
        }
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierImageDO getSupplierImage(Long id) {
        return supplierImageMapper.selectById(id);
    }

    @Override
    public List<ErpSupplierImageDO> getSupplierImageList(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierImageMapper.selectListBySupplierId(supplierId);
    }

}
