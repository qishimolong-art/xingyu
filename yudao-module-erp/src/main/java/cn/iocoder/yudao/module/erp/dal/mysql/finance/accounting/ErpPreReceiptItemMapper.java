package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 预收款单明细 Mapper
 */
@Mapper
public interface ErpPreReceiptItemMapper extends BaseMapperX<ErpPreReceiptItemDO> {

    default List<ErpPreReceiptItemDO> selectListByPreReceiptId(Long preReceiptId) {
        return selectList(ErpPreReceiptItemDO::getPreReceiptId, preReceiptId);
    }

    default List<ErpPreReceiptItemDO> selectListByPreReceiptIds(Collection<Long> preReceiptIds) {
        return selectList(ErpPreReceiptItemDO::getPreReceiptId, preReceiptIds);
    }

}
