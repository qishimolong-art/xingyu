package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierItemPageReqVO;
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
        ErpSupplierImageDO existing = validateSupplierImageExists(updateReqVO.getId());
        if (!java.util.Objects.equals(existing.getSupplierId(), updateReqVO.getSupplierId())) {
            throw exception(SUPPLIER_IMAGE_NOT_EXISTS);
        }
        supplierImageMapper.updateById(BeanUtils.toBean(updateReqVO, ErpSupplierImageDO.class));
    }

    @Override
    public void deleteSupplierImage(Long id) {
        validateSupplierImageExists(id);
        supplierImageMapper.deleteById(id);
    }

    private ErpSupplierImageDO validateSupplierImageExists(Long id) {
        ErpSupplierImageDO existing = supplierImageMapper.selectById(id);
        if (existing == null) throw exception(SUPPLIER_IMAGE_NOT_EXISTS);
        validateSupplierExists(existing.getSupplierId());
        return existing;
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierImageDO getSupplierImage(Long id) {
        return validateSupplierImageExists(id);
    }

    @Override
    public List<ErpSupplierImageDO> getSupplierImageList(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierImageMapper.selectListBySupplierId(supplierId);
    }


    @Override
    public PageResult<ErpSupplierImageDO> getSupplierImagePage(ErpSupplierItemPageReqVO reqVO) {
        validateSupplierExists(reqVO.getSupplierId());
        reqVO.boundPageSize();
        return supplierImageMapper.selectPage(reqVO);
    }
}
