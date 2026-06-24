package cn.iocoder.yudao.module.system.dal.mysql.dept;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DeptMapper extends BaseMapperX<DeptDO> {

    default List<DeptDO> selectList(DeptListReqVO reqVO) {
        return selectList(reqVO, null);
    }

    default List<DeptDO> selectList(DeptListReqVO reqVO, Collection<Long> leaderUserIds) {
        return selectList(new LambdaQueryWrapperX<DeptDO>()
                .likeIfPresent(DeptDO::getName, reqVO.getName())
                .eqIfPresent(DeptDO::getLeaderUserId, reqVO.getLeaderUserId())
                .inIfPresent(DeptDO::getLeaderUserId, leaderUserIds)
                .eqIfPresent(DeptDO::getStatus, reqVO.getStatus()));
    }

    default DeptDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(new LambdaQueryWrapperX<DeptDO>()
                .eq(DeptDO::getParentId, parentId)
                .eq(DeptDO::getName, name));
    }

    default List<DeptDO> selectListByName(String name) {
        return selectList(DeptDO::getName, name);
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(DeptDO::getParentId, parentId);
    }

    default DeptDO selectFirstByParentIdOrderBySortDesc(Long parentId) {
        QueryWrapperX<DeptDO> query = new QueryWrapperX<DeptDO>()
                .eq("parent_id", parentId)
                .orderByDesc("sort")
                .limitN(1);
        query.isNotNull("sort");
        return selectOne(query);
    }

    default DeptDO selectFirstByParentIdOrderBySortDescExcludeId(Long parentId, Long excludeId) {
        QueryWrapperX<DeptDO> query = new QueryWrapperX<DeptDO>()
                .eq("parent_id", parentId)
                .neIfPresent("id", excludeId)
                .orderByDesc("sort")
                .limitN(1);
        query.isNotNull("sort");
        return selectOne(query);
    }

    default List<DeptDO> selectListByParentId(Collection<Long> parentIds) {
        return selectList(DeptDO::getParentId, parentIds);
    }

    default List<DeptDO> selectListByLeaderUserId(Long id) {
        return selectList(DeptDO::getLeaderUserId, id);
    }

    default List<DeptDO> selectListByLeaderUserIds(Collection<Long> ids) {
        return selectList(DeptDO::getLeaderUserId, ids);
    }

}
