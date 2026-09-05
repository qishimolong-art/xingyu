package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeItemDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpReceivableOtherIncomeItemMapper extends BaseMapperX<ErpReceivableOtherIncomeItemDO> {

    default List<ErpReceivableOtherIncomeItemDO> selectListByIncomeId(Long incomeId) {
        return selectList(new LambdaQueryWrapperX<ErpReceivableOtherIncomeItemDO>()
                .eq(ErpReceivableOtherIncomeItemDO::getIncomeId, incomeId)
                .orderByAsc(ErpReceivableOtherIncomeItemDO::getId));
    }

    default PageResult<ErpReceivableOtherIncomeItemDO> selectPageByIncomeId(ErpReceivableOtherIncomeItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpReceivableOtherIncomeItemDO> query = new LambdaQueryWrapperX<ErpReceivableOtherIncomeItemDO>()
                .eq(ErpReceivableOtherIncomeItemDO::getIncomeId, reqVO.getIncomeId());
        SFunction<ErpReceivableOtherIncomeItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpReceivableOtherIncomeItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpReceivableOtherIncomeItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpReceivableOtherIncomeItemDO> selectListByIncomeIds(Collection<Long> incomeIds) {
        return selectList(ErpReceivableOtherIncomeItemDO::getIncomeId, incomeIds);
    }

    default int deleteByIncomeId(Long incomeId) {
        return delete(ErpReceivableOtherIncomeItemDO::getIncomeId, incomeId);
    }

    default List<ErpReceivableOtherIncomeItemDO> selectListByItemNameOrInvoiceNo(String itemName, String invoiceNo) {
        return selectList(new LambdaQueryWrapperX<ErpReceivableOtherIncomeItemDO>()
                .likeIfPresent(ErpReceivableOtherIncomeItemDO::getItemName, itemName)
                .likeIfPresent(ErpReceivableOtherIncomeItemDO::getInvoiceNo, invoiceNo));
    }

    static SFunction<ErpReceivableOtherIncomeItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpReceivableOtherIncomeItemDO::getId;
            case "itemName":
                return ErpReceivableOtherIncomeItemDO::getItemName;
            case "amount":
                return ErpReceivableOtherIncomeItemDO::getAmount;
            case "invoiceNo":
                return ErpReceivableOtherIncomeItemDO::getInvoiceNo;
            case "party":
                return ErpReceivableOtherIncomeItemDO::getParty;
            case "customerId":
                return ErpReceivableOtherIncomeItemDO::getCustomerId;
            case "deptId":
                return ErpReceivableOtherIncomeItemDO::getDeptId;
            case "bizDate":
                return ErpReceivableOtherIncomeItemDO::getBizDate;
            case "handlerId":
                return ErpReceivableOtherIncomeItemDO::getHandlerId;
            case "qty":
                return ErpReceivableOtherIncomeItemDO::getQty;
            case "freightType":
                return ErpReceivableOtherIncomeItemDO::getFreightType;
            case "remark":
                return ErpReceivableOtherIncomeItemDO::getRemark;
            default:
                return null;
        }
    }
}
