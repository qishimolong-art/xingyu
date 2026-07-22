package cn.iocoder.yudao.module.system.dal.mysql.logger;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.logger.vo.loginlog.LoginLogPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.logger.LoginLogDO;
import cn.iocoder.yudao.module.system.enums.logger.LoginResultEnum;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper extends BaseMapperX<LoginLogDO> {

    default PageResult<LoginLogDO> selectPage(LoginLogPageReqVO reqVO) {
        LambdaQueryWrapperX<LoginLogDO> query = new LambdaQueryWrapperX<LoginLogDO>()
                .likeIfPresent(LoginLogDO::getUserIp, reqVO.getUserIp())
                .likeIfPresent(LoginLogDO::getUsername, reqVO.getUsername())
                .betweenIfPresent(LoginLogDO::getCreateTime, reqVO.getCreateTime());
        if (Boolean.TRUE.equals(reqVO.getStatus())) {
            query.eq(LoginLogDO::getResult, LoginResultEnum.SUCCESS.getResult());
        } else if (Boolean.FALSE.equals(reqVO.getStatus())) {
            query.gt(LoginLogDO::getResult, LoginResultEnum.SUCCESS.getResult());
        }
        orderByIfPresent(query, reqVO);
        return selectPage(reqVO, query);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<LoginLogDO> wrapper, LoginLogPageReqVO reqVO) {
        SFunction<LoginLogDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            orderByDefault(wrapper);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn);
            orderByIdIfNotPresent(wrapper, reqVO.getOrderField());
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn);
            orderByIdIfNotPresent(wrapper, reqVO.getOrderField());
            return;
        }
        orderByDefault(wrapper);
    }

    static void orderByDefault(LambdaQueryWrapperX<LoginLogDO> wrapper) {
        wrapper.orderByDesc(LoginLogDO::getId);
    }

    static void orderByIdIfNotPresent(LambdaQueryWrapperX<LoginLogDO> wrapper, String orderField) {
        if (!"id".equals(orderField == null ? null : orderField.trim())) {
            wrapper.orderByDesc(LoginLogDO::getId);
        }
    }

    static SFunction<LoginLogDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return LoginLogDO::getId;
            case "logType":
                return LoginLogDO::getLogType;
            case "username":
                return LoginLogDO::getUsername;
            case "userIp":
                return LoginLogDO::getUserIp;
            case "userAgent":
                return LoginLogDO::getUserAgent;
            case "result":
                return LoginLogDO::getResult;
            case "createTime":
                return LoginLogDO::getCreateTime;
            default:
                return null;
        }
    }

}
