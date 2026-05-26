package cn.iocoder.yudao.module.erp.service.purchase;

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
        validateSupplierAccountExists(updateReqVO.getId());
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

    private void validateSupplierAccountExists(Long id) {
        if (supplierAccountMapper.selectById(id) == null) {
            throw exception(SUPPLIER_ACCOUNT_NOT_EXISTS);
        }
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierAccountDO getSupplierAccount(Long id) {
        return supplierAccountMapper.selectById(id);
    }

    @Override
    public List<ErpSupplierAccountDO> getSupplierAccountList(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierAccountMapper.selectListBySupplierId(supplierId);
    }

}
