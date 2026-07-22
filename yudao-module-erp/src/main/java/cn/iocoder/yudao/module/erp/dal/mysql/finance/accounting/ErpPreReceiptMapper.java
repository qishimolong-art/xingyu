package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 预收款单 Mapper
 */
@Mapper
public interface ErpPreReceiptMapper extends BaseMapperX<ErpPreReceiptDO> {

    default PageResult<ErpPreReceiptDO> selectPage(ErpPreReceiptPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPreReceiptDO> wrapper = new LambdaQueryWrapperX<ErpPreReceiptDO>()
                .likeIfPresent(ErpPreReceiptDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpPreReceiptDO::getBizTime, reqVO.getBizTime())
                .eqIfPresent(ErpPreReceiptDO::getPartyType, reqVO.getPartyType())
                .eqIfPresent(ErpPreReceiptDO::getPartyId, reqVO.getPartyId())
                .likeIfPresent(ErpPreReceiptDO::getPartyName, reqVO.getPartyName())
                .eqIfPresent(ErpPreReceiptDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(ErpPreReceiptDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpPreReceiptDO::getId);
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpPreReceiptDO::getNo,
                ErpPreReceiptDO::getPartyName,
                ErpPreReceiptDO::getRemark);
        return selectPage(reqVO, wrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPreReceiptDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPreReceiptDO>()
                .eq(ErpPreReceiptDO::getId, id).eq(ErpPreReceiptDO::getStatus, status));
    }

    default ErpPreReceiptDO selectByNo(String no) {
        return selectOne(ErpPreReceiptDO::getNo, no);
    }

}
