package cn.iocoder.yudao.module.erp.framework.cloudprint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwPrintSignUtilsTest {

    @Test
    void sign_success() {
        assertEquals("5F52EC78A3B496D9FA8C5E3A74FE520B",
                SwPrintSignUtils.sign("sw-aiot", "secret001", 16788909829382L));
    }

}
