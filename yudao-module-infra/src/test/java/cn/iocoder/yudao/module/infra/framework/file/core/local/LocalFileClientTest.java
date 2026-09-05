package cn.iocoder.yudao.module.infra.framework.file.core.local;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.module.infra.framework.file.core.client.local.LocalFileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.local.LocalFileClientConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalFileClientTest {

    @TempDir
    private java.nio.file.Path tempDir;

    @Test
    public void testUpload_blankDomainReturnsRelativeUrl() {
        LocalFileClientConfig config = new LocalFileClientConfig();
        config.setDomain("");
        config.setBasePath(tempDir.toString());
        LocalFileClient client = new LocalFileClient(-1L, config);
        client.init();

        byte[] content = new byte[]{1, 2, 3};
        String url = client.upload(content, "erp/sale-out/express/1/test.png", "image/png");

        assertEquals("/admin-api/infra/file/-1/get/erp/sale-out/express/1/test.png", url);
        assertArrayEquals(content, client.getContent("erp/sale-out/express/1/test.png"));
    }

    @Test
    @Disabled
    public void test() {
        // 创建客户端
        LocalFileClientConfig config = new LocalFileClientConfig();
        config.setDomain("http://127.0.0.1:48080");
        config.setBasePath("/Users/yunai/file_test");
        LocalFileClient client = new LocalFileClient(0L, config);
        client.init();
        // 上传文件
        String path = IdUtil.fastSimpleUUID() + ".jpg";
        byte[] content = ResourceUtil.readBytes("file/erweima.jpg");
        String fullPath = client.upload(content, path, "image/jpeg");
        System.out.println("访问地址：" + fullPath);
        client.delete(path);
    }

    @Test
    @Disabled
    public void testGetContent_notFound() {
        // 创建客户端
        LocalFileClientConfig config = new LocalFileClientConfig();
        config.setDomain("http://127.0.0.1:48080");
        config.setBasePath("/Users/yunai/file_test");
        LocalFileClient client = new LocalFileClient(0L, config);
        client.init();
        // 上传文件
        byte[] content = client.getContent(randomString());
        System.out.println();
    }

}
