package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayablePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpOtherPayableMapper extends BaseMapperX<ErpOtherPayableDO> {

    default ErpOtherPayableDO selectByNo(String no) {
        return selectOne(ErpOtherPayableDO::getNo, no);
    }

    default PageResult<ErpOtherPayableDO> selectPage(ErpOtherPayablePageReqVO reqVO) {
        LambdaQueryWrapperX<ErpOtherPayableDO> wrapper = new LambdaQueryWrapperX<ErpOtherPayableDO>()
                .likeIfPresent(ErpOtherPayableDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpOtherPayableDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpOtherPayableDO::getPartyType, reqVO.getPartyType())
                .eqIfPresent(ErpOtherPayableDO::getPartyId, reqVO.getPartyId())
                .likeIfPresent(ErpOtherPayableDO::getPartyName, reqVO.getPartyName())
                .eqIfPresent(ErpOtherPayableDO::getAccountId, reqVO.getAccountId())
                .betweenIfPresent(ErpOtherPayableDO::getBizTime, reqVO.getBizTime())
                .likeIfPresent(ErpOtherPayableDO::getRemark, reqVO.getRemark())
                .orderByDesc(ErpOtherPayableDO::getId);
        ErpKeywordQuery.appendWithDeptNameAndParty(wrapper, reqVO.getKeyword(),
                ErpOtherPayableDO::getNo,
                ErpOtherPayableDO::getPartyName,
                ErpOtherPayableDO::getRemark);
        return selectPage(reqVO, wrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpOtherPayableDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpOtherPayableDO>()
                .eq(ErpOtherPayableDO::getId, id).eq(ErpOtherPayableDO::getStatus, status));
    }

}
