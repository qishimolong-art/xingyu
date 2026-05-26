package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 凭证主表 Mapper
 *
 * @author Claude
 */
@Mapper
public interface ErpVoucherMapper extends BaseMapperX<ErpVoucherDO> {

    default ErpVoucherDO selectByVoucherNo(String voucherNo) {
        return selectOne(ErpVoucherDO::getVoucherNo, voucherNo);
    }

    default PageResult<ErpVoucherDO> selectPage(ErpVoucherPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpVoucherDO>()
                .likeIfPresent(ErpVoucherDO::getVoucherNo, reqVO.getVoucherNo())
                .eqIfPresent(ErpVoucherDO::getVoucherWord, reqVO.getVoucherWord())
                .betweenIfPresent(ErpVoucherDO::getVoucherDate, reqVO.getVoucherDate())
                .eqIfPresent(ErpVoucherDO::getPeriodYear, reqVO.getPeriodYear())
                .eqIfPresent(ErpVoucherDO::getPeriodMonth, reqVO.getPeriodMonth())
                .eqIfPresent(ErpVoucherDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(ErpVoucherDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(ErpVoucherDO::getSourceBizType, reqVO.getSourceBizType())
                .eqIfPresent(ErpVoucherDO::getMakerUserId, reqVO.getMakerUserId())
                .eqIfPresent(ErpVoucherDO::getAuditorUserId, reqVO.getAuditorUserId())
                .likeIfPresent(ErpVoucherDO::getSummary, reqVO.getSummary())
                .likeIfPresent(ErpVoucherDO::getRemark, reqVO.getRemark())
                .orderByDesc(ErpVoucherDO::getId));
    }

    default int updateByIdAndAuditStatus(Long id, Integer auditStatus, ErpVoucherDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpVoucherDO>()
                .eq(ErpVoucherDO::getId, id).eq(ErpVoucherDO::getAuditStatus, auditStatus));
    }

    default List<ErpVoucherDO> selectListByBiz(Integer sourceBizType, Long sourceBizId) {
        return selectList(new LambdaQueryWrapperX<ErpVoucherDO>()
                .eq(ErpVoucherDO::getSourceBizType, sourceBizType)
                .eq(ErpVoucherDO::getSourceBizId, sourceBizId));
    }

}
