package cn.iocoder.yudao.module.member.api.address;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.ip.core.Area;
import cn.iocoder.yudao.framework.ip.core.utils.AreaUtils;
import cn.iocoder.yudao.module.member.api.address.dto.MemberAddressRespDTO;
import cn.iocoder.yudao.module.member.api.address.dto.MemberAddressTargetRespDTO;
import cn.iocoder.yudao.module.member.convert.address.AddressConvert;
import cn.iocoder.yudao.module.member.dal.dataobject.address.MemberAddressDO;
import cn.iocoder.yudao.module.member.dal.dataobject.user.MemberUserDO;
import cn.iocoder.yudao.module.member.dal.mysql.address.MemberAddressMapper;
import cn.iocoder.yudao.module.member.service.address.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

/**
 * 用户收件地址 API 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MemberAddressApiImpl implements MemberAddressApi {

    @Resource
    private AddressService addressService;
    @Resource
    private MemberAddressMapper memberAddressMapper;

    @Override
    public MemberAddressRespDTO getAddress(Long id, Long userId) {
        return AddressConvert.INSTANCE.convert02(addressService.getAddress(userId, id));
    }

    @Override
    public MemberAddressRespDTO getDefaultAddress(Long userId) {
        return AddressConvert.INSTANCE.convert02(addressService.getDefaultUserAddress(userId));
    }

    @Override
    public PageResult<MemberAddressTargetRespDTO> getAddressTargetUserPage(Collection<Integer> areaIds,
                                                                           Integer userStatus,
                                                                           PageParam pageParam) {
        Set<Long> expandedAreaIds = expandAreaIds(areaIds);
        PageResult<MemberUserDO> userPage = memberAddressMapper.selectAddressTargetUserPage(
                pageParam, expandedAreaIds, userStatus);
        return new PageResult<>(convertTargets(userPage.getList(), expandedAreaIds), userPage.getTotal());
    }

    @Override
    public List<MemberAddressTargetRespDTO> getAddressTargetUserList(Collection<Integer> areaIds,
                                                                      Integer userStatus) {
        Set<Long> expandedAreaIds = expandAreaIds(areaIds);
        return convertTargets(memberAddressMapper.selectAddressTargetUserList(expandedAreaIds, userStatus),
                expandedAreaIds);
    }

    @Override
    public Long getAddressTargetCount(Collection<Integer> areaIds) {
        return memberAddressMapper.selectAddressTargetCount(expandAreaIds(areaIds));
    }

    private List<MemberAddressTargetRespDTO> convertTargets(List<MemberUserDO> users, Set<Long> areaIds) {
        if (CollUtil.isEmpty(users)) {
            return Collections.emptyList();
        }
        List<Long> userIds = convertList(users, MemberUserDO::getId);
        List<MemberAddressDO> addresses = memberAddressMapper.selectListByUserIdsAndAreaIds(userIds, areaIds);
        Map<Long, LinkedHashSet<Long>> matchedAreaMap = new LinkedHashMap<>();
        addresses.forEach(address -> matchedAreaMap
                .computeIfAbsent(address.getUserId(), key -> new LinkedHashSet<>())
                .add(address.getAreaId()));

        List<MemberAddressTargetRespDTO> result = new ArrayList<>(users.size());
        for (MemberUserDO user : users) {
            List<Long> matchedAreaIds = new ArrayList<>(matchedAreaMap.getOrDefault(user.getId(), new LinkedHashSet<>()));
            MemberAddressTargetRespDTO target = new MemberAddressTargetRespDTO();
            target.setUserId(user.getId());
            target.setNickname(user.getNickname());
            target.setMobile(user.getMobile());
            target.setStatus(user.getStatus());
            target.setMatchedAreaIds(matchedAreaIds);
            target.setMatchedAreaNames(convertList(matchedAreaIds,
                    areaId -> AreaUtils.format(Math.toIntExact(areaId))));
            result.add(target);
        }
        return result;
    }

    private Set<Long> expandAreaIds(Collection<Integer> areaIds) {
        if (CollUtil.isEmpty(areaIds)) {
            throw invalidParamException("收货地区不能为空");
        }
        Set<Long> result = new LinkedHashSet<>();
        for (Integer areaId : areaIds) {
            Area root = AreaUtils.getArea(areaId);
            if (root == null) {
                throw invalidParamException("收货地区不存在：" + areaId);
            }
            ArrayDeque<Area> queue = new ArrayDeque<>();
            queue.add(root);
            while (!queue.isEmpty()) {
                Area area = queue.removeFirst();
                result.add(area.getId().longValue());
                queue.addAll(area.getChildren());
            }
        }
        return result;
    }

}
