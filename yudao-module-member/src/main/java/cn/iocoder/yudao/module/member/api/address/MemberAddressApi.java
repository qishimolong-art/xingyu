package cn.iocoder.yudao.module.member.api.address;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.member.api.address.dto.MemberAddressRespDTO;
import cn.iocoder.yudao.module.member.api.address.dto.MemberAddressTargetRespDTO;

import java.util.Collection;
import java.util.List;

/**
 * 用户收件地址 API 接口
 *
 * @author 芋道源码
 */
public interface MemberAddressApi {

    /**
     * 获得用户收件地址
     *
     * @param id 收件地址编号
     * @param userId 用户编号
     * @return 用户收件地址
     */
    MemberAddressRespDTO getAddress(Long id, Long userId);

    /**
     * 获得用户默认收件地址
     *
     * @param userId 用户编号
     * @return 用户收件地址
     */
    MemberAddressRespDTO getDefaultAddress(Long userId);

    /**
     * 按会员任一收货地址所在地区，分页查询去重后的会员。
     * 传入省、市节点时会自动包含全部下级地区。
     */
    PageResult<MemberAddressTargetRespDTO> getAddressTargetUserPage(Collection<Integer> areaIds,
                                                                    Integer userStatus,
                                                                    PageParam pageParam);

    /**
     * 按会员任一收货地址所在地区，查询去重后的全部会员。
     * 仅用于后台批量业务，返回数据不包含收件人和详细地址。
     */
    List<MemberAddressTargetRespDTO> getAddressTargetUserList(Collection<Integer> areaIds,
                                                               Integer userStatus);

    /** 按地区统计命中的未删除收货地址数量，传入上级地区时包含全部下级地区。 */
    Long getAddressTargetCount(Collection<Integer> areaIds);

}
