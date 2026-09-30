package cn.iocoder.yudao.module.member.service.address;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.member.controller.app.address.vo.AppAddressCreateReqVO;
import cn.iocoder.yudao.module.member.controller.app.address.vo.AppAddressUpdateReqVO;
import cn.iocoder.yudao.module.member.dal.dataobject.address.MemberAddressDO;
import cn.iocoder.yudao.module.member.dal.mysql.address.MemberAddressMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.math.BigDecimal;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import cn.iocoder.yudao.framework.common.exception.ServiceException;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.module.member.enums.ErrorCodeConstants.ADDRESS_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link AddressServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import(AddressServiceImpl.class)
public class MemberAddressServiceImplTest extends BaseDbUnitTest {

    private AppAddressCreateReqVO locationRequest() {
        AppAddressCreateReqVO request = new AppAddressCreateReqVO();
        request.setName("张三");
        request.setMobile("13800000000");
        request.setAreaId(510107L);
        request.setDetailAddress("测试小区1号");
        request.setDefaultStatus(true);
        request.setLongitude(new BigDecimal("104.065000"));
        request.setLatitude(new BigDecimal("30.657000"));
        request.setMapName("测试小区");
        request.setMapAddress("四川省成都市测试小区");
        return request;
    }

    @Test
    public void testLocationRoundTripAndLegacyUpdates() {
        Long userId = 101L;
        Long id = addressService.createAddress(userId, locationRequest());
        MemberAddressDO saved = addressService.getAddress(userId, id);
        assertEquals(new BigDecimal("104.065000"), saved.getLongitude());
        assertEquals("测试小区", addressService.getAddressList(userId).get(0).getMapName());
        AppAddressUpdateReqVO update = BeanUtils.toBean(locationRequest(), AppAddressUpdateReqVO.class);
        update.setId(id);
        update.setLongitude(null);
        update.setLatitude(null);
        update.setMapName(null);
        update.setMapAddress(null);
        update.setName("李四");
        addressService.updateAddress(userId, update);
        assertEquals(saved.getLongitude(), addressService.getAddress(userId, id).getLongitude());
        assertEquals(saved.getMapName(), addressService.getAddress(userId, id).getMapName());
        update.setDetailAddress("新的收货地址");
        addressService.updateAddress(userId, update);
        saved = addressService.getAddress(userId, id);
        assertNull(saved.getLongitude());
        assertNull(saved.getLatitude());
        assertNull(saved.getMapName());
        assertNull(saved.getMapAddress());
        update.setLongitude(new BigDecimal("105.123456"));
        update.setLatitude(new BigDecimal("31.123456"));
        update.setMapName("新地点");
        addressService.updateAddress(userId, update);
        assertEquals(update.getLongitude(), addressService.getAddress(userId, id).getLongitude());
    }

    @Test
    public void testInvalidLocationDoesNotResetDefault() {
        Long id = addressService.createAddress(102L, locationRequest());
        AppAddressCreateReqVO bad = locationRequest();
        bad.setLatitude(null);
        assertEquals(400, assertThrows(ServiceException.class, () -> addressService.createAddress(102L, bad)).getCode());
        bad.setLatitude(BigDecimal.ZERO);
        bad.setLongitude(BigDecimal.ZERO);
        assertThrows(ServiceException.class, () -> addressService.createAddress(102L, bad));
        bad.setLongitude(new BigDecimal("181"));
        assertThrows(ServiceException.class, () -> addressService.createAddress(102L, bad));
        bad.setLongitude(new BigDecimal("104"));
        bad.setLatitude(new BigDecimal("91"));
        assertThrows(ServiceException.class, () -> addressService.createAddress(102L, bad));
        assertEquals(id, addressService.getDefaultUserAddress(102L).getId());
        assertEquals(1, addressService.getAddressList(102L).size());
    }

    @Test
    public void testDefaultAndOwnershipPreserveLocation() {
        Long id = addressService.createAddress(103L, locationRequest());
        Long secondId = addressService.createAddress(103L, locationRequest());
        assertEquals(secondId, addressService.getDefaultUserAddress(103L).getId());
        assertEquals(new BigDecimal("104.065000"), addressService.getAddress(103L, id).getLongitude());
        AppAddressUpdateReqVO update = BeanUtils.toBean(locationRequest(), AppAddressUpdateReqVO.class);
        update.setId(id);
        assertServiceException(() -> addressService.updateAddress(104L, update), ADDRESS_NOT_EXISTS);
        assertServiceException(() -> addressService.deleteAddress(104L, id), ADDRESS_NOT_EXISTS);
        assertNull(addressService.getAddress(104L, id));
        addressService.deleteAddress(103L, secondId);
        assertEquals(1, addressService.getAddressList(103L).size());
    }

    @Resource
    private AddressServiceImpl addressService;

    @Resource
    private MemberAddressMapper addressMapper;

    @Test
    public void testCreateAddress_success() {
        // 准备参数
        AppAddressCreateReqVO reqVO = randomPojo(AppAddressCreateReqVO.class, o -> { o.setLongitude(null); o.setLatitude(null); o.setMapName(null); o.setMapAddress(null); });

        // 调用
        Long addressId = addressService.createAddress(randomLongId(), reqVO);
        // 断言
        assertNotNull(addressId);
        // 校验记录的属性是否正确
        MemberAddressDO address = addressMapper.selectById(addressId);
        assertPojoEquals(reqVO, address);
    }

    @Test
    public void testUpdateAddress_success() {
        // mock 数据
        MemberAddressDO dbAddress = randomPojo(MemberAddressDO.class, o -> { o.setLongitude(null); o.setLatitude(null); });
        addressMapper.insert(dbAddress);// @Sql: 先插入出一条存在的数据
        // 准备参数
        AppAddressUpdateReqVO reqVO = randomPojo(AppAddressUpdateReqVO.class, o -> {
            o.setLongitude(null); o.setLatitude(null); o.setMapName(null); o.setMapAddress(null);
            o.setId(dbAddress.getId()); // 设置更新的 ID
        });

        // 调用
        addressService.updateAddress(dbAddress.getUserId(), reqVO);
        // 校验是否更新正确
        MemberAddressDO address = addressMapper.selectById(reqVO.getId()); // 获取最新的
        assertPojoEquals(reqVO, address);
    }

    @Test
    public void testUpdateAddress_notExists() {
        // 准备参数
        AppAddressUpdateReqVO reqVO = randomPojo(AppAddressUpdateReqVO.class);

        // 调用, 并断言异常
        assertServiceException(() -> addressService.updateAddress(randomLongId(), reqVO), ADDRESS_NOT_EXISTS);
    }

    @Test
    public void testDeleteAddress_success() {
        // mock 数据
        MemberAddressDO dbAddress = randomPojo(MemberAddressDO.class, o -> { o.setLongitude(null); o.setLatitude(null); });
        addressMapper.insert(dbAddress);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbAddress.getId();

        // 调用
        addressService.deleteAddress(dbAddress.getUserId(), id);
        // 校验数据不存在了
        assertNull(addressMapper.selectById(id));
    }

    @Test
    public void testDeleteAddress_notExists() {
        // 准备参数
        Long id = randomLongId();

        // 调用, 并断言异常
        assertServiceException(() -> addressService.deleteAddress(randomLongId(), id), ADDRESS_NOT_EXISTS);
    }

}
