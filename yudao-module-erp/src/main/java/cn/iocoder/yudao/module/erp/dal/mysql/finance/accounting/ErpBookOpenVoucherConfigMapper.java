package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenVoucherConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 开账凭证类型勾选 Mapper
 *
 * @author Claude
 */
@Mapper
public interface ErpBookOpenVoucherConfigMapper extends BaseMapperX<ErpBookOpenVoucherConfigDO> {

    default List<ErpBookOpenVoucherConfigDO> selectListByBookOpenId(Long bookOpenId) {
        return selectList(ErpBookOpenVoucherConfigDO::getBookOpenId, bookOpenId);
    }

    default int deleteByBookOpenId(Long bookOpenId) {
        return delete(ErpBookOpenVoucherConfigDO::getBookOpenId, bookOpenId);
    }

    default ErpBookOpenVoucherConfigDO selectByBookOpenIdAndVoucherType(Long bookOpenId, Integer voucherType) {
        return selectOne(new LambdaQueryWrapperX<ErpBookOpenVoucherConfigDO>()
                .eq(ErpBookOpenVoucherConfigDO::getBookOpenId, bookOpenId)
                .eq(ErpBookOpenVoucherConfigDO::getVoucherType, voucherType));
    }

}
