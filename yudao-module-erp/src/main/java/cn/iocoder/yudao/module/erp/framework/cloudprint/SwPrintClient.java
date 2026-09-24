package cn.iocoder.yudao.module.erp.framework.cloudprint;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.framework.cloudprint.config.SwPrintProperties;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.DeviceInfo;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.PtFileData;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.PtFileReq;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.SwResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SwPrintClient {

    private static final MediaType HTML = MediaType.parse("text/html; charset=utf-8");
    private static final MediaType PDF = MediaType.parse("application/pdf");

    @Resource
    private SwPrintProperties properties;

    private OkHttpClient httpClient;

    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    public PtFileData ptFile(PtFileReq req, byte[] fileBytes, String fileName) {
        ensureConfigured();
        long times = System.currentTimeMillis();
        String token = SwPrintSignUtils.sign(properties.getUsername(), properties.getSecret(), times);
        MultipartBody.Builder body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("username", properties.getUsername())
                .addFormDataPart("times", String.valueOf(times))
                .addFormDataPart("devid", req.getDevid())
                .addFormDataPart("reqid", req.getReqid())
                .addFormDataPart("type", String.valueOf(req.getType()))
                .addFormDataPart("width", String.valueOf(req.getWidth()));
        addIfPresent(body, "height", req.getHeight());
        addIfPresent(body, "pcopy", req.getPcopy());
        addIfPresent(body, "ptype", req.getPtype());
        addIfPresent(body, "rotate", req.getRotate());
        body.addFormDataPart("file", fileName, RequestBody.create(resolveMediaType(req.getType()), fileBytes));
        String url = properties.getBaseUrl() + "/files/ptFile";
        String responseBody = execute(url, token, body.build());
        SwResponse<PtFileData> response = JsonUtils.parseObject(responseBody,
                new com.fasterxml.jackson.core.type.TypeReference<SwResponse<PtFileData>>() {});
        if (response == null || response.getCode() == null) {
            throw new SwPrintException(null, "云打印平台响应为空");
        }
        if (!Integer.valueOf(200).equals(response.getCode())
                || !Boolean.TRUE.equals(response.getSuccess())
                || response.getData() == null
                || !Boolean.TRUE.equals(response.getData().getSuccess())) {
            throw new SwPrintException(response.getCode(),
                    response.getData() != null && StringUtils.hasText(response.getData().getMessage())
                            ? response.getData().getMessage() : response.getMessage());
        }
        return response.getData();
    }

    public DeviceInfo getDevice(String devid) {
        ensureConfigured();
        long times = System.currentTimeMillis();
        String token = SwPrintSignUtils.sign(properties.getUsername(), properties.getSecret(), times);
        String url = buildGetDeviceUrl(devid, times);
        String responseBody = executeGet(url, token);
        SwResponse<DeviceInfo> response = JsonUtils.parseObject(responseBody,
                new com.fasterxml.jackson.core.type.TypeReference<SwResponse<DeviceInfo>>() {});
        if (response == null || !Integer.valueOf(200).equals(response.getCode()) || response.getData() == null) {
            throw new SwPrintException(response == null ? null : response.getCode(),
                    response == null ? "云打印平台响应为空" : response.getMessage());
        }
        return response.getData();
    }

    private String execute(String url, String token, RequestBody body) {
        long start = System.currentTimeMillis();
        Request request = new Request.Builder().url(url).post(body).header("token", token).build();
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            log.info("[sw-print][url({}) token(***) cost({}ms) response({})]",
                    url, System.currentTimeMillis() - start, responseBody);
            if (!response.isSuccessful()) {
                throw new SwPrintException(response.code(), "云打印平台暂不可用");
            }
            return responseBody;
        } catch (SocketTimeoutException ex) {
            throw new SwPrintException(null, "云打印平台提交超时，结果未知", true, ex);
        } catch (IOException ex) {
            throw new SwPrintException(null, "云打印平台暂不可用", false, ex);
        }
    }

    private String executeGet(String url, String token) {
        long start = System.currentTimeMillis();
        Request request = new Request.Builder().url(url).get().header("token", token).build();
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            log.info("[sw-print][url({}) token(***) cost({}ms) response({})]",
                    url, System.currentTimeMillis() - start, responseBody);
            if (!response.isSuccessful()) {
                throw new SwPrintException(response.code(), "云打印平台暂不可用");
            }
            return responseBody;
        } catch (SocketTimeoutException ex) {
            throw new SwPrintException(null, "云打印平台提交超时，结果未知", true, ex);
        } catch (IOException ex) {
            throw new SwPrintException(null, "云打印平台暂不可用", false, ex);
        }
    }

    private void ensureConfigured() {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            throw new SwPrintException(null, "云打印未启用");
        }
        if (!StringUtils.hasText(properties.getUsername()) || !StringUtils.hasText(properties.getSecret())) {
            throw new SwPrintException(null, "云打印账号凭据未配置");
        }
    }

    private String buildGetDeviceUrl(String devid, long times) {
        String rawUrl = properties.getBaseUrl() + "/api/printer/getDevice/" + devid;
        HttpUrl httpUrl = HttpUrl.parse(rawUrl);
        if (httpUrl == null) {
            throw new SwPrintException(null, "云打印平台地址配置错误");
        }
        return httpUrl.newBuilder()
                .addQueryParameter("username", properties.getUsername())
                .addQueryParameter("times", String.valueOf(times))
                .build()
                .toString();
    }

    private MediaType resolveMediaType(Integer type) {
        return Integer.valueOf(7).equals(type) ? PDF : HTML;
    }

    private void addIfPresent(MultipartBody.Builder body, String name, Object value) {
        if (value != null) {
            body.addFormDataPart(name, String.valueOf(value));
        }
    }

}
