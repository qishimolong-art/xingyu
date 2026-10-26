package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierItemPageReqVO;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplieraccount.ErpSupplierAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierAccountDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpSupplierAccountServiceImpl implements ErpSupplierAccountService {

    @Resource
    private ErpSupplierAccountMapper supplierAccountMapper;
    @Resource
    private ErpSupplierService supplierService;

    @Override
    public Long createSupplierAccount(ErpSupplierAccountSaveReqVO createReqVO) {
        validateSupplierExists(createReqVO.getSupplierId());
        if (Boolean.TRUE.equals(createReqVO.getDefaulted())) {
            supplierAccountMapper.clearDefaultBySupplierId(createReqVO.getSupplierId(), null);
        }
        ErpSupplierAccountDO account = BeanUtils.toBean(createReqVO, ErpSupplierAccountDO.class);
        supplierAccountMapper.insert(account);
        return account.getId();
    }

    @Override
    public void updateSupplierAccount(ErpSupplierAccountSaveReqVO updateReqVO) {
        validateSupplierExists(updateReqVO.getSupplierId());
        ErpSupplierAccountDO existing = validateSupplierAccountExists(updateReqVO.getId());
        if (!java.util.Objects.equals(existing.getSupplierId(), updateReqVO.getSupplierId())) {
            throw exception(SUPPLIER_ACCOUNT_NOT_EXISTS);
        }
        if (Boolean.TRUE.equals(updateReqVO.getDefaulted())) {
            supplierAccountMapper.clearDefaultBySupplierId(updateReqVO.getSupplierId(), updateReqVO.getId());
        }
        supplierAccountMapper.updateById(BeanUtils.toBean(updateReqVO, ErpSupplierAccountDO.class));
    }

    @Override
    public void deleteSupplierAccount(Long id) {
        validateSupplierAccountExists(id);
        supplierAccountMapper.deleteById(id);
    }

    private ErpSupplierAccountDO validateSupplierAccountExists(Long id) {
        ErpSupplierAccountDO existing = supplierAccountMapper.selectById(id);
        if (existing == null) throw exception(SUPPLIER_ACCOUNT_NOT_EXISTS);
        validateSupplierExists(existing.getSupplierId());
        return existing;
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierAccountDO getSupplierAccount(Long id) {
        return validateSupplierAccountExists(id);
    }

    @Override
    public List<ErpSupplierAccountDO> getSupplierAccountList(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierAccountMapper.selectListBySupplierId(supplierId);
    }


    @Override
    public PageResult<ErpSupplierAccountDO> getSupplierAccountPage(ErpSupplierItemPageReqVO reqVO) {
        validateSupplierExists(reqVO.getSupplierId());
        reqVO.boundPageSize();
        return supplierAccountMapper.selectPage(reqVO);
    }
}
