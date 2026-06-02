package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 销售调价单 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpSalePriceAdjustMapper extends BaseMapperX<ErpSalePriceAdjustDO> {

    default PageResult<ErpSalePriceAdjustDO> selectPage(ErpSalePriceAdjustPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpSalePriceAdjustDO>()
                .likeIfPresent(ErpSalePriceAdjustDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpSalePriceAdjustDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpSalePriceAdjustDO::getCustomerId, reqVO.getCustomerId())
                .betweenIfPresent(ErpSalePriceAdjustDO::getAdjustDate, reqVO.getAdjustDate())
                .inIfPresent(ErpSalePriceAdjustDO::getId, reqVO.getIds())
                .orderByDesc(ErpSalePriceAdjustDO::getId));
    }

}
