package cn.iocoder.yudao.module.erp.dal.mysql.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceSortUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ErpPayableOtherMapper extends BaseMapperX<ErpPayableOtherDO> {

    default PageResult<ErpPayableOtherDO> selectPage(ErpPayableOtherPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPayableOtherDO> wrapper = new LambdaQueryWrapperX<ErpPayableOtherDO>()
                .inIfPresent(ErpPayableOtherDO::getId, reqVO.getIds())
                .likeIfPresent(ErpPayableOtherDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpPayableOtherDO::getBizTime, reqVO.getBizTime())
                .eqIfPresent(ErpPayableOtherDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPayableOtherDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpPayableOtherDO::getHandlerId, reqVO.getHandlerId())
                .eqIfPresent(ErpPayableOtherDO::getCreator, reqVO.getCreator())
                .likeIfPresent(ErpPayableOtherDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpPayableOtherDO::getStatus, reqVO.getStatus());
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpPayableOtherDO::getNo,
                ErpPayableOtherDO::getVoucherNo,
                ErpPayableOtherDO::getProject,
                ErpPayableOtherDO::getSourceType,
                ErpPayableOtherDO::getRemark);
        ErpFinanceSortUtils.apply(wrapper, reqVO.getOrderField(), reqVO.getOrderDirection(), "erp_payable_other",
                "no", "bizTime", "supplierId", "deptId", "handlerId", "payableAmount", "settledAmount",
                "sourceType", "status", "voucherNo", "creator", "updater", "createTime", "updateTime",
                "remark");
        return selectPage(reqVO, wrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPayableOtherDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPayableOtherDO>()
                .eq(ErpPayableOtherDO::getId, id)
                .eq(ErpPayableOtherDO::getStatus, status));
    }

    default ErpPayableOtherDO selectByNo(String no) {
        return selectOne(ErpPayableOtherDO::getNo, no);
    }

    @Select("SELECT * FROM erp_payable_other WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    ErpPayableOtherDO selectByIdForUpdate(Long id);

}
