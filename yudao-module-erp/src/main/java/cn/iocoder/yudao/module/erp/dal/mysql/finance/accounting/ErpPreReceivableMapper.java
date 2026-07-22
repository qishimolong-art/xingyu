package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 预收账款单 Mapper
 */
@Mapper
public interface ErpPreReceivableMapper extends BaseMapperX<ErpPreReceivableDO> {

    default PageResult<ErpPreReceivableDO> selectPage(ErpPreReceivablePageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPreReceivableDO> wrapper = new LambdaQueryWrapperX<ErpPreReceivableDO>()
                .likeIfPresent(ErpPreReceivableDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpPreReceivableDO::getBizTime, reqVO.getBizTime())
                .eqIfPresent(ErpPreReceivableDO::getPartyType, reqVO.getPartyType())
                .eqIfPresent(ErpPreReceivableDO::getPartyId, reqVO.getPartyId())
                .likeIfPresent(ErpPreReceivableDO::getPartyName, reqVO.getPartyName())
                .eqIfPresent(ErpPreReceivableDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(ErpPreReceivableDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpPreReceivableDO::getId);
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpPreReceivableDO::getNo,
                ErpPreReceivableDO::getPartyName,
                ErpPreReceivableDO::getRemark);
        return selectPage(reqVO, wrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPreReceivableDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPreReceivableDO>()
                .eq(ErpPreReceivableDO::getId, id).eq(ErpPreReceivableDO::getStatus, status));
    }

    default ErpPreReceivableDO selectByNo(String no) {
        return selectOne(ErpPreReceivableDO::getNo, no);
    }

}
