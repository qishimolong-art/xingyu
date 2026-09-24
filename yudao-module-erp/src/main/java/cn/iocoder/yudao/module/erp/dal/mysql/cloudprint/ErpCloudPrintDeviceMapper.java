package cn.iocoder.yudao.module.erp.dal.mysql.cloudprint;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDevicePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintDeviceDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

@Mapper
public interface ErpCloudPrintDeviceMapper extends BaseMapperX<ErpCloudPrintDeviceDO> {

    default PageResult<ErpCloudPrintDeviceDO> selectPage(ErpCloudPrintDevicePageReqVO reqVO) {
        LambdaQueryWrapperX<ErpCloudPrintDeviceDO> wrapper = new LambdaQueryWrapperX<ErpCloudPrintDeviceDO>()
                .eqIfPresent(ErpCloudPrintDeviceDO::getDevType, reqVO.getDevType())
                .eqIfPresent(ErpCloudPrintDeviceDO::getContentType, reqVO.getContentType())
                .eqIfPresent(ErpCloudPrintDeviceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpCloudPrintDeviceDO::getOnlineState, reqVO.getOnlineState())
                .eqIfPresent(ErpCloudPrintDeviceDO::getDeptId, reqVO.getDeptId());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            wrapper.and(query -> query
                    .like(ErpCloudPrintDeviceDO::getNickname, reqVO.getKeyword())
                    .or()
                    .like(ErpCloudPrintDeviceDO::getDevid, reqVO.getKeyword()));
        }
        wrapper.orderByDesc(ErpCloudPrintDeviceDO::getDefaulted)
                .orderByAsc(ErpCloudPrintDeviceDO::getStatus)
                .orderByDesc(ErpCloudPrintDeviceDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default ErpCloudPrintDeviceDO selectDefault() {
        return selectOne(new LambdaQueryWrapper<ErpCloudPrintDeviceDO>()
                .eq(ErpCloudPrintDeviceDO::getDefaulted, true)
                .eq(ErpCloudPrintDeviceDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByDesc(ErpCloudPrintDeviceDO::getId)
                .last("LIMIT 1"));
    }

    default ErpCloudPrintDeviceDO selectByDevid(String devid) {
        return selectOne(ErpCloudPrintDeviceDO::getDevid, devid);
    }

    default void clearDefaulted(Long excludeId) {
        LambdaUpdateWrapper<ErpCloudPrintDeviceDO> wrapper = new LambdaUpdateWrapper<ErpCloudPrintDeviceDO>()
                .set(ErpCloudPrintDeviceDO::getDefaulted, false)
                .eq(ErpCloudPrintDeviceDO::getDefaulted, true);
        if (excludeId != null) {
            wrapper.ne(ErpCloudPrintDeviceDO::getId, excludeId);
        }
        update(null, wrapper);
    }

}
