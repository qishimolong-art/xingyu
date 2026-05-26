package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSupplierTaskMapper extends BaseMapperX<ErpSupplierTaskDO> {

    default List<ErpSupplierTaskDO> selectListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierTaskDO>()
                .eq(ErpSupplierTaskDO::getSupplierId, supplierId)
                .orderByDesc(ErpSupplierTaskDO::getYear)
                .orderByDesc(ErpSupplierTaskDO::getMonth)
                .orderByDesc(ErpSupplierTaskDO::getId));
    }

    default ErpSupplierTaskDO selectByUniqueKey(Long supplierId, Integer year, Integer month, String taskLevel) {
        LambdaQueryWrapperX<ErpSupplierTaskDO> wrapper = new LambdaQueryWrapperX<ErpSupplierTaskDO>()
                .eq(ErpSupplierTaskDO::getSupplierId, supplierId)
                .eq(ErpSupplierTaskDO::getYear, year)
                .eq(ErpSupplierTaskDO::getMonth, month);
        if (taskLevel == null) {
            wrapper.isNull(ErpSupplierTaskDO::getTaskLevel);
        } else {
            wrapper.eq(ErpSupplierTaskDO::getTaskLevel, taskLevel);
        }
        return selectOne(wrapper);
    }

}
