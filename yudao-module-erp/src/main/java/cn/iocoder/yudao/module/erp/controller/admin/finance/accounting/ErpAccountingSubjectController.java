package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpOpeningBalanceUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpAccountingSubjectDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpSubjectAuxiliaryDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAccountingSubjectService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpSubjectAuxiliaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 会计科目")
@RestController
@RequestMapping("/erp/accounting-subject")
@Validated
public class ErpAccountingSubjectController {

    @Resource
    private ErpAccountingSubjectService subjectService;

    @Resource
    private ErpSubjectAuxiliaryService subjectAuxiliaryService;

    @PostMapping("/create")
    @Operation(summary = "创建会计科目")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:create')")
    public CommonResult<Long> createSubject(@Valid @RequestBody ErpAccountingSubjectSaveReqVO createReqVO) {
        return success(subjectService.createSubject(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会计科目")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:update')")
    public CommonResult<Boolean> updateSubject(@Valid @RequestBody ErpAccountingSubjectSaveReqVO updateReqVO) {
        subjectService.updateSubject(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会计科目")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:delete')")
    public CommonResult<Boolean> deleteSubject(@RequestParam("id") Long id) {
        subjectService.deleteSubject(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会计科目")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:query')")
    public CommonResult<ErpAccountingSubjectRespVO> getSubject(@RequestParam("id") Long id) {
        ErpAccountingSubjectDO subject = subjectService.getSubject(id);
        ErpAccountingSubjectRespVO vo = BeanUtils.toBean(subject, ErpAccountingSubjectRespVO.class);
        if (vo != null) {
            List<String> auxTypes = subjectAuxiliaryService.getListBySubjectId(id).stream()
                    .map(ErpSubjectAuxiliaryDO::getAuxiliaryType).collect(Collectors.toList());
            vo.setAuxiliaryTypes(auxTypes);
        }
        return success(vo);
    }

    @GetMapping("/list")
    @Operation(summary = "按大类返回科目树（前端 Tab 切换用）")
    @Parameter(name = "category", description = "科目大类：1-资产 2-负债 3-共同 4-权益 5-成本 6-损益")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:query')")
    public CommonResult<List<ErpAccountingSubjectRespVO>> getSubjectList(@RequestParam(value = "category", required = false) Integer category) {
        return success(subjectService.getSubjectTreeList(category));
    }

    @GetMapping("/tree")
    @Operation(summary = "返回所有科目树形结构")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:query')")
    public CommonResult<List<ErpAccountingSubjectRespVO>> getSubjectTree() {
        return success(subjectService.getSubjectTree());
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得科目精简列表（凭证录入下拉用）")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:query')")
    public CommonResult<List<ErpAccountingSubjectRespVO>> getSimpleList(
            @RequestParam(value = "leafOnly", required = false) Boolean leafOnly,
            @RequestParam(value = "category", required = false) Integer category) {
        List<ErpAccountingSubjectDO> list = subjectService.getSubjectSimpleList(leafOnly, category);
        List<ErpAccountingSubjectRespVO> result = list.stream().map(d -> {
            ErpAccountingSubjectRespVO vo = new ErpAccountingSubjectRespVO();
            vo.setId(d.getId());
            vo.setSubjectCode(d.getSubjectCode());
            vo.setSubjectName(d.getSubjectName());
            vo.setShortName(d.getShortName());
            vo.setSubjectCategory(d.getSubjectCategory());
            vo.setIsLeaf(d.getIsLeaf());
            vo.setBalanceDirection(d.getBalanceDirection());
            return vo;
        }).collect(Collectors.toList());
        return success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "会计科目分页")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:query')")
    public CommonResult<PageResult<ErpAccountingSubjectRespVO>> getSubjectPage(@Valid ErpAccountingSubjectPageReqVO pageReqVO) {
        PageResult<ErpAccountingSubjectDO> pageResult = subjectService.getSubjectPage(pageReqVO);
        PageResult<ErpAccountingSubjectRespVO> voPage = BeanUtils.toBean(pageResult, ErpAccountingSubjectRespVO.class);
        // 填辅助核算
        if (voPage.getList() != null && !voPage.getList().isEmpty()) {
            List<Long> ids = voPage.getList().stream().map(ErpAccountingSubjectRespVO::getId).collect(Collectors.toList());
            Map<Long, List<String>> auxMap = subjectAuxiliaryService.getListBySubjectIds(ids).stream()
                    .collect(Collectors.groupingBy(ErpSubjectAuxiliaryDO::getSubjectId,
                            Collectors.mapping(ErpSubjectAuxiliaryDO::getAuxiliaryType, Collectors.toList())));
            for (ErpAccountingSubjectRespVO vo : voPage.getList()) {
                vo.setAuxiliaryTypes(auxMap.getOrDefault(vo.getId(), new ArrayList<>()));
            }
        }
        return success(voPage);
    }

    @GetMapping("/get-by-code")
    @Operation(summary = "按科目编码查询")
    @Parameter(name = "code", description = "科目编码", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:query')")
    public CommonResult<ErpAccountingSubjectRespVO> getSubjectByCode(@RequestParam("code") String code) {
        ErpAccountingSubjectDO subject = subjectService.getSubjectByCode(code);
        if (subject == null) {
            return success(null);
        }
        ErpAccountingSubjectRespVO vo = BeanUtils.toBean(subject, ErpAccountingSubjectRespVO.class);
        List<String> auxTypes = subjectAuxiliaryService.getListBySubjectId(subject.getId()).stream()
                .map(ErpSubjectAuxiliaryDO::getAuxiliaryType).collect(Collectors.toList());
        vo.setAuxiliaryTypes(auxTypes);
        return success(vo);
    }

    @GetMapping("/auxiliary-list")
    @Operation(summary = "查询某科目挂的辅助核算类型列表")
    @Parameter(name = "subjectId", description = "科目编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:query')")
    public CommonResult<List<String>> getAuxiliaryListBySubject(@RequestParam("subjectId") Long subjectId) {
        List<String> types = subjectAuxiliaryService.getListBySubjectId(subjectId).stream()
                .map(ErpSubjectAuxiliaryDO::getAuxiliaryType).collect(Collectors.toList());
        return success(types);
    }

    @PutMapping("/batch-update-opening-balance")
    @Operation(summary = "批量保存期初余额")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:update')")
    public CommonResult<Boolean> batchUpdateOpeningBalance(@Valid @RequestBody ErpOpeningBalanceUpdateReqVO reqVO) {
        subjectService.batchUpdateOpeningBalance(reqVO);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出科目 Excel")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:query')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSubjectExcel(@Valid ErpAccountingSubjectPageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        List<ErpAccountingSubjectDO> list;
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            list = subjectService.getSubjectList(pageReqVO.getIds());
        } else {
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
            list = subjectService.getSubjectPage(pageReqVO).getList();
        }
        ExcelUtils.write(response, "会计科目.xls", "数据", ErpAccountingSubjectRespVO.class,
                BeanUtils.toBean(list, ErpAccountingSubjectRespVO.class));
    }

    @GetMapping("/export-template")
    @Operation(summary = "下载期初余额导入模板")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:update')")
    public void exportImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "期初余额导入模板.xls", "期初余额",
                ErpAccountingSubjectImportExcelVO.class,
                Collections.singletonList(new ErpAccountingSubjectImportExcelVO()));
    }

    @PostMapping("/import-excel")
    @Operation(summary = "导入期初余额 Excel")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:update')")
    public CommonResult<Map<String, String>> importOpeningBalance(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpAccountingSubjectImportExcelVO> list = ExcelUtils.read(file, ErpAccountingSubjectImportExcelVO.class);
        return success(subjectService.importOpeningBalance(list));
    }

    @PostMapping("/import-subjects")
    @Operation(summary = "导入会计科目（支持新增 + 重复处理）")
    @Parameter(name = "mode", description = "处理模式：DRY_RUN=预检 / OVERWRITE=覆盖 / SKIP=跳过；默认 DRY_RUN")
    @PreAuthorize("@ss.hasPermission('erp:accounting-subject:update')")
    public CommonResult<ErpAccountingSubjectImportRespVO> importSubjects(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", required = false) String mode) throws Exception {
        List<ErpAccountingSubjectImportExcelVO> list = ExcelUtils.read(file, ErpAccountingSubjectImportExcelVO.class);
        return success(subjectService.importSubjects(list, mode));
    }

}
