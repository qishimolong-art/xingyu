package cn.iocoder.yudao.framework.test.core.util;

import lombok.Data;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilsTest {

    @Test
    void randomPojo_shouldKeepCoordinatesWithinGeographicAndColumnLimits() {
        for (int i = 0; i < 100; i++) {
            CoordinatePojo pojo = RandomUtils.randomPojo(CoordinatePojo.class);
            assertTrue(pojo.getLongitude().abs().compareTo(new BigDecimal("180")) <= 0);
            assertTrue(pojo.getLatitude().abs().compareTo(new BigDecimal("90")) <= 0);
            assertEquals(6, pojo.getLongitude().scale());
            assertEquals(6, pojo.getLatitude().scale());
            assertTrue(pojo.getLongitude().precision() <= 10);
            assertTrue(pojo.getLatitude().precision() <= 10);
            // 经纬度生成规则不影响其他 BigDecimal 属性。
            assertEquals(2, pojo.getAmount().scale());
            assertTrue(pojo.getAmount().signum() >= 0);
            assertTrue(pojo.getAmount().compareTo(new BigDecimal("100000")) < 0);
        }
    }

    @Data
    public static class CoordinatePojo {
        private BigDecimal longitude;
        private BigDecimal latitude;
        private BigDecimal amount;
    }
}
