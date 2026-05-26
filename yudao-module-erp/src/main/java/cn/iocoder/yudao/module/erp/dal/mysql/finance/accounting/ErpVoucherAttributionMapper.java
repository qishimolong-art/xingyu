package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherAttributionDO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 凭证归属（跨月调整）Mapper
 *
 * @author Claude
 */
@Mapper
public interface ErpVoucherAttributionMapper extends BaseMapperX<ErpVoucherAttributionDO> {

    default PageResult<ErpVoucherAttributionDO> selectPage(ErpVoucherAttributionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpVoucherAttributionDO>()
                .eqIfPresent(ErpVoucherAttributionDO::getBizType, reqVO.getBizType())
                .likeIfPresent(ErpVoucherAttributionDO::getBizNo, reqVO.getBizNo())
                .betweenIfPresent(ErpVoucherAttributionDO::getBizDate, reqVO.getBizDate())
                .eqIfPresent(ErpVoucherAttributionDO::getAttributionStatus, reqVO.getAttributionStatus())
                .eqIfPresent(ErpVoucherAttributionDO::getAttributionYear, reqVO.getAttributionYear())
                .eqIfPresent(ErpVoucherAttributionDO::getAttributionMonth, reqVO.getAttributionMonth())
                .eqIfPresent(ErpVoucherAttributionDO::getHandlerUserId, reqVO.getHandlerUserId())
                .eqIfPresent(ErpVoucherAttributionDO::getAuditorUserId, reqVO.getAuditorUserId())
                .eqIfPresent(ErpVoucherAttributionDO::getSettleMethod, reqVO.getSettleMethod())
                .eqIfPresent(ErpVoucherAttributionDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpVoucherAttributionDO::getReceivableConfirmed, reqVO.getReceivableConfirmed())
                .eqIfPresent(ErpVoucherAttributionDO::getInvoiceIssued, reqVO.getInvoiceIssued())
                .eqIfPresent(ErpVoucherAttributionDO::getShipStatus, reqVO.getShipStatus())
                .likeIfPresent(ErpVoucherAttributionDO::getTransactionParty, reqVO.getTransactionParty())
                .likeIfPresent(ErpVoucherAttributionDO::getSummary, reqVO.getSummary())
                .orderByDesc(ErpVoucherAttributionDO::getId));
    }

    /**
     * 乐观锁更新：仅当当前 attribution_status = oldStatus 时才更新成功。
     * 用于幂等保护并发生成凭证（H3）。
     */
    default int updateByIdAndStatus(Long id, Integer oldStatus, ErpVoucherAttributionDO updateObj) {
        return update(updateObj, Wrappers.<ErpVoucherAttributionDO>lambdaUpdate()
                .eq(ErpVoucherAttributionDO::getId, id)
                .eq(ErpVoucherAttributionDO::getAttributionStatus, oldStatus));
    }

}
