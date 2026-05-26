package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword.ErpVoucherWordSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherWordDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherWordService;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 凭证字字典")
@RestController
@RequestMapping("/erp/voucher-word")
@Validated
public class ErpVoucherWordController {

    @Resource
    private ErpVoucherWordService voucherWordService;

    @PostMapping("/create")
    @Operation(summary = "创建凭证字")
    @PreAuthorize("@ss.hasPermission('erp:voucher-word:create')")
    public CommonResult<Long> createVoucherWord(@Valid @RequestBody ErpVoucherWordSaveReqVO createReqVO) {
        return success(voucherWordService.createVoucherWord(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新凭证字")
    @PreAuthorize("@ss.hasPermission('erp:voucher-word:update')")
    public CommonResult<Boolean> updateVoucherWord(@Valid @RequestBody ErpVoucherWordSaveReqVO updateReqVO) {
        voucherWordService.updateVoucherWord(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除凭证字")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:voucher-word:delete')")
    public CommonResult<Boolean> deleteVoucherWord(@RequestParam("id") Long id) {
        voucherWordService.deleteVoucherWord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得凭证字")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:voucher-word:query')")
    public CommonResult<ErpVoucherWordRespVO> getVoucherWord(@RequestParam("id") Long id) {
        ErpVoucherWordDO voucherWord = voucherWordService.getVoucherWord(id);
        return success(BeanUtils.toBean(voucherWord, ErpVoucherWordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得凭证字分页")
    @PreAuthorize("@ss.hasPermission('erp:voucher-word:query')")
    public CommonResult<PageResult<ErpVoucherWordRespVO>> getVoucherWordPage(@Valid ErpVoucherWordPageReqVO pageReqVO) {
        PageResult<ErpVoucherWordDO> pageResult = voucherWordService.getVoucherWordPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpVoucherWordRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用的凭证字精简列表", description = "新增凭证时下拉用")
    @PreAuthorize("@ss.hasPermission('erp:voucher-word:query')")
    public CommonResult<List<ErpVoucherWordRespVO>> getVoucherWordSimpleList() {
        List<ErpVoucherWordDO> list = voucherWordService.getEnabledVoucherWordList();
        return success(convertList(list, voucherWord -> new ErpVoucherWordRespVO()
                .setId(voucherWord.getId())
                .setCode(voucherWord.getCode())
                .setName(voucherWord.getName())
                .setSort(voucherWord.getSort())));
    }

}
