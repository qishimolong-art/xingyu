package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierItemPageReqVO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierAccountDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSupplierAccountMapper extends BaseMapperX<ErpSupplierAccountDO> {

    default PageResult<ErpSupplierAccountDO> selectPage(ErpSupplierItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpSupplierAccountDO>()
                .eq(ErpSupplierAccountDO::getSupplierId, reqVO.getSupplierId())
                .orderByAsc(ErpSupplierAccountDO::getSort)
                .orderByDesc(ErpSupplierAccountDO::getId));
    }

    default List<ErpSupplierAccountDO> selectListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierAccountDO>()
                .eq(ErpSupplierAccountDO::getSupplierId, supplierId)
                .orderByAsc(ErpSupplierAccountDO::getSort)
                .orderByDesc(ErpSupplierAccountDO::getId));
    }

    default void clearDefaultBySupplierId(Long supplierId, Long excludeId) {
        LambdaUpdateWrapper<ErpSupplierAccountDO> wrapper = new LambdaUpdateWrapper<ErpSupplierAccountDO>()
                .eq(ErpSupplierAccountDO::getSupplierId, supplierId)
                .set(ErpSupplierAccountDO::getDefaulted, false);
        if (excludeId != null) {
            wrapper.ne(ErpSupplierAccountDO::getId, excludeId);
        }
        update(null, wrapper);
    }

}
