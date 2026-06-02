package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuotePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 报价订单 Mapper
 */
@Mapper
public interface ErpSaleQuoteMapper extends BaseMapperX<ErpSaleQuoteDO> {

    default PageResult<ErpSaleQuoteDO> selectPage(ErpSaleQuotePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpSaleQuoteDO>()
                .likeIfPresent(ErpSaleQuoteDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpSaleQuoteDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSaleQuoteDO::getSaleUserId, reqVO.getSaleUserId())
                .betweenIfPresent(ErpSaleQuoteDO::getQuoteTime, reqVO.getQuoteTime())
                .eqIfPresent(ErpSaleQuoteDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpSaleQuoteDO::getRemark, reqVO.getRemark())
                .inIfPresent(ErpSaleQuoteDO::getId, reqVO.getIds())
                .orderByDesc(ErpSaleQuoteDO::getId));
    }

    default ErpSaleQuoteDO selectByNo(String no) {
        return selectOne(ErpSaleQuoteDO::getNo, no);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpSaleQuoteDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpSaleQuoteDO>()
                .eq(ErpSaleQuoteDO::getId, id).eq(ErpSaleQuoteDO::getStatus, status));
    }

}
