package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 凭证分录明细 Mapper
 *
 * @author Claude
 */
@Mapper
public interface ErpVoucherItemMapper extends BaseMapperX<ErpVoucherItemDO> {

    default List<ErpVoucherItemDO> selectListByVoucherId(Long voucherId) {
        return selectList(ErpVoucherItemDO::getVoucherId, voucherId);
    }

    default int deleteByVoucherId(Long voucherId) {
        return delete(ErpVoucherItemDO::getVoucherId, voucherId);
    }

    default Long selectCountBySubjectId(Long subjectId) {
        return selectCount(new LambdaQueryWrapperX<ErpVoucherItemDO>()
                .eq(ErpVoucherItemDO::getSubjectId, subjectId));
    }

}
