package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserBatchUpdateReqVO;

import java.util.Set;

/**
 * Extension point for user batch update.
 */
public interface AdminUserBatchUpdateExtension {

    /**
     * Validate extension fields before any batch update is applied.
     *
     * @param reqVO user batch update request
     * @param userIds deduplicated user ids
     */
    void validate(UserBatchUpdateReqVO reqVO, Set<Long> userIds);

    /**
     * Update one user inside the batch update transaction.
     *
     * @param userId user id
     * @param reqVO user batch update request
     */
    void update(Long userId, UserBatchUpdateReqVO reqVO);

}
