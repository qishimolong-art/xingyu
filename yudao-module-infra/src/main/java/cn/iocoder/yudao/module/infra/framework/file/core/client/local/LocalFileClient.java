package cn.iocoder.yudao.module.infra.framework.file.core.client.local;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.framework.file.core.client.AbstractFileClient;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件客户端
 *
 * @author 芋道源码
 */
public class LocalFileClient extends AbstractFileClient<LocalFileClientConfig> {

    public LocalFileClient(Long id, LocalFileClientConfig config) {
        super(id, config);
    }

    @Override
    protected void doInit() {
    }

    @Override
    public String upload(byte[] content, String path, String type) {
        // 执行写入
        String filePath = getFilePath(path);
        FileUtil.mkParentDirs(filePath);
        FileUtil.writeBytes(content, filePath);
        // 拼接返回路径
        return super.formatFileUrl(config.getDomain(), path);
    }

    @Override
    public void delete(String path) {
        String filePath = getFilePath(path);
        FileUtil.del(filePath);
    }

    @Override
    public byte[] getContent(String path) {
        String filePath = getFilePath(path);
        try {
            return FileUtil.readBytes(filePath);
        } catch (IORuntimeException ex) {
            if (ex.getMessage().startsWith("File not exist:")) {
                return null;
            }
            throw ex;
        }
    }

    private String getFilePath(String path) {
        String basePath = config.getBasePath();
        if (StrUtil.isNotBlank(basePath)) {
            return resolveBasePath(basePath).resolve(path).toString();
        }
        return "file" + File.separator + path;
    }

    private Path resolveBasePath(String basePath) {
        Path configuredPath = Paths.get(basePath);
        if (configuredPath.isAbsolute()) {
            return configuredPath.toAbsolutePath().normalize();
        }
        Path current = Paths.get("").toAbsolutePath().normalize();
        Path matched = null;
        while (current != null) {
            Path candidate = current.resolve(configuredPath).normalize();
            if (FileUtil.exist(candidate.toString())) {
                matched = candidate;
            }
            current = current.getParent();
        }
        if (matched != null) {
            return matched;
        }
        return configuredPath.toAbsolutePath().normalize();
    }

}
