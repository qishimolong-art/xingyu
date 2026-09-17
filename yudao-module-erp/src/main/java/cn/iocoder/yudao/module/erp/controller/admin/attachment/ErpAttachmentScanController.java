package cn.iocoder.yudao.module.erp.controller.admin.attachment;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanCreateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanSessionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanStatusRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.attachment.vo.ErpAttachmentScanUploadRespVO;
import cn.iocoder.yudao.module.erp.service.attachment.ErpAttachmentScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "ERP 扫码上传附件")
@RestController
@RequestMapping("/erp/attachment-scan")
@Validated
public class ErpAttachmentScanController {

    private static final String ROUTER_MODE_HEADER = "X-Erp-Router-Mode";

    @Resource
    private ErpAttachmentScanService attachmentScanService;

    @PostMapping("/create")
    @Operation(summary = "创建扫码上传附件会话")
    public CommonResult<ErpAttachmentScanCreateRespVO> create(@Valid @RequestBody ErpAttachmentScanCreateReqVO reqVO,
                                                             HttpServletRequest request) {
        return success(attachmentScanService.createAttachmentScan(reqVO, resolveFrontendBaseUrl(request),
                isHashRoute(request)));
    }

    @GetMapping("/session")
    @PermitAll
    @Operation(summary = "获得扫码上传附件会话")
    @Parameter(name = "ticket", description = "临时票据", required = true)
    public CommonResult<ErpAttachmentScanSessionRespVO> getSession(@RequestParam("ticket") String ticket) {
        return success(attachmentScanService.getSession(ticket));
    }

    @PostMapping("/upload")
    @PermitAll
    @Operation(summary = "扫码上传附件")
    @Parameter(name = "file", description = "文件附件", required = true,
            schema = @Schema(type = "string", format = "binary"))
    public CommonResult<ErpAttachmentScanUploadRespVO> upload(@RequestParam("ticket") String ticket,
                                                             @RequestParam("file") MultipartFile file) {
        return success(attachmentScanService.upload(ticket, file));
    }

    @GetMapping("/status")
    @Operation(summary = "获得扫码上传附件状态")
    @Parameter(name = "sessionId", description = "会话编号", required = true)
    public CommonResult<ErpAttachmentScanStatusRespVO> getStatus(@RequestParam("sessionId") Long sessionId) {
        return success(attachmentScanService.getStatus(sessionId));
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消扫码上传附件会话")
    @Parameter(name = "sessionId", description = "会话编号", required = true)
    public CommonResult<Boolean> cancel(@RequestParam("sessionId") Long sessionId) {
        attachmentScanService.cancel(sessionId);
        return success(true);
    }

    private String resolveFrontendBaseUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (StrUtil.isNotBlank(referer)) {
            int hashIndex = referer.indexOf("/#/");
            if (hashIndex > 0) {
                return referer.substring(0, hashIndex);
            }
        }
        String origin = request.getHeader("Origin");
        if (StrUtil.isNotBlank(origin)) {
            return origin;
        }
        StringBuilder baseUrl = new StringBuilder(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (port > 0 && port != 80 && port != 443) {
            baseUrl.append(":").append(port);
        }
        return baseUrl.toString();
    }

    private boolean isHashRoute(HttpServletRequest request) {
        String routerMode = request.getHeader(ROUTER_MODE_HEADER);
        if (StrUtil.isNotBlank(routerMode)) {
            return "hash".equalsIgnoreCase(routerMode);
        }
        String referer = request.getHeader("Referer");
        return StrUtil.isNotBlank(referer) && referer.contains("/#/");
    }

}
