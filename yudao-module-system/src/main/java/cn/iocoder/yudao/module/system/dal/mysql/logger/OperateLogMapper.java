package cn.iocoder.yudao.module.system.dal.mysql.logger;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.api.logger.dto.OperateLogPageReqDTO;
import cn.iocoder.yudao.module.system.controller.admin.logger.vo.operatelog.OperateLogPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.logger.OperateLogDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface OperateLogMapper extends BaseMapperX<OperateLogDO> {

    default PageResult<OperateLogDO> selectPage(OperateLogPageReqVO pageReqDTO) {
        LambdaQueryWrapperX<OperateLogDO> wrapper = new LambdaQueryWrapperX<OperateLogDO>()
                .eqIfPresent(OperateLogDO::getUserId, pageReqDTO.getUserId())
                .eqIfPresent(OperateLogDO::getBizId, pageReqDTO.getBizId())
                .likeIfPresent(OperateLogDO::getSubType, pageReqDTO.getSubType())
                .likeIfPresent(OperateLogDO::getAction, pageReqDTO.getAction())
                .betweenIfPresent(OperateLogDO::getCreateTime, pageReqDTO.getCreateTime());
        appendTypeFilter(wrapper, pageReqDTO.getType());
        orderByIfPresent(wrapper, pageReqDTO);
        return selectPage(pageReqDTO, wrapper);
    }

    default PageResult<OperateLogDO> selectPage(OperateLogPageReqDTO pageReqDTO) {
        return selectPage(pageReqDTO, new LambdaQueryWrapperX<OperateLogDO>()
                .eqIfPresent(OperateLogDO::getType, pageReqDTO.getType())
                .eqIfPresent(OperateLogDO::getBizId, pageReqDTO.getBizId())
                .eqIfPresent(OperateLogDO::getUserId, pageReqDTO.getUserId())
                .orderByDesc(OperateLogDO::getId));
    }

    default List<OperateLogDO> selectListForModuleOptions() {
        return selectList(new LambdaQueryWrapperX<OperateLogDO>()
                .select(OperateLogDO::getType)
                .isNotNull(OperateLogDO::getType));
    }

    static void appendTypeFilter(LambdaQueryWrapperX<OperateLogDO> wrapper, String type) {
        if (!StringUtils.hasText(type)) {
            return;
        }
        List<String> types = buildCompatibleTypes(type.trim());
        wrapper.and(item -> item.in(OperateLogDO::getType, types));
    }

    static List<String> buildCompatibleTypes(String type) {
        List<String> types = new ArrayList<>();
        types.add(type);
        types.add("ERP" + type);
        types.add("ERP " + type);
        types.add("SYSTEM" + type);
        types.add("SYSTEM " + type);
        return types;
    }

    static void orderByIfPresent(LambdaQueryWrapperX<OperateLogDO> wrapper, OperateLogPageReqVO reqVO) {
        SFunction<OperateLogDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
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

    static void orderByDefault(LambdaQueryWrapperX<OperateLogDO> wrapper) {
        wrapper.orderByDesc(OperateLogDO::getId);
    }

    static void orderByIdIfNotPresent(LambdaQueryWrapperX<OperateLogDO> wrapper, String orderField) {
        if (!"id".equals(orderField == null ? null : orderField.trim())) {
            wrapper.orderByDesc(OperateLogDO::getId);
        }
    }

    static SFunction<OperateLogDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return OperateLogDO::getId;
            case "type":
                return OperateLogDO::getType;
            case "subType":
                return OperateLogDO::getSubType;
            case "createTime":
                return OperateLogDO::getCreateTime;
            case "userIp":
                return OperateLogDO::getUserIp;
            default:
                return null;
        }
    }

}
