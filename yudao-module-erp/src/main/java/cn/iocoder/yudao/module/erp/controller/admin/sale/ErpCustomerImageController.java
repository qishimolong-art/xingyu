package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImagePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImageRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImageSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerImageDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 客户图片")
@RestController
@RequestMapping("/erp/customer-image")
@Validated
public class ErpCustomerImageController {

    @Resource
    private ErpCustomerImageService imageService;

    @PostMapping("/create")
    @Operation(summary = "创建客户图片")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Long> createImage(@Valid @RequestBody ErpCustomerImageSaveReqVO createReqVO) {
        return success(imageService.createImage(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户图片")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> updateImage(@Valid @RequestBody ErpCustomerImageSaveReqVO updateReqVO) {
        imageService.updateImage(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户图片")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> deleteImage(@RequestParam("id") Long id) {
        imageService.deleteImage(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户图片")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<ErpCustomerImageRespVO> getImage(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(imageService.getImage(id), ErpCustomerImageRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户图片分页")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<ErpCustomerImageRespVO>> getImagePage(@Valid ErpCustomerImagePageReqVO pageReqVO) {
        PageResult<ErpCustomerImageDO> pageResult = imageService.getImagePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCustomerImageRespVO.class));
    }

    @GetMapping("/list-by-customer")
    @Operation(summary = "获得客户图片列表")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<List<ErpCustomerImageRespVO>> getImageListByCustomer(@RequestParam("customerId") Long customerId) {
        return success(BeanUtils.toBean(imageService.getImageListByCustomerId(customerId), ErpCustomerImageRespVO.class));
    }

}
