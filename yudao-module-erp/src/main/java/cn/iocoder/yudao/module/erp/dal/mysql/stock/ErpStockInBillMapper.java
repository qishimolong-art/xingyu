package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpStockInBillMapper extends BaseMapperX<ErpStockInBillDO> {

    default PageResult<ErpStockInBillDO> selectPage(ErpStockInBillPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpStockInBillDO> wrapper = new LambdaQueryWrapperX<ErpStockInBillDO>()
                .likeIfPresent(ErpStockInBillDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpStockInBillDO::getBillDate, reqVO.getBillDate())
                .eqIfPresent(ErpStockInBillDO::getWarehouseId, reqVO.getWarehouseId())
                .likeIfPresent(ErpStockInBillDO::getSourceUnitName, reqVO.getSourceUnitName())
                .likeIfPresent(ErpStockInBillDO::getSourceNo, reqVO.getSourceNo())
                .eqIfPresent(ErpStockInBillDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpStockInBillDO::getPickupUserName, reqVO.getPickupUserName())
                .eqIfPresent(ErpStockInBillDO::getCreator, reqVO.getCreator());
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpStockInBillDO::getNo, ErpStockInBillDO::getSourceNo,
                ErpStockInBillDO::getSourceUnitName, ErpStockInBillDO::getPickupUserName,
                ErpStockInBillDO::getAuditorName, ErpStockInBillDO::getPriority,
                ErpStockInBillDO::getPickup, ErpStockInBillDO::getRemark);
        orderBy(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void orderBy(LambdaQueryWrapperX<ErpStockInBillDO> wrapper, ErpStockInBillPageReqVO reqVO) {
        SFunction<ErpStockInBillDO, ?> column = getOrderColumn(reqVO.getOrderField());
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        boolean desc = "desc".equalsIgnoreCase(reqVO.getOrderDirection());
        if (column != null && (asc || desc)) wrapper.orderBy(true, asc, column);
        wrapper.orderByDesc(ErpStockInBillDO::getId);
    }

    static SFunction<ErpStockInBillDO, ?> getOrderColumn(String field) {
        if (field == null) return null;
        switch (field.trim()) {
            case "priority": return ErpStockInBillDO::getPriority;
            case "no": return ErpStockInBillDO::getNo;
            case "pickup": return ErpStockInBillDO::getPickup;
            case "billDate": return ErpStockInBillDO::getBillDate;
            case "sourceNo": return ErpStockInBillDO::getSourceNo;
            case "status": return ErpStockInBillDO::getStatus;
            case "createTime": return ErpStockInBillDO::getCreateTime;
            case "updateTime": return ErpStockInBillDO::getUpdateTime;
            case "auditTime": return ErpStockInBillDO::getAuditTime;
            case "printTime": return ErpStockInBillDO::getPrintTime;
            case "printCount": return ErpStockInBillDO::getPrintCount;
            case "totalWeight": return ErpStockInBillDO::getTotalWeight;
            case "remark": return ErpStockInBillDO::getRemark;
            default: return null;
        }
    }

    default ErpStockInBillDO selectByNo(String no) {
        return selectOne(ErpStockInBillDO::getNo, no);
    }

    default List<ErpStockInBillDO> selectListBySource(Integer sourceBizType, Long sourceId) {
        return selectList(new LambdaQueryWrapperX<ErpStockInBillDO>()
                .eq(ErpStockInBillDO::getSourceBizType, sourceBizType)
                .eq(ErpStockInBillDO::getSourceId, sourceId)
                .orderByAsc(ErpStockInBillDO::getId));
    }

}
