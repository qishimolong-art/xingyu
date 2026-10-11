package cn.iocoder.yudao.module.member.dal.mysql.address;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.module.member.dal.dataobject.address.MemberAddressDO;
import cn.iocoder.yudao.module.member.dal.dataobject.user.MemberUserDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MemberAddressMapper extends BaseMapperX<MemberAddressDO> {

    default MemberAddressDO selectByIdAndUserId(Long id, Long userId) {
        return selectOne(MemberAddressDO::getId, id, MemberAddressDO::getUserId, userId);
    }

    default List<MemberAddressDO> selectListByUserIdAndDefaulted(Long userId, Boolean defaulted) {
        return selectList(new LambdaQueryWrapperX<MemberAddressDO>().eq(MemberAddressDO::getUserId, userId)
                .eqIfPresent(MemberAddressDO::getDefaultStatus, defaulted));
    }

    default List<MemberAddressDO> selectListByUserIdsAndAreaIds(Collection<Long> userIds,
                                                                 Collection<Long> areaIds) {
        return selectList(new LambdaQueryWrapperX<MemberAddressDO>()
                .in(MemberAddressDO::getUserId, userIds)
                .in(MemberAddressDO::getAreaId, areaIds));
    }

    @Select({"<script>",
            "SELECT COUNT(*) FROM member_address a",
            "INNER JOIN member_user u ON u.id = a.user_id AND u.tenant_id = a.tenant_id AND u.deleted = 0",
            "WHERE a.deleted = 0",
            "AND a.area_id IN",
            "<foreach collection='areaIds' item='areaId' open='(' separator=',' close=')'>#{areaId}</foreach>",
            "</script>"})
    Long selectAddressTargetCount(@Param("areaIds") Collection<Long> areaIds);

    @Select({"<script>",
            "SELECT u.* FROM member_user u",
            "WHERE u.deleted = 0",
            "<if test='status != null'>AND u.status = #{status}</if>",
            "AND EXISTS (",
            "  SELECT 1 FROM member_address a",
            "  WHERE a.user_id = u.id AND a.tenant_id = u.tenant_id AND a.deleted = 0",
            "    AND a.area_id IN",
            "    <foreach collection='areaIds' item='areaId' open='(' separator=',' close=')'>#{areaId}</foreach>",
            ")",
            "ORDER BY u.id DESC",
            "</script>"})
    IPage<MemberUserDO> selectAddressTargetUserPage(IPage<MemberUserDO> page,
                                                     @Param("areaIds") Collection<Long> areaIds,
                                                     @Param("status") Integer status);

    default PageResult<MemberUserDO> selectAddressTargetUserPage(PageParam pageParam, Collection<Long> areaIds,
                                                                  Integer status) {
        IPage<MemberUserDO> page = MyBatisUtils.buildPage(pageParam);
        selectAddressTargetUserPage(page, areaIds, status);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Select({"<script>",
            "SELECT DISTINCT u.* FROM member_user u",
            "INNER JOIN member_address a ON a.user_id = u.id AND a.tenant_id = u.tenant_id AND a.deleted = 0",
            "WHERE u.deleted = 0",
            "<if test='status != null'>AND u.status = #{status}</if>",
            "AND a.area_id IN",
            "<foreach collection='areaIds' item='areaId' open='(' separator=',' close=')'>#{areaId}</foreach>",
            "ORDER BY u.id DESC",
            "</script>"})
    List<MemberUserDO> selectAddressTargetUserList(@Param("areaIds") Collection<Long> areaIds,
                                                    @Param("status") Integer status);

}
