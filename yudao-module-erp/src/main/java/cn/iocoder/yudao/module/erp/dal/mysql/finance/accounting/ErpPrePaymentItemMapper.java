package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpPrePaymentItemMapper extends BaseMapperX<ErpPrePaymentItemDO> {

    default List<ErpPrePaymentItemDO> selectListByPrePaymentId(Long prePaymentId) {
        return selectList(ErpPrePaymentItemDO::getPrePaymentId, prePaymentId);
    }

    default int deleteByPrePaymentId(Long prePaymentId) {
        return delete(ErpPrePaymentItemDO::getPrePaymentId, prePaymentId);
    }

}
