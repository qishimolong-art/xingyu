package cn.iocoder.yudao.module.infra.framework.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yudao.file")
@Data
public class FileProperties {

    private final Local local = new Local();

    @Data
    public static class Local {

        private Boolean enabled = false;

        private String basePath = "file";

        private String domain = "";

    }

}
