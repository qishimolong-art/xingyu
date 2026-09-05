package cn.iocoder.yudao.module.system.dal.mysql.user;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Mapper
public interface AdminUserMapper extends BaseMapperX<AdminUserDO> {

    default AdminUserDO selectByUsername(String username) {
        return selectOne(AdminUserDO::getUsername, username);
    }

    default AdminUserDO selectByEmail(String email) {
        return selectOne(AdminUserDO::getEmail, email);
    }

    default AdminUserDO selectByMobile(String mobile) {
        return selectOne(AdminUserDO::getMobile, mobile);
    }

    default List<AdminUserDO> selectListByMobile(String mobile) {
        return selectList(AdminUserDO::getMobile, mobile);
    }

    default PageResult<AdminUserDO> selectPage(UserPageReqVO reqVO, Collection<Long> deptIds, Collection<Long> userIds) {
        LambdaQueryWrapperX<AdminUserDO> wrapper = new LambdaQueryWrapperX<AdminUserDO>()
                .likeIfPresent(AdminUserDO::getUsername, reqVO.getUsername())
                .likeIfPresent(AdminUserDO::getNickname, reqVO.getNickname())
                .likeIfPresent(AdminUserDO::getMobile, reqVO.getMobile())
                .likeIfPresent(AdminUserDO::getEmail, reqVO.getEmail())
                .eqIfPresent(AdminUserDO::getSex, reqVO.getSex())
                .eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AdminUserDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(AdminUserDO::getDeptId, deptIds)
                .inIfPresent(AdminUserDO::getId, userIds);
        if (reqVO.getDataScope() != null) {
            if (Objects.equals(reqVO.getDataScope(), 0)) {
                wrapper.isNull(AdminUserDO::getDataScope);
            } else {
                wrapper.eq(AdminUserDO::getDataScope, reqVO.getDataScope());
            }
        }
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<AdminUserDO> wrapper, UserPageReqVO reqVO) {
        String relationOrderBySql = getRelationOrderBySql(reqVO.getOrderField(), reqVO.getOrderDirection());
        if (relationOrderBySql != null) {
            wrapper.last(relationOrderBySql);
            return;
        }
        SFunction<AdminUserDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
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

    static void orderByDefault(LambdaQueryWrapperX<AdminUserDO> wrapper) {
        wrapper.orderByDesc(AdminUserDO::getId);
    }

    static void orderByIdIfNotPresent(LambdaQueryWrapperX<AdminUserDO> wrapper, String orderField) {
        if (!"id".equals(orderField == null ? null : orderField.trim())) {
            wrapper.orderByDesc(AdminUserDO::getId);
        }
    }

    static String getRelationOrderBySql(String orderField, String orderDirection) {
        if (!isValidOrderDirection(orderDirection)) {
            return null;
        }
        String orderKey = getRelationOrderKey(orderField);
        if (orderKey == null) {
            return null;
        }
        String direction = "asc".equalsIgnoreCase(orderDirection) ? "ASC" : "DESC";
        // 升序时 NULL 排最前（nullFirst=0），降序时 NULL 排最后（nullFirst=1），与字符串升降序语义一致
        String nullFirst = "asc".equalsIgnoreCase(orderDirection) ? "0" : "1";
        return "ORDER BY CASE WHEN " + orderKey + " IS NULL THEN " + nullFirst + " ELSE " + (nullFirst.equals("0") ? "1" : "0") + " END ASC, "
                + orderKey + " " + direction + ", system_users.id DESC";
    }

    static boolean isValidOrderDirection(String orderDirection) {
        return "asc".equalsIgnoreCase(orderDirection) || "desc".equalsIgnoreCase(orderDirection);
    }

    static String getRelationOrderKey(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "deptName":
                return "(SELECT d.name FROM system_dept d"
                        + " WHERE d.id = system_users.dept_id"
                        + " AND d.deleted = 0"
                        + " AND d.tenant_id = system_users.tenant_id"
                        + " LIMIT 1)";
            case "roleNames":
                return "(SELECT MIN(r.name) FROM system_user_role ur"
                        + " INNER JOIN system_role r ON r.id = ur.role_id"
                        + " AND r.deleted = 0"
                        + " AND r.tenant_id = system_users.tenant_id"
                        + " WHERE ur.user_id = system_users.id"
                        + " AND ur.deleted = 0)";
            default:
                return null;
        }
    }

    static SFunction<AdminUserDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return AdminUserDO::getId;
            case "username":
                return AdminUserDO::getUsername;
            case "nickname":
                return AdminUserDO::getNickname;
            case "email":
                return AdminUserDO::getEmail;
            case "mobile":
                return AdminUserDO::getMobile;
            case "sex":
                return AdminUserDO::getSex;
            case "status":
                return AdminUserDO::getStatus;
            case "dataScope":
                return AdminUserDO::getDataScope;
            case "createTime":
                return AdminUserDO::getCreateTime;
            default:
                return null;
        }
    }

    default List<AdminUserDO> selectListByNickname(String nickname) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>().like(AdminUserDO::getNickname, nickname));
    }

    default List<AdminUserDO> selectListByNicknameEq(String nickname) {
        return selectList(AdminUserDO::getNickname, nickname);
    }

    default List<AdminUserDO> selectListByStatus(Integer status) {
        return selectList(AdminUserDO::getStatus, status);
    }

    default List<AdminUserDO> selectListByDeptIds(Collection<Long> deptIds) {
        return selectList(AdminUserDO::getDeptId, deptIds);
    }

}
