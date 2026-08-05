package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.dal.dataobject.user.UserPriceFieldDO;
import cn.iocoder.yudao.module.system.service.user.dto.UserPriceFieldConfigDTO;

import java.util.List;

/**
 * 用户-价格字段 Service 接口
 */
public interface UserPriceFieldService {

    /**
     * 查询用户的全部价格字段配置（含不可见的）
     *
     * @param userId 用户 ID
     * @return 配置列表
     */
    List<UserPriceFieldDO> getUserPriceFields(Long userId);

    /**
     * 查询用户的全部配件价格字段配置，包含字段目录和显示名称。
     *
     * @param userId 用户 ID
     * @return 价格字段配置列表
     */
    List<UserPriceFieldConfigDTO> getUserPriceFieldConfigs(Long userId);

    /**
     * 保存用户价格字段配置（全量覆写：先删后插）
     *
     * @param userId     用户 ID
     * @param fieldCodes 需要设为可见的字段编码列表；不在列表中的字段视为不可见
     */
    void saveUserPriceFields(Long userId, List<String> fieldCodes);

    /**
     * Get product field keys hidden by the user's price field configuration.
     *
     * @param userId user id
     * @return hidden erp_product field keys
     */
    List<String> getHiddenProductPriceFields(Long userId);

}
