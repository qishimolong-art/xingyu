package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 销售调价单 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpSalePriceAdjustMapper extends BaseMapperX<ErpSalePriceAdjustDO> {

    default PageResult<ErpSalePriceAdjustDO> selectPage(ErpSalePriceAdjustPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSalePriceAdjustDO> wrapper = new LambdaQueryWrapperX<ErpSalePriceAdjustDO>()
                .likeIfPresent(ErpSalePriceAdjustDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpSalePriceAdjustDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpSalePriceAdjustDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSalePriceAdjustDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpSalePriceAdjustDO::getAdjustDate, reqVO.getAdjustDate())
                .inIfPresent(ErpSalePriceAdjustDO::getId, reqVO.getIds());
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<ErpSalePriceAdjustDO> wrapper,
                                 ErpSalePriceAdjustPageReqVO reqVO) {
        SFunction<ErpSalePriceAdjustDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpSalePriceAdjustDO::getId);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn);
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn);
            return;
        }
        wrapper.orderByDesc(ErpSalePriceAdjustDO::getId);
    }

    static SFunction<ErpSalePriceAdjustDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpSalePriceAdjustDO::getNo;
            case "customerId":
            case "customerName":
                return ErpSalePriceAdjustDO::getCustomerId;
            case "adjustDate":
                return ErpSalePriceAdjustDO::getAdjustDate;
            case "status":
                return ErpSalePriceAdjustDO::getStatus;
            case "totalAdjustPrice":
                return ErpSalePriceAdjustDO::getTotalAdjustPrice;
            case "creator":
            case "creatorName":
                return ErpSalePriceAdjustDO::getCreator;
            case "createTime":
                return ErpSalePriceAdjustDO::getCreateTime;
            case "updater":
            case "updaterName":
                return ErpSalePriceAdjustDO::getUpdater;
            case "updateTime":
                return ErpSalePriceAdjustDO::getUpdateTime;
            case "adjustUserId":
            case "adjustUserName":
                return ErpSalePriceAdjustDO::getAdjustUserId;
            default:
                return null;
        }
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpSalePriceAdjustDO::getCustomerId, customerId);
    }

}
