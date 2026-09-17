package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.vin.ErpVinRecognizeRespVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ErpVinRecognizeServiceImpl implements ErpVinRecognizeService {

    private static final String DEFAULT_ENDPOINT = "https://slyvinstd.market.alicloudapi.com/vin/query";
    private static final String MODEL_LIST_KEY = "model_list";
    private static final String VIN_PATTERN = "^[A-HJ-NPR-Z0-9]{17}$";

    @Value("${yudao.erp.vin.endpoint:" + DEFAULT_ENDPOINT + "}")
    private String endpoint;
    @Value("${yudao.erp.vin.app-key:}")
    private String appKey;
    @Value("${yudao.erp.vin.app-secret:}")
    private String appSecret;
    @Value("${yudao.erp.vin.app-code:}")
    private String appCode;
    @Value("${yudao.erp.vin.timeout:8000}")
    private int timeout;

    @Override
    public ErpVinRecognizeRespVO recognize(String vin) {
        String normalizedVin = normalizeVin(vin);
        if (!normalizedVin.matches(VIN_PATTERN)) {
            throw new ServiceException(1_030_209_000, "VIN码必须是17位字母或数字，且不能包含 I、O、Q");
        }
        if (StrUtil.isBlank(appCode)) {
            throw new ServiceException(1_030_209_001, "VIN识别未配置 AppCode，请配置 yudao.erp.vin.app-code");
        }

        try (HttpResponse response = HttpRequest.get(endpoint)
                .header("Authorization", "APPCODE " + appCode)
                .form("vin", normalizedVin)
                .timeout(timeout)
                .execute()) {
            String body = response.body();
            if (!response.isOk()) {
                log.warn("VIN recognize http failed, status: {}, body: {}", response.getStatus(), body);
                throw new ServiceException(1_030_209_002, "VIN识别接口请求失败：" + response.getStatus());
            }
            return parseResponse(normalizedVin, body);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("VIN recognize failed, vin: {}", normalizedVin, ex);
            throw new ServiceException(1_030_209_003, "VIN识别接口调用异常，请稍后重试");
        }
    }

    String normalizeVin(String vin) {
        return StrUtil.trimToEmpty(vin).toUpperCase();
    }

    ErpVinRecognizeRespVO parseResponse(String vin, String body) {
        JSONObject rawObject = JSONUtil.parseObj(body);
        Map<String, Object> raw = toMap(rawObject);
        Map<String, Object> data = extractData(rawObject);
        List<Map<String, Object>> modelList = extractModelList(rawObject, data);

        ErpVinRecognizeRespVO respVO = new ErpVinRecognizeRespVO();
        respVO.setVin(vin);
        respVO.setSuccess(resolveSuccess(rawObject));
        respVO.setMessage(resolveMessage(rawObject));
        respVO.setData(data);
        respVO.setModelList(modelList);
        respVO.setRaw(raw);
        return respVO;
    }

    private Map<String, Object> extractData(JSONObject rawObject) {
        Object dataObject = rawObject.get("data");
        if (dataObject instanceof JSONObject) {
            JSONObject dataJsonObject = (JSONObject) dataObject;
            Object resultObject = dataJsonObject.get("result");
            if (resultObject instanceof JSONObject) {
                return toMap((JSONObject) resultObject);
            }
            if (resultObject instanceof Map) {
                return new LinkedHashMap<>((Map<String, Object>) resultObject);
            }
            return toMap(dataJsonObject);
        }
        if (dataObject instanceof Map) {
            Map<String, Object> dataMap = new LinkedHashMap<>((Map<String, Object>) dataObject);
            Object resultObject = dataMap.get("result");
            if (resultObject instanceof JSONObject) {
                return toMap((JSONObject) resultObject);
            }
            if (resultObject instanceof Map) {
                return new LinkedHashMap<>((Map<String, Object>) resultObject);
            }
            return dataMap;
        }
        Object resultObject = rawObject.get("result");
        if (resultObject instanceof JSONObject) {
            return toMap((JSONObject) resultObject);
        }
        if (resultObject instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) resultObject);
        }
        return toMap(rawObject);
    }

    private List<Map<String, Object>> extractModelList(JSONObject rawObject, Map<String, Object> data) {
        Object modelListObject = firstNonNull(data.get(MODEL_LIST_KEY), rawObject.get(MODEL_LIST_KEY));
        List<Map<String, Object>> modelList = new ArrayList<>();
        if (modelListObject instanceof JSONArray) {
            for (Object item : (JSONArray) modelListObject) {
                addModelItem(modelList, item);
            }
        } else if (modelListObject instanceof Iterable) {
            for (Object item : (Iterable<?>) modelListObject) {
                addModelItem(modelList, item);
            }
        }
        data.remove(MODEL_LIST_KEY);
        return modelList;
    }

    private void addModelItem(List<Map<String, Object>> modelList, Object item) {
        if (item instanceof JSONObject) {
            modelList.add(toMap((JSONObject) item));
        } else if (item instanceof Map) {
            modelList.add(new LinkedHashMap<>((Map<String, Object>) item));
        }
    }

    private Boolean resolveSuccess(JSONObject rawObject) {
        Object success = firstNonNull(rawObject.get("success"), rawObject.get("status"), rawObject.get("code"));
        if (success instanceof Boolean) {
            return (Boolean) success;
        }
        if (success instanceof Number) {
            return ((Number) success).intValue() == 0 || ((Number) success).intValue() == 200;
        }
        String text = success == null ? null : String.valueOf(success);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        return "true".equalsIgnoreCase(text) || "success".equalsIgnoreCase(text) || "ok".equalsIgnoreCase(text)
                || "0".equals(text) || "200".equals(text);
    }

    private String resolveMessage(JSONObject rawObject) {
        Object message = firstNonNull(rawObject.get("msg"), rawObject.get("message"), rawObject.get("reason"),
                rawObject.get("error_msg"), rawObject.get("errorMsg"));
        return message == null ? null : String.valueOf(message);
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Map<String, Object> toMap(JSONObject object) {
        Map<String, Object> map = new LinkedHashMap<>();
        object.forEach((key, value) -> map.put(key, normalizeJsonValue(value)));
        return map;
    }

    private Object normalizeJsonValue(Object value) {
        if (value instanceof JSONObject) {
            return toMap((JSONObject) value);
        }
        if (value instanceof JSONArray) {
            List<Object> list = new ArrayList<>();
            for (Object item : (JSONArray) value) {
                list.add(normalizeJsonValue(item));
            }
            return list;
        }
        return value;
    }

}
