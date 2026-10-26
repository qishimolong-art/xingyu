package cn.iocoder.yudao.framework.web.core.handler;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GlobalResponseBodyHandlerTest {

    private final GlobalResponseBodyHandler handler = new GlobalResponseBodyHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void recordsOriginalCommonResultWithoutChangingBody() {
        CommonResult<String> body = CommonResult.success("ok");
        assertSame(body, write(body));
        assertSame(body, WebFrameworkUtils.getCommonResult(request));
    }

    @Test
    void transformedBodyDoesNotThrowOrOverwriteRecordedResult() {
        CommonResult<String> original = CommonResult.success("ok");
        WebFrameworkUtils.setCommonResult(request, original);
        Object body = new ObjectMapper().valueToTree(original);

        assertSame(body, write(body));
        assertSame(original, WebFrameworkUtils.getCommonResult(request));
    }

    @Test
    void unrecordedTransformedBodyPassesThrough() {
        Object body = new ObjectMapper().createObjectNode().put("amount", "5.055556");
        assertSame(body, write(body));
        assertNull(WebFrameworkUtils.getCommonResult(request));
    }

    @Test
    void nullBodyPassesThrough() {
        assertNull(write(null));
        assertNull(WebFrameworkUtils.getCommonResult(request));
    }

    private Object write(Object body) {
        return handler.beforeBodyWrite(body, null, MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class, new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(new MockHttpServletResponse()));
    }
}
