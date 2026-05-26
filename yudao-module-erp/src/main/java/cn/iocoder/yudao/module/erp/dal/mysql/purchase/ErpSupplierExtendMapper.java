package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierExtendDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSupplierExtendMapper extends BaseMapperX<ErpSupplierExtendDO> {

    default List<ErpSupplierExtendDO> selectListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierExtendDO>()
                .eq(ErpSupplierExtendDO::getSupplierId, supplierId)
                .orderByAsc(ErpSupplierExtendDO::getSort)
                .orderByDesc(ErpSupplierExtendDO::getId));
    }

}
