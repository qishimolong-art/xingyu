package cn.iocoder.yudao.module.erp.service.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErpMnemonicCodeUtilsTest {

    @Test
    void buildPinyinCode_whenProductNameContainsMeiFu_thenUsesFuInitial() {
        assertEquals("MFZDBSXYATF8LV12X1L",
                ErpMnemonicCodeUtils.buildPinyinCode("美孚自动变速箱油 ATF 8LV, 12X1L"));
        assertEquals("MF1HJDBXOX0W20SQ12X1L",
                ErpMnemonicCodeUtils.buildPinyinCode("美孚1号经典表现欧系 0W-20 SQ 12X1L"));
    }

    @Test
    void buildPinyinCode_whenProductNameContainsJiHu_thenUsesExpectedInitials() {
        assertEquals("JHXLCP", ErpMnemonicCodeUtils.buildPinyinCode("极护系列产品"));
    }

    @Test
    void buildLegacyPinyinCode_keepsOldCollatorResultForGuardedBackfill() {
        assertEquals("MZ1HJDBXOX0W20SQ12X1L",
                ErpMnemonicCodeUtils.buildLegacyPinyinCode("美孚1号经典表现欧系 0W-20 SQ 12X1L"));
    }

}
