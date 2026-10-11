package cn.iocoder.yudao.module.member.api.address;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.member.dal.dataobject.address.MemberAddressDO;
import cn.iocoder.yudao.module.member.dal.dataobject.user.MemberUserDO;
import cn.iocoder.yudao.module.member.dal.mysql.address.MemberAddressMapper;
import cn.iocoder.yudao.module.member.service.address.AddressService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberAddressApiImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private MemberAddressApiImpl memberAddressApi;
    @Mock
    private AddressService addressService;
    @Mock
    private MemberAddressMapper memberAddressMapper;

    @Test
    void getAddressTargetUserList_expandsParentAreasAndDeduplicatesMatchedAreas() {
        MemberUserDO user = new MemberUserDO().setId(1L).setNickname("会员一")
                .setMobile("13800000000").setStatus(0);
        when(memberAddressMapper.selectAddressTargetUserList(anyCollection(), isNull()))
                .thenReturn(Collections.singletonList(user));
        when(memberAddressMapper.selectListByUserIdsAndAreaIds(anyCollection(), anyCollection()))
                .thenReturn(Arrays.asList(
                        new MemberAddressDO().setUserId(1L).setAreaId(510107L),
                        new MemberAddressDO().setUserId(1L).setAreaId(510107L)));

        var result = memberAddressApi.getAddressTargetUserList(
                Arrays.asList(510000, 110000), null);

        assertEquals(1, result.size());
        assertEquals(Collections.singletonList(510107L), result.get(0).getMatchedAreaIds());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> areaCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(memberAddressMapper).selectAddressTargetUserList(areaCaptor.capture(), isNull());
        assertTrue(areaCaptor.getValue().contains(510000L));
        assertTrue(areaCaptor.getValue().contains(510107L));
        assertTrue(areaCaptor.getValue().contains(110000L));
        assertTrue(areaCaptor.getValue().contains(110101L));
    }

    @Test
    void getAddressTargetCount_usesExpandedAreas() {
        when(memberAddressMapper.selectAddressTargetCount(anyCollection())).thenReturn(13L);

        assertEquals(13L, memberAddressApi.getAddressTargetCount(Collections.singleton(510000)));
        verify(memberAddressMapper).selectAddressTargetCount(anyCollection());
    }

}
