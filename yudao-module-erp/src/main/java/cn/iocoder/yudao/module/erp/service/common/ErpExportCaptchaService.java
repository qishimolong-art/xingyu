package cn.iocoder.yudao.module.erp.service.common;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ERP_EXPORT_CAPTCHA_INVALID;

@Service
@Validated
public class ErpExportCaptchaService {

    private static final String TEMP_CODE = "123456";

    public void validate(String captchaCode, String verifyCode) {
        String code = StringUtils.hasText(captchaCode) ? captchaCode : verifyCode;
        if (!TEMP_CODE.equals(code)) {
            throw exception(ERP_EXPORT_CAPTCHA_INVALID);
        }
    }

}
