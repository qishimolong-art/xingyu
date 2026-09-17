package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpPrePaymentMapper extends BaseMapperX<ErpPrePaymentDO> {

    default PageResult<ErpPrePaymentDO> selectPage(ErpPrePaymentPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPrePaymentDO> wrapper = new LambdaQueryWrapperX<ErpPrePaymentDO>()
                .likeIfPresent(ErpPrePaymentDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpPrePaymentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpPrePaymentDO::getPartyType, reqVO.getPartyType())
                .eqIfPresent(ErpPrePaymentDO::getPartyId, reqVO.getPartyId())
                .likeIfPresent(ErpPrePaymentDO::getPartyName, reqVO.getPartyName())
                .eqIfPresent(ErpPrePaymentDO::getAccountId, reqVO.getAccountId())
                .betweenIfPresent(ErpPrePaymentDO::getBizTime, reqVO.getBizTime())
                .orderByDesc(ErpPrePaymentDO::getId);
        ErpKeywordQuery.appendWithDeptNameAndParty(wrapper, reqVO.getKeyword(),
                ErpPrePaymentDO::getNo,
                ErpPrePaymentDO::getPartyName,
                ErpPrePaymentDO::getRemark);
        return selectPage(reqVO, wrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPrePaymentDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPrePaymentDO>()
                .eq(ErpPrePaymentDO::getId, id).eq(ErpPrePaymentDO::getStatus, status));
    }

    default ErpPrePaymentDO selectByNo(String no) {
        return selectOne(ErpPrePaymentDO::getNo, no);
    }

}
