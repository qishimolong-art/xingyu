package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.businesslicense.ErpCustomerBusinessLicenseOcrReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.businesslicense.ErpCustomerBusinessLicenseOcrRespVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Validated
@Slf4j
public class ErpCustomerBusinessLicenseOcrServiceImpl implements ErpCustomerBusinessLicenseOcrService {

    private static final String DEFAULT_ENDPOINT =
            "https://bizlicense.market.alicloudapi.com/rest/160601/ocr/ocr_business_license.json";

    @Value("${yudao.erp.business-license-ocr.endpoint:" + DEFAULT_ENDPOINT + "}")
    private String endpoint;
    @Value("${yudao.erp.business-license-ocr.app-code:}")
    private String appCode;
    @Value("${yudao.erp.business-license-ocr.timeout:8000}")
    private int timeout;

    @Override
    public ErpCustomerBusinessLicenseOcrRespVO recognize(ErpCustomerBusinessLicenseOcrReqVO reqVO) {
        if (StrUtil.isBlank(appCode)) {
            throw new ServiceException(1_020_200_100,
                    "营业执照识别未配置 AppCode，请配置 yudao.erp.business-license-ocr.app-code");
        }

        String image = normalizeImage(reqVO.getImage());
        JSONObject requestBody = JSONUtil.createObj().set("image", image);
        try (HttpResponse response = HttpRequest.post(endpoint)
                .header("Authorization", "APPCODE " + appCode)
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(requestBody.toString())
                .timeout(timeout)
                .execute()) {
            String body = response.body();
            if (!response.isOk()) {
                log.warn("Customer business license OCR http failed, status: {}, body: {}",
                        response.getStatus(), body);
                throw new ServiceException(1_020_200_101,
                        "营业执照识别接口请求失败：" + response.getStatus());
            }
            return parseResponse(body);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Customer business license OCR failed", ex);
            throw new ServiceException(1_020_200_102, "营业执照识别接口调用异常，请稍后重试");
        }
    }

    String normalizeImage(String image) {
        String normalizedImage = StrUtil.trimToEmpty(image);
        if (StrUtil.startWithIgnoreCase(normalizedImage, "data:")) {
            int commaIndex = normalizedImage.indexOf(',');
            if (commaIndex >= 0) {
                return normalizedImage.substring(commaIndex + 1);
            }
        }
        return normalizedImage;
    }

    ErpCustomerBusinessLicenseOcrRespVO parseResponse(String body) {
        JSONObject rawObject = JSONUtil.parseObj(body);
        Map<String, Object> raw = toMap(rawObject);
        Map<String, Object> data = extractData(rawObject);

        ErpCustomerBusinessLicenseOcrRespVO respVO = new ErpCustomerBusinessLicenseOcrRespVO();
        respVO.setSuccess(resolveSuccess(rawObject));
        respVO.setMessage(resolveMessage(rawObject));
        respVO.setData(data);
        respVO.setRaw(raw);

        String creditCode = firstText(rawObject,
                "reg_num", "regNum", "credit_code", "creditCode", "social_credit_code", "socialCreditCode",
                "unified_social_credit_code", "unifiedSocialCreditCode", "registration_number", "统一社会信用代码");
        respVO.setUnifiedCreditCode(creditCode);
        respVO.setTaxNo(firstNonBlank(firstText(rawObject, "tax_no", "taxNo", "taxpayer_id", "纳税人识别号"), creditCode));
        respVO.setInvoiceCompany(firstText(rawObject, "name", "company_name", "companyName", "名称"));
        respVO.setInvoiceAddress(firstText(rawObject, "address", "registered_address", "registeredAddress", "住所"));
        respVO.setLegalPerson(firstText(rawObject, "person", "legal_person", "legalPerson", "representative", "法定代表人"));
        respVO.setCompanyType(firstText(rawObject, "type", "company_type", "companyType", "类型"));
        respVO.setRegisteredCapital(firstText(rawObject, "capital", "registered_capital", "registeredCapital", "注册资本"));
        respVO.setEstablishDate(firstText(rawObject, "establish_date", "establishDate", "start_date", "startDate", "成立日期"));
        respVO.setValidPeriod(firstText(rawObject, "valid_period", "validPeriod", "valid_date", "validDate", "营业期限"));
        respVO.setBusinessScope(firstText(rawObject, "business", "business_scope", "businessScope", "scope", "经营范围"));
        return respVO;
    }

    private Map<String, Object> extractData(JSONObject rawObject) {
        Object dataObject = firstNonNull(rawObject.get("data"), rawObject.get("result"));
        if (dataObject instanceof JSONObject) {
            return toMap((JSONObject) dataObject);
        }
        if (dataObject instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) dataObject);
        }
        return toMap(rawObject);
    }

    private Boolean resolveSuccess(JSONObject rawObject) {
        Object success = firstNonNull(rawObject.get("success"), rawObject.get("status"), rawObject.get("code"),
                rawObject.get("error_code"), rawObject.get("errorCode"));
        if (success instanceof Boolean) {
            return (Boolean) success;
        }
        if (success instanceof Number) {
            int code = ((Number) success).intValue();
            return code == 0 || code == 200;
        }
        String text = success == null ? null : String.valueOf(success);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        return "true".equalsIgnoreCase(text) || "success".equalsIgnoreCase(text) || "ok".equalsIgnoreCase(text)
                || "0".equals(text) || "200".equals(text);
    }

    private String resolveMessage(JSONObject rawObject) {
        Object message = firstNonNull(rawObject.get("message"), rawObject.get("msg"), rawObject.get("reason"),
                rawObject.get("error_msg"), rawObject.get("errorMsg"));
        return message == null ? null : String.valueOf(message);
    }

    private String firstText(Object source, String... keys) {
        List<String> normalizedKeys = new ArrayList<>(keys.length);
        for (String key : keys) {
            normalizedKeys.add(normalizeJsonKey(key));
        }
        return toNonBlankString(findFirstValue(source, normalizedKeys));
    }

    private Object findFirstValue(Object source, List<String> normalizedKeys) {
        if (source instanceof JSONObject) {
            JSONObject object = (JSONObject) source;
            for (Map.Entry<String, Object> entry : object.entrySet()) {
                if (normalizedKeys.contains(normalizeJsonKey(entry.getKey()))) {
                    return entry.getValue();
                }
            }
            for (Object value : object.values()) {
                Object matched = findFirstValue(value, normalizedKeys);
                if (matched != null) {
                    return matched;
                }
            }
        } else if (source instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) source;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && normalizedKeys.contains(normalizeJsonKey(String.valueOf(entry.getKey())))) {
                    return entry.getValue();
                }
            }
            for (Object value : map.values()) {
                Object matched = findFirstValue(value, normalizedKeys);
                if (matched != null) {
                    return matched;
                }
            }
        } else if (source instanceof JSONArray) {
            for (Object item : (JSONArray) source) {
                Object matched = findFirstValue(item, normalizedKeys);
                if (matched != null) {
                    return matched;
                }
            }
        } else if (source instanceof Iterable) {
            for (Object item : (Iterable<?>) source) {
                Object matched = findFirstValue(item, normalizedKeys);
                if (matched != null) {
                    return matched;
                }
            }
        }
        return null;
    }

    private String normalizeJsonKey(String key) {
        return StrUtil.trimToEmpty(key)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .toLowerCase();
    }

    private String toNonBlankString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StrUtil.isBlank(text) ? null : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
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
