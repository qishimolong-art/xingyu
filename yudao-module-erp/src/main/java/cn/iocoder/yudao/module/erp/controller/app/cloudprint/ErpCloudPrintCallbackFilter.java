package cn.iocoder.yudao.module.erp.controller.app.cloudprint;

import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintCallbackConstants;
import cn.iocoder.yudao.module.erp.framework.cloudprint.config.SwPrintProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ErpCloudPrintCallbackFilter extends OncePerRequestFilter {

    private static final String CALLBACK_PATH = "/erp/print/callback";

    @Resource
    private SwPrintProperties properties;
    @Resource
    private WebProperties webProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = requestPath(request);
        String callbackBasePath = callbackBasePath();
        return !path.equals(callbackBasePath) && !path.startsWith(callbackBasePath + "/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = requestPath(request);
        String callbackBasePath = callbackBasePath();
        String pathToken = path.length() > callbackBasePath.length()
                ? path.substring(callbackBasePath.length() + 1) : null;
        if (!validPathToken(pathToken)) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "Not Found");
            return;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Allow", "POST");
            writeError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
            return;
        }
        if (!isJsonContentType(request.getContentType())) {
            writeError(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
            return;
        }
        if (request.getContentLengthLong() > ErpCloudPrintCallbackConstants.MAX_BODY_BYTES) {
            writeError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload Too Large");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean validPathToken(String pathToken) {
        String expected = properties.getCallbackPathToken();
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(pathToken)
                || pathToken.indexOf('/') >= 0) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                pathToken.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isJsonContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        try {
            return MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(contentType));
        } catch (Exception ex) {
            return false;
        }
    }

    private String callbackBasePath() {
        String prefix = webProperties.getAppApi().getPrefix();
        if (!StringUtils.hasText(prefix) || "/".equals(prefix)) {
            return CALLBACK_PATH;
        }
        return (prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix) + CALLBACK_PATH;
    }

    private static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return StringUtils.hasText(contextPath) && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length()) : uri;
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

}
