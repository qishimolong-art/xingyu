package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenVoucherConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenVoucherConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenVoucherConfigDO;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 系统开账")
@RestController
@RequestMapping("/erp/book-open")
@Validated
public class ErpBookOpenController {

    @Resource
    private ErpBookOpenService bookOpenService;

    @PostMapping("/create")
    @Operation(summary = "新增开账记录")
    @PreAuthorize("@ss.hasPermission('erp:book-open:create')")
    public CommonResult<Long> createBookOpen(@Valid @RequestBody ErpBookOpenSaveReqVO createReqVO) {
        return success(bookOpenService.createBookOpen(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新开账记录")
    @PreAuthorize("@ss.hasPermission('erp:book-open:update')")
    public CommonResult<Boolean> updateBookOpen(@Valid @RequestBody ErpBookOpenSaveReqVO updateReqVO) {
        bookOpenService.updateBookOpen(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除开账记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:book-open:delete')")
    public CommonResult<Boolean> deleteBookOpen(@RequestParam("id") Long id) {
        bookOpenService.deleteBookOpen(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得开账记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:book-open:query')")
    public CommonResult<ErpBookOpenRespVO> getBookOpen(@RequestParam("id") Long id) {
        ErpBookOpenDO bookOpen = bookOpenService.getBookOpen(id);
        if (bookOpen == null) {
            return success(null);
        }
        ErpBookOpenRespVO respVO = BeanUtils.toBean(bookOpen, ErpBookOpenRespVO.class);
        // 顺带带出凭证类型勾选
        List<ErpBookOpenVoucherConfigDO> configs = bookOpenService.getBookOpenVoucherConfigList(id);
        respVO.setVoucherConfigs(convertVoucherConfigs(configs));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "分页获得开账记录")
    @PreAuthorize("@ss.hasPermission('erp:book-open:query')")
    public CommonResult<PageResult<ErpBookOpenRespVO>> getBookOpenPage(@Valid ErpBookOpenPageReqVO pageReqVO) {
        PageResult<ErpBookOpenDO> pageResult = bookOpenService.getBookOpenPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpBookOpenRespVO.class));
    }

    @GetMapping("/voucher-config-list")
    @Operation(summary = "获得指定开账的凭证类型勾选列表")
    @Parameter(name = "bookOpenId", description = "开账主键", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:book-open:query')")
    public CommonResult<List<ErpBookOpenVoucherConfigRespVO>> getBookOpenVoucherConfigList(
            @RequestParam("bookOpenId") Long bookOpenId) {
        List<ErpBookOpenVoucherConfigDO> list = bookOpenService.getBookOpenVoucherConfigList(bookOpenId);
        return success(convertVoucherConfigs(list));
    }

    @PutMapping("/update-voucher-configs")
    @Operation(summary = "批量更新指定开账的凭证类型勾选")
    @PreAuthorize("@ss.hasPermission('erp:book-open:update')")
    public CommonResult<Boolean> updateBookOpenVoucherConfigs(
            @Valid @RequestBody ErpBookOpenVoucherConfigSaveReqVO reqVO) {
        bookOpenService.updateBookOpenVoucherConfigs(reqVO);
        return success(true);
    }

    private List<ErpBookOpenVoucherConfigRespVO> convertVoucherConfigs(List<ErpBookOpenVoucherConfigDO> list) {
        Map<Integer, String> typeNameMap = Arrays.stream(ErpVoucherTypeEnum.values())
                .collect(Collectors.toMap(ErpVoucherTypeEnum::getType, ErpVoucherTypeEnum::getName));
        return convertList(list, item -> {
            ErpBookOpenVoucherConfigRespVO vo = BeanUtils.toBean(item, ErpBookOpenVoucherConfigRespVO.class);
            vo.setVoucherTypeName(typeNameMap.get(item.getVoucherType()));
            return vo;
        });
    }

}
