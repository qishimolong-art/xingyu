package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface ErpStockOutBillMapper extends BaseMapperX<ErpStockOutBillDO> {

    default PageResult<ErpStockOutBillDO> selectPage(ErpStockOutBillPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpStockOutBillDO> wrapper = new LambdaQueryWrapperX<ErpStockOutBillDO>()
                .likeIfPresent(ErpStockOutBillDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpStockOutBillDO::getBillDate, reqVO.getBillDate())
                .eqIfPresent(ErpStockOutBillDO::getWarehouseId, reqVO.getWarehouseId())
                .likeIfPresent(ErpStockOutBillDO::getShippingArea, reqVO.getShippingArea())
                .likeIfPresent(ErpStockOutBillDO::getSourceUnitName, reqVO.getSourceUnitName())
                .likeIfPresent(ErpStockOutBillDO::getSourceNo, reqVO.getSourceNo())
                .eqIfPresent(ErpStockOutBillDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpStockOutBillDO::getPickUserName, reqVO.getPickUserName())
                .eqIfPresent(ErpStockOutBillDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpStockOutBillDO::getAuditor, reqVO.getAuditor());
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpStockOutBillDO::getNo, ErpStockOutBillDO::getSourceNo,
                ErpStockOutBillDO::getSourceUnitName, ErpStockOutBillDO::getWarehouseName,
                ErpStockOutBillDO::getShippingArea, ErpStockOutBillDO::getPickUserName,
                ErpStockOutBillDO::getAuditorName, ErpStockOutBillDO::getPriority,
                ErpStockOutBillDO::getPick, ErpStockOutBillDO::getSourceRemark,
                ErpStockOutBillDO::getRemark);
        orderBy(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void orderBy(LambdaQueryWrapperX<ErpStockOutBillDO> wrapper, ErpStockOutBillPageReqVO reqVO) {
        SFunction<ErpStockOutBillDO, ?> column = getOrderColumn(reqVO.getOrderField());
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        boolean desc = "desc".equalsIgnoreCase(reqVO.getOrderDirection());
        if (column != null && (asc || desc)) wrapper.orderBy(true, asc, column);
        wrapper.orderByDesc(ErpStockOutBillDO::getId);
    }

    static SFunction<ErpStockOutBillDO, ?> getOrderColumn(String field) {
        if (field == null) return null;
        switch (field.trim()) {
            case "priority": return ErpStockOutBillDO::getPriority;
            case "no": return ErpStockOutBillDO::getNo;
            case "pick": return ErpStockOutBillDO::getPick;
            case "billDate": return ErpStockOutBillDO::getBillDate;
            case "shippingArea": return ErpStockOutBillDO::getShippingArea;
            case "sourceNo": return ErpStockOutBillDO::getSourceNo;
            case "status": return ErpStockOutBillDO::getStatus;
            case "createTime": return ErpStockOutBillDO::getCreateTime;
            case "updateTime": return ErpStockOutBillDO::getUpdateTime;
            case "auditTime": return ErpStockOutBillDO::getAuditTime;
            case "printTime": return ErpStockOutBillDO::getPrintTime;
            case "printCount": return ErpStockOutBillDO::getPrintCount;
            case "totalWeight": return ErpStockOutBillDO::getTotalWeight;
            case "sourceRemark": return ErpStockOutBillDO::getSourceRemark;
            case "remark": return ErpStockOutBillDO::getRemark;
            default: return null;
        }
    }

    default ErpStockOutBillDO selectBySource(Integer sourceBizType, Long sourceId) {
        return selectOne(ErpStockOutBillDO::getSourceBizType, sourceBizType,
                ErpStockOutBillDO::getSourceId, sourceId);
    }

    default List<ErpStockOutBillDO> selectListBySource(Integer sourceBizType, Long sourceId) {
        return selectList(new LambdaQueryWrapperX<ErpStockOutBillDO>()
                .eq(ErpStockOutBillDO::getSourceBizType, sourceBizType)
                .eq(ErpStockOutBillDO::getSourceId, sourceId)
                .orderByAsc(ErpStockOutBillDO::getId));
    }

    default List<ErpStockOutBillDO> selectListBySources(Integer sourceBizType,
                                                        Collection<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpStockOutBillDO>()
                .eq(ErpStockOutBillDO::getSourceBizType, sourceBizType)
                .in(ErpStockOutBillDO::getSourceId, sourceIds)
                .orderByAsc(ErpStockOutBillDO::getId));
    }

    default ErpStockOutBillDO selectByNo(String no) {
        return selectOne(ErpStockOutBillDO::getNo, no);
    }

}
