package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpOtherReceivableMapper extends BaseMapperX<ErpOtherReceivableDO> {

    default PageResult<ErpOtherReceivableDO> selectPage(ErpOtherReceivablePageReqVO reqVO) {
        LambdaQueryWrapperX<ErpOtherReceivableDO> wrapper = new LambdaQueryWrapperX<ErpOtherReceivableDO>()
                .likeIfPresent(ErpOtherReceivableDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpOtherReceivableDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpOtherReceivableDO::getPartyType, reqVO.getPartyType())
                .eqIfPresent(ErpOtherReceivableDO::getPartyId, reqVO.getPartyId())
                .likeIfPresent(ErpOtherReceivableDO::getPartyName, reqVO.getPartyName())
                .eqIfPresent(ErpOtherReceivableDO::getAccountId, reqVO.getAccountId())
                .betweenIfPresent(ErpOtherReceivableDO::getBizTime, reqVO.getBizTime())
                .eqIfPresent(ErpOtherReceivableDO::getCreator, reqVO.getCreator())
                .orderByDesc(ErpOtherReceivableDO::getId);
        ErpKeywordQuery.appendWithDeptNameAndParty(wrapper, reqVO.getKeyword(),
                ErpOtherReceivableDO::getNo,
                ErpOtherReceivableDO::getPartyName,
                ErpOtherReceivableDO::getRemark);
        return selectPage(reqVO, wrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpOtherReceivableDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpOtherReceivableDO>()
                .eq(ErpOtherReceivableDO::getId, id).eq(ErpOtherReceivableDO::getStatus, status));
    }

    default ErpOtherReceivableDO selectByNo(String no) {
        return selectOne(ErpOtherReceivableDO::getNo, no);
    }

}
