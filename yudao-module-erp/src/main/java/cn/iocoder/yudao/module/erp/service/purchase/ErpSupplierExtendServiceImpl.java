package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierItemPageReqVO;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextend.ErpSupplierExtendSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierExtendDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierExtendMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpSupplierExtendServiceImpl implements ErpSupplierExtendService {

    @Resource
    private ErpSupplierExtendMapper supplierExtendMapper;
    @Resource
    private ErpSupplierService supplierService;

    @Override
    public Long createSupplierExtend(ErpSupplierExtendSaveReqVO createReqVO) {
        validateSupplierExists(createReqVO.getSupplierId());
        ErpSupplierExtendDO extend = BeanUtils.toBean(createReqVO, ErpSupplierExtendDO.class);
        supplierExtendMapper.insert(extend);
        return extend.getId();
    }

    @Override
    public void updateSupplierExtend(ErpSupplierExtendSaveReqVO updateReqVO) {
        validateSupplierExists(updateReqVO.getSupplierId());
        ErpSupplierExtendDO existing = validateSupplierExtendExists(updateReqVO.getId());
        if (!java.util.Objects.equals(existing.getSupplierId(), updateReqVO.getSupplierId())) {
            throw exception(SUPPLIER_EXTEND_NOT_EXISTS);
        }
        supplierExtendMapper.updateById(BeanUtils.toBean(updateReqVO, ErpSupplierExtendDO.class));
    }

    @Override
    public void deleteSupplierExtend(Long id) {
        validateSupplierExtendExists(id);
        supplierExtendMapper.deleteById(id);
    }

    private ErpSupplierExtendDO validateSupplierExtendExists(Long id) {
        ErpSupplierExtendDO existing = supplierExtendMapper.selectById(id);
        if (existing == null) throw exception(SUPPLIER_EXTEND_NOT_EXISTS);
        validateSupplierExists(existing.getSupplierId());
        return existing;
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierExtendDO getSupplierExtend(Long id) {
        return validateSupplierExtendExists(id);
    }

    @Override
    public List<ErpSupplierExtendDO> getSupplierExtendList(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierExtendMapper.selectListBySupplierId(supplierId);
    }


    @Override
    public PageResult<ErpSupplierExtendDO> getSupplierExtendPage(ErpSupplierItemPageReqVO reqVO) {
        validateSupplierExists(reqVO.getSupplierId());
        reqVO.boundPageSize();
        return supplierExtendMapper.selectPage(reqVO);
    }
}
