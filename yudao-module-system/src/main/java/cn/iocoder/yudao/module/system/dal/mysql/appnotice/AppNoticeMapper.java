package cn.iocoder.yudao.module.system.dal.mysql.appnotice;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.appnotice.vo.AppNoticePageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.appnotice.AppNoticeDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.ENABLE;

@Mapper
public interface AppNoticeMapper extends BaseMapperX<AppNoticeDO> {

    default PageResult<AppNoticeDO> selectPage(AppNoticePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AppNoticeDO>()
                .likeIfPresent(AppNoticeDO::getTitle, reqVO.getTitle())
                .eqIfPresent(AppNoticeDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AppNoticeDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AppNoticeDO::getId));
    }

    default AppNoticeDO selectHomeNotice() {
        return selectOne(buildVisibleWrapper(LocalDateTime.now())
                .orderByDesc(AppNoticeDO::getSort)
                .orderByDesc(AppNoticeDO::getCreateTime)
                .orderByDesc(AppNoticeDO::getId)
                .last("LIMIT 1"));
    }

    default List<AppNoticeDO> selectHomeNoticeList() {
        return selectList(buildVisibleWrapper(LocalDateTime.now())
                .orderByDesc(AppNoticeDO::getSort)
                .orderByDesc(AppNoticeDO::getCreateTime)
                .orderByDesc(AppNoticeDO::getId));
    }

    default AppNoticeDO selectVisibleNotice(Long id) {
        return selectOne(buildVisibleWrapper(LocalDateTime.now())
                .eq(AppNoticeDO::getId, id));
    }

    static LambdaQueryWrapper<AppNoticeDO> buildVisibleWrapper(LocalDateTime now) {
        return new LambdaQueryWrapperX<AppNoticeDO>()
                .eq(AppNoticeDO::getStatus, ENABLE.getStatus())
                .and(wrapper -> wrapper.isNull(AppNoticeDO::getStartTime).or().le(AppNoticeDO::getStartTime, now))
                .and(wrapper -> wrapper.isNull(AppNoticeDO::getEndTime).or().ge(AppNoticeDO::getEndTime, now));
    }

}
