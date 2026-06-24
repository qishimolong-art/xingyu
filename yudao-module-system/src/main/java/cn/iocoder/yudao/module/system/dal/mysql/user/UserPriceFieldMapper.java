package cn.iocoder.yudao.module.system.dal.mysql.user;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.user.UserPriceFieldDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserPriceFieldMapper extends BaseMapperX<UserPriceFieldDO> {

    /**
     * 查询指定用户的全部价格字段配置
     */
    default List<UserPriceFieldDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<UserPriceFieldDO>()
                .eq(UserPriceFieldDO::getUserId, userId));
    }

    /**
     * 按 userId 逻辑删除（MyBatis Plus 自动将 deleted 置为 1）
     */
    @Delete("DELETE FROM system_user_price_field WHERE user_id = #{userId} AND tenant_id = #{tenantId}")
    void deleteByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

}
