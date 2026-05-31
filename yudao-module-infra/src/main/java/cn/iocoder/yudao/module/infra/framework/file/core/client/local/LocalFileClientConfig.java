package cn.iocoder.yudao.module.infra.framework.file.core.client.local;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientConfig;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * 本地文件客户端的配置类
 */
@Data
public class LocalFileClientConfig implements FileClientConfig {

    /**
     * 基础路径
     */
    @NotEmpty(message = "基础路径不能为空")
    private String basePath;

    /**
     * 自定义域名，可为空；为空时返回相对路径
     */
    private String domain;

}
