package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierBillDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSupplierBillMapper extends BaseMapperX<ErpSupplierBillDO> {

    default List<ErpSupplierBillDO> selectListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierBillDO>()
                .eq(ErpSupplierBillDO::getSupplierId, supplierId)
                .orderByDesc(ErpSupplierBillDO::getBillDate)
                .orderByDesc(ErpSupplierBillDO::getId));
    }

}
