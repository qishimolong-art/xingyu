package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceSortUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpReceivableOtherMapper extends BaseMapperX<ErpReceivableOtherDO> {

    default PageResult<ErpReceivableOtherDO> selectPage(ErpReceivableOtherPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpReceivableOtherDO> wrapper = new LambdaQueryWrapperX<ErpReceivableOtherDO>()
                .inIfPresent(ErpReceivableOtherDO::getId, reqVO.getIds())
                .likeIfPresent(ErpReceivableOtherDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getBizTime())
                .eqIfPresent(ErpReceivableOtherDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpReceivableOtherDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpReceivableOtherDO::getHandlerId, reqVO.getHandlerId())
                .eqIfPresent(ErpReceivableOtherDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpReceivableOtherDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpReceivableOtherDO::getRemark, reqVO.getRemark());
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpReceivableOtherDO::getNo,
                ErpReceivableOtherDO::getVoucherNo,
                ErpReceivableOtherDO::getProject,
                ErpReceivableOtherDO::getSourceType,
                ErpReceivableOtherDO::getReceivableType,
                ErpReceivableOtherDO::getRemark,
                ErpReceivableOtherDO::getPaperNoteDesc,
                ErpReceivableOtherDO::getSourceNo);
        ErpFinanceSortUtils.apply(wrapper, reqVO.getOrderField(), reqVO.getOrderDirection(), "erp_receivable_other",
                "no", "bizTime", "customerId", "deptId", "handlerId", "receivableAmount", "settledAmount",
                "sourceType", "status", "voucherNo", "creator", "updater", "createTime", "updateTime",
                "remark", "project");
        return selectPage(reqVO, wrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpReceivableOtherDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpReceivableOtherDO>()
                .eq(ErpReceivableOtherDO::getId, id)
                .eq(ErpReceivableOtherDO::getStatus, status));
    }

    default ErpReceivableOtherDO selectByNo(String no) {
        return selectOne(ErpReceivableOtherDO::getNo, no);
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpReceivableOtherDO::getCustomerId, customerId);
    }
}
