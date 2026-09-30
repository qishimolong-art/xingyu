package cn.iocoder.yudao.module.member.service.address;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.member.controller.app.address.vo.AppAddressCreateReqVO;
import cn.iocoder.yudao.module.member.controller.app.address.vo.AppAddressUpdateReqVO;
import cn.iocoder.yudao.module.member.convert.address.AddressConvert;
import cn.iocoder.yudao.module.member.dal.dataobject.address.MemberAddressDO;
import cn.iocoder.yudao.module.member.dal.mysql.address.MemberAddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.module.member.controller.app.address.vo.AppAddressBaseVO;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.member.enums.ErrorCodeConstants.ADDRESS_NOT_EXISTS;

/**
 * 用户收件地址 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class AddressServiceImpl implements AddressService {

    @Resource
    private MemberAddressMapper memberAddressMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAddress(Long userId, AppAddressCreateReqVO createReqVO) {
        validateLocation(createReqVO);
        // 如果添加的是默认收件地址，则将原默认地址修改为非默认
        if (Boolean.TRUE.equals(createReqVO.getDefaultStatus())) {
            List<MemberAddressDO> addresses = memberAddressMapper.selectListByUserIdAndDefaulted(userId, true);
            addresses.forEach(address -> memberAddressMapper.updateById(new MemberAddressDO().setId(address.getId()).setDefaultStatus(false)));
        }

        // 插入
        MemberAddressDO address = AddressConvert.INSTANCE.convert(createReqVO);
        address.setUserId(userId);
        memberAddressMapper.insert(address);
        // 返回
        return address.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long userId, AppAddressUpdateReqVO updateReqVO) {
        // 校验存在,校验是否能够操作
        MemberAddressDO oldAddress = getAddress(userId, updateReqVO.getId());
        if (oldAddress == null) {
            throw exception(ADDRESS_NOT_EXISTS);
        }
        validateLocation(updateReqVO);

        // 如果修改的是默认收件地址，则将原默认地址修改为非默认
        if (Boolean.TRUE.equals(updateReqVO.getDefaultStatus())) {
            List<MemberAddressDO> addresses = memberAddressMapper.selectListByUserIdAndDefaulted(userId, true);
            addresses.stream().filter(u -> !u.getId().equals(updateReqVO.getId())) // 排除自己
                    .forEach(address -> memberAddressMapper.updateById(new MemberAddressDO().setId(address.getId()).setDefaultStatus(false)));
        }

        // 更新
        MemberAddressDO updateObj = AddressConvert.INSTANCE.convert(updateReqVO);
        boolean hasLocation = updateReqVO.getLongitude() != null;
        boolean addressChanged = !Objects.equals(oldAddress.getAreaId(), updateReqVO.getAreaId())
                || !Objects.equals(oldAddress.getDetailAddress(), updateReqVO.getDetailAddress());
        if (!hasLocation && !addressChanged) {
            updateObj.setLongitude(oldAddress.getLongitude());
            updateObj.setLatitude(oldAddress.getLatitude());
            updateObj.setMapName(oldAddress.getMapName());
            updateObj.setMapAddress(oldAddress.getMapAddress());
        }
        // 显式 SET 位置字段，避免 MyBatis 忽略 null 而留下旧坐标；不影响默认地址的局部更新。
        LambdaUpdateWrapper<MemberAddressDO> locationUpdate = new LambdaUpdateWrapper<MemberAddressDO>()
                .eq(MemberAddressDO::getId, updateReqVO.getId())
                .eq(MemberAddressDO::getUserId, userId)
                .set(MemberAddressDO::getLongitude, updateObj.getLongitude())
                .set(MemberAddressDO::getLatitude, updateObj.getLatitude())
                .set(MemberAddressDO::getMapName, updateObj.getMapName())
                .set(MemberAddressDO::getMapAddress, updateObj.getMapAddress());
        updateObj.setLongitude(null);
        updateObj.setLatitude(null);
        updateObj.setMapName(null);
        updateObj.setMapAddress(null);
        memberAddressMapper.update(updateObj, locationUpdate);
    }

    private void validateLocation(AppAddressBaseVO request) {
        BigDecimal lon = request.getLongitude();
        BigDecimal lat = request.getLatitude();
        if ((lon == null) != (lat == null)) {
            throw invalidParamException("收货位置经纬度必须同时提供");
        }
        if (lon != null && (lon.abs().compareTo(new BigDecimal("180")) > 0
                || lat.abs().compareTo(new BigDecimal("90")) > 0
                || (lon.setScale(6, java.math.RoundingMode.HALF_UP).signum() == 0
                    && lat.setScale(6, java.math.RoundingMode.HALF_UP).signum() == 0))) {
            throw invalidParamException("收货位置经纬度无效");
        }
        if ((request.getMapName() != null && request.getMapName().length() > 200)
                || (request.getMapAddress() != null && request.getMapAddress().length() > 500)) {
            throw invalidParamException("地图地点名称或地址过长");
        }
        if (lon == null) {
            request.setMapName(null);
            request.setMapAddress(null);
        }
    }

    @Override
    public void deleteAddress(Long userId, Long id) {
        // 校验存在,校验是否能够操作
        validAddressExists(userId, id);
        // 删除
        memberAddressMapper.deleteById(id);
    }

    private void validAddressExists(Long userId, Long id) {
        MemberAddressDO addressDO = getAddress(userId, id);
        if (addressDO == null) {
            throw exception(ADDRESS_NOT_EXISTS);
        }
    }

    @Override
    public MemberAddressDO getAddress(Long userId, Long id) {
        return memberAddressMapper.selectByIdAndUserId(id, userId);
    }

    @Override
    public List<MemberAddressDO> getAddressList(Long userId) {
        return memberAddressMapper.selectListByUserIdAndDefaulted(userId, null);
    }

    @Override
    public MemberAddressDO getDefaultUserAddress(Long userId) {
        List<MemberAddressDO> addresses = memberAddressMapper.selectListByUserIdAndDefaulted(userId, true);
        return CollUtil.getFirst(addresses);
    }

}
