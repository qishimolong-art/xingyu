package cn.iocoder.yudao.framework.mybatis.core.handler;

import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.erp.dal.dataobject.TestErpDeptDO;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultDBFieldHandlerTest {

    @Mock
    private PermissionCommonApi permissionApi;
    @Mock
    private ObjectProvider<PermissionCommonApi> permissionApiProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testInsertFill_UseLoginDept() {
        // mock login user
        HashMap<String, String> info = new HashMap<>();
        info.put(LoginUser.INFO_KEY_DEPT_ID, "1");
        mockLoginUser(info);

        TestErpDeptDO data = new TestErpDeptDO();
        MetaObject metaObject = SystemMetaObject.forObject(data);
        new DefaultDBFieldHandler(permissionApiProvider).insertFill(metaObject);

        assertEquals(1L, data.getDeptId());
        assertEquals("100", data.getCreator());
        assertEquals("100", data.getUpdater());
    }

    @Test
    void testInsertFill_UseUserDeptWhenLoginDeptMissing() {
        mockLoginUser(new HashMap<>());
        when(permissionApiProvider.getIfAvailable()).thenReturn(permissionApi);
        when(permissionApi.getDeptIdsByUserId(100L)).thenReturn(cn.hutool.core.collection.CollUtil.newHashSet(20L, 10L));

        TestErpDeptDO data = new TestErpDeptDO();
        MetaObject metaObject = SystemMetaObject.forObject(data);
        new DefaultDBFieldHandler(permissionApiProvider).insertFill(metaObject);

        assertEquals(10L, data.getDeptId());
    }

    @Test
    void testInsertFill_KeepExplicitDeptId() {
        // mock login user
        HashMap<String, String> info = new HashMap<>();
        info.put(LoginUser.INFO_KEY_DEPT_ID, "1");
        mockLoginUser(info);

        TestErpDeptDO data = new TestErpDeptDO().setDeptId(30L);
        MetaObject metaObject = SystemMetaObject.forObject(data);
        new DefaultDBFieldHandler(permissionApiProvider).insertFill(metaObject);

        assertEquals(30L, data.getDeptId());
    }

    private void mockLoginUser(HashMap<String, String> info) {
        LoginUser loginUser = new LoginUser().setId(100L).setInfo(info);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                loginUser, null, Collections.emptyList()));
    }

}
