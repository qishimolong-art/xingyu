package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpOpeningBalanceUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpAccountingSubjectDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpSubjectAuxiliaryDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpAccountingSubjectMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNTING_SUBJECT_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNTING_SUBJECT_HAS_CHILDREN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNTING_SUBJECT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNTING_SUBJECT_USED_BY_VOUCHER;

/**
 * ERP 会计科目 Service 实现
 *
 * @author Claude
 */
@Service
@Validated
public class ErpAccountingSubjectServiceImpl implements ErpAccountingSubjectService {

    @Resource
    private ErpAccountingSubjectMapper subjectMapper;

    @Resource
    private ErpSubjectAuxiliaryService subjectAuxiliaryService;

    @Resource
    private ErpVoucherItemMapper voucherItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSubject(ErpAccountingSubjectSaveReqVO createReqVO) {
        // 1. 校验编码唯一
        ErpAccountingSubjectDO existing = subjectMapper.selectBySubjectCode(createReqVO.getSubjectCode());
        if (existing != null) {
            throw exception(ACCOUNTING_SUBJECT_CODE_DUPLICATE, createReqVO.getSubjectCode());
        }
        // 2. 推断父科目编码 / 层级
        ErpAccountingSubjectDO subject = BeanUtils.toBean(createReqVO, ErpAccountingSubjectDO.class);
        inferParentAndLevel(subject);
        ErpAccountingSubjectDO parent = validateParentSubjectIfNecessary(subject);
        // 3. 默认值
        if (subject.getIsLeaf() == null) {
            subject.setIsLeaf(true);
        }
        if (subject.getEnable() == null) {
            subject.setEnable(true);
        }
        if (subject.getSort() == null) {
            subject.setSort(0);
        }
        if (subject.getOpeningBalance() == null) {
            subject.setOpeningBalance(BigDecimal.ZERO);
        }
        // 4. 父科目存在则把它从末级翻成非末级
        if (parent != null && Boolean.TRUE.equals(parent.getIsLeaf())) {
            ErpAccountingSubjectDO patch = new ErpAccountingSubjectDO();
            patch.setId(parent.getId());
            patch.setIsLeaf(false);
            subjectMapper.updateById(patch);
        }
        // 5. 落库
        subjectMapper.insert(subject);
        // 6. 保存辅助核算
        subjectAuxiliaryService.saveSubjectAuxiliary(subject.getId(), subject.getSubjectCode(),
                createReqVO.getAuxiliaryTypes());
        return subject.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubject(ErpAccountingSubjectSaveReqVO updateReqVO) {
        // 1. 校验存在
        ErpAccountingSubjectDO old = validateSubject(updateReqVO.getId());
        ErpAccountingSubjectDO inferred = null;
        if (updateReqVO.getSubjectCode() != null && !updateReqVO.getSubjectCode().equals(old.getSubjectCode())) {
            inferred = BeanUtils.toBean(updateReqVO, ErpAccountingSubjectDO.class);
            inferParentAndLevel(inferred);
            validateParentSubjectIfNecessary(inferred);
        }
        // 2. 校验编码若变化则唯一
        if (updateReqVO.getSubjectCode() != null && !updateReqVO.getSubjectCode().equals(old.getSubjectCode())) {
            ErpAccountingSubjectDO existing = subjectMapper.selectBySubjectCode(updateReqVO.getSubjectCode());
            if (existing != null && !existing.getId().equals(old.getId())) {
                throw exception(ACCOUNTING_SUBJECT_CODE_DUPLICATE, updateReqVO.getSubjectCode());
            }
            // 级联更新子科目 parentCode（S8 修复：改编码不级联导致树断链）
            List<ErpAccountingSubjectDO> children = subjectMapper.selectList(
                    new LambdaQueryWrapper<ErpAccountingSubjectDO>()
                            .eq(ErpAccountingSubjectDO::getParentCode, old.getSubjectCode()));
            if (CollUtil.isNotEmpty(children)) {
                for (ErpAccountingSubjectDO child : children) {
                    ErpAccountingSubjectDO patch = new ErpAccountingSubjectDO();
                    patch.setId(child.getId());
                    patch.setParentCode(updateReqVO.getSubjectCode());
                    subjectMapper.updateById(patch);
                }
            }
        }
        // 3. 更新字段（不允许更新 isLeaf，由 createSubject/deleteSubject 自动维护）
        ErpAccountingSubjectDO update = BeanUtils.toBean(updateReqVO, ErpAccountingSubjectDO.class);
        if (inferred != null) {
            update.setParentCode(inferred.getParentCode());
            update.setSubjectLevel(inferred.getSubjectLevel());
        }
        update.setIsLeaf(null);
        subjectMapper.updateById(update);
        // 4. 同步辅助核算（auxiliaryTypes 为 null 表示不修改；空集合表示清空）
        if (updateReqVO.getAuxiliaryTypes() != null) {
            subjectAuxiliaryService.saveSubjectAuxiliary(old.getId(),
                    updateReqVO.getSubjectCode() != null ? updateReqVO.getSubjectCode() : old.getSubjectCode(),
                    updateReqVO.getAuxiliaryTypes());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubject(Long id) {
        ErpAccountingSubjectDO subject = validateSubject(id);
        // 1. 校验无子科目
        List<ErpAccountingSubjectDO> children = subjectMapper.selectListByParentCode(subject.getSubjectCode());
        if (!children.isEmpty()) {
            throw exception(ACCOUNTING_SUBJECT_HAS_CHILDREN, subject.getSubjectCode());
        }
        // 2. 校验未被凭证引用
        Long usedCount = voucherItemMapper.selectCountBySubjectId(id);
        if (usedCount != null && usedCount > 0) {
            throw exception(ACCOUNTING_SUBJECT_USED_BY_VOUCHER, subject.getSubjectCode());
        }
        // 3. 删除主表
        subjectMapper.deleteById(id);
        // 4. 清辅助核算
        subjectAuxiliaryService.deleteBySubjectId(id);
        // 5. 父科目若已无其它子科目，回填 isLeaf=true
        if (subject.getParentCode() != null) {
            List<ErpAccountingSubjectDO> siblings = subjectMapper.selectListByParentCode(subject.getParentCode());
            if (siblings.isEmpty()) {
                ErpAccountingSubjectDO parent = subjectMapper.selectBySubjectCode(subject.getParentCode());
                if (parent != null && !Boolean.TRUE.equals(parent.getIsLeaf())) {
                    ErpAccountingSubjectDO patch = new ErpAccountingSubjectDO();
                    patch.setId(parent.getId());
                    patch.setIsLeaf(true);
                    subjectMapper.updateById(patch);
                }
            }
        }
    }

    @Override
    public ErpAccountingSubjectDO getSubject(Long id) {
        return subjectMapper.selectById(id);
    }

    @Override
    public ErpAccountingSubjectDO validateSubject(Long id) {
        ErpAccountingSubjectDO subject = subjectMapper.selectById(id);
        if (subject == null) {
            throw exception(ACCOUNTING_SUBJECT_NOT_EXISTS);
        }
        return subject;
    }

    @Override
    public ErpAccountingSubjectDO getSubjectByCode(String subjectCode) {
        if (subjectCode == null || subjectCode.isEmpty()) {
            return null;
        }
        return subjectMapper.selectBySubjectCode(subjectCode);
    }

    @Override
    public List<ErpAccountingSubjectDO> getSubjectList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return subjectMapper.selectByIds(ids);
    }

    @Override
    public Map<Long, ErpAccountingSubjectDO> getSubjectMap(Collection<Long> ids) {
        return CollectionUtils.convertMap(getSubjectList(ids), ErpAccountingSubjectDO::getId);
    }

    @Override
    public List<ErpAccountingSubjectRespVO> getSubjectTreeList(Integer category) {
        List<ErpAccountingSubjectDO> list = category != null
                ? subjectMapper.selectListByCategory(category)
                : subjectMapper.selectListAllOrderByCode();
        return buildTree(list);
    }

    @Override
    public List<ErpAccountingSubjectRespVO> getSubjectTree() {
        return getSubjectTreeList(null);
    }

    @Override
    public List<ErpAccountingSubjectDO> getSubjectSimpleList(Boolean leafOnly, Integer category) {
        if (Boolean.TRUE.equals(leafOnly) && category != null) {
            // 取该大类下末级
            List<ErpAccountingSubjectDO> list = subjectMapper.selectListByCategory(category);
            return list.stream().filter(s -> Boolean.TRUE.equals(s.getIsLeaf())).collect(Collectors.toList());
        }
        if (Boolean.TRUE.equals(leafOnly)) {
            return subjectMapper.selectListByIsLeaf(true);
        }
        if (category != null) {
            return subjectMapper.selectListByCategory(category);
        }
        return subjectMapper.selectListAllOrderByCode();
    }

    @Override
    public List<ErpAccountingSubjectDO> getLeafSubjectList() {
        return subjectMapper.selectListByIsLeaf(true);
    }

    @Override
    public PageResult<ErpAccountingSubjectDO> getSubjectPage(ErpAccountingSubjectPageReqVO pageReqVO) {
        return subjectMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateOpeningBalance(ErpOpeningBalanceUpdateReqVO reqVO) {
        if (reqVO == null || reqVO.getItems() == null || reqVO.getItems().isEmpty()) {
            return;
        }
        // S10 修复：批量更新前校验所有 id 都存在
        List<Long> ids = reqVO.getItems().stream()
                .map(ErpOpeningBalanceUpdateReqVO.Item::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!ids.isEmpty()) {
            List<ErpAccountingSubjectDO> existing = subjectMapper.selectByIds(ids);
            if (existing.size() != ids.size()) {
                throw exception(ACCOUNTING_SUBJECT_NOT_EXISTS);
            }
        }
        for (ErpOpeningBalanceUpdateReqVO.Item item : reqVO.getItems()) {
            if (item.getId() == null) {
                continue;
            }
            ErpAccountingSubjectDO patch = new ErpAccountingSubjectDO();
            patch.setId(item.getId());
            patch.setOpeningBalance(item.getOpeningBalance() != null ? item.getOpeningBalance() : BigDecimal.ZERO);
            subjectMapper.updateById(patch);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> importOpeningBalance(List<ErpAccountingSubjectImportExcelVO> list) {
        Map<String, String> result = new LinkedHashMap<>();
        if (list == null || list.isEmpty()) {
            return result;
        }
        // 预查所有需要的科目
        List<String> codes = list.stream().map(ErpAccountingSubjectImportExcelVO::getSubjectCode)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        Map<String, ErpAccountingSubjectDO> codeMap = new HashMap<>();
        if (!codes.isEmpty()) {
            for (ErpAccountingSubjectDO s : subjectMapper.selectListBySubjectCodes(codes)) {
                codeMap.put(s.getSubjectCode(), s);
            }
        }
        for (ErpAccountingSubjectImportExcelVO row : list) {
            if (row == null || row.getSubjectCode() == null || row.getSubjectCode().isEmpty()) {
                continue;
            }
            ErpAccountingSubjectDO subject = codeMap.get(row.getSubjectCode());
            if (subject == null) {
                result.put(row.getSubjectCode(), "科目不存在，请先建立科目");
                continue;
            }
            ErpAccountingSubjectDO patch = new ErpAccountingSubjectDO();
            patch.setId(subject.getId());
            patch.setOpeningBalance(row.getOpeningBalance() != null ? row.getOpeningBalance() : BigDecimal.ZERO);
            subjectMapper.updateById(patch);
            result.put(row.getSubjectCode(), null);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpAccountingSubjectImportRespVO importSubjects(List<ErpAccountingSubjectImportExcelVO> list, String mode) {
        ErpAccountingSubjectImportRespVO resp = new ErpAccountingSubjectImportRespVO();
        String normalizedMode = mode == null ? "DRY_RUN" : mode.toUpperCase();
        boolean dryRun = "DRY_RUN".equals(normalizedMode);
        resp.setDryRun(dryRun);
        if (list == null || list.isEmpty()) {
            return resp;
        }
        // S10 修复：拓扑排序，确保父科目在子科目之前处理，避免新建子科目时 inferParentAndLevel 查不到父
        list = sortByParentDependency(list,
                ErpAccountingSubjectImportExcelVO::getSubjectCode,
                ErpAccountingSubjectImportExcelVO::getParentCode);
        // 预查所有 subjectCode 现状
        List<String> codes = list.stream().map(ErpAccountingSubjectImportExcelVO::getSubjectCode)
                .filter(c -> c != null && !c.isEmpty())
                .distinct().collect(Collectors.toList());
        Map<String, ErpAccountingSubjectDO> existingMap = new HashMap<>();
        if (!codes.isEmpty()) {
            for (ErpAccountingSubjectDO s : subjectMapper.selectListBySubjectCodes(codes)) {
                existingMap.put(s.getSubjectCode(), s);
            }
        }
        for (ErpAccountingSubjectImportExcelVO row : list) {
            if (row == null || row.getSubjectCode() == null || row.getSubjectCode().trim().isEmpty()) {
                continue;
            }
            String code = row.getSubjectCode().trim();
            ErpAccountingSubjectDO existing = existingMap.get(code);
            if (existing != null) {
                resp.getExistedCodes().add(code);
                if (dryRun) {
                    continue;
                }
                if ("OVERWRITE".equals(normalizedMode)) {
                    try {
                        ErpAccountingSubjectDO patch = buildPatchFromRow(row, existing);
                        subjectMapper.updateById(patch);
                        resp.getUpdatedCodes().add(code);
                    } catch (Exception e) {
                        resp.getFailures().add(new ErpAccountingSubjectImportRespVO.FailureItem(code, e.getMessage()));
                    }
                } else { // SKIP
                    resp.getSkippedCodes().add(code);
                }
            } else {
                if (dryRun) {
                    // 预检：不写库
                    continue;
                }
                try {
                    ErpAccountingSubjectDO created = buildNewSubjectFromRow(row);
                    subjectMapper.insert(created);
                    resp.getCreatedCodes().add(code);
                } catch (Exception e) {
                    resp.getFailures().add(new ErpAccountingSubjectImportRespVO.FailureItem(code, e.getMessage()));
                }
            }
        }
        return resp;
    }

    /** Excel 行 → 更新补丁（保留已有 id，覆盖名称/类型/方向/余额等） */
    private ErpAccountingSubjectDO buildPatchFromRow(ErpAccountingSubjectImportExcelVO row,
                                                    ErpAccountingSubjectDO existing) {
        ErpAccountingSubjectDO patch = new ErpAccountingSubjectDO();
        patch.setId(existing.getId());
        if (row.getSubjectName() != null && !row.getSubjectName().trim().isEmpty()) {
            patch.setSubjectName(row.getSubjectName().trim());
        }
        if (row.getShortName() != null) {
            patch.setShortName(row.getShortName().trim());
        }
        Integer category = parseSubjectCategory(row.getSubjectCategory());
        if (category != null) {
            patch.setSubjectCategory(category);
        }
        if (row.getParentCode() != null) {
            patch.setParentCode(row.getParentCode().trim().isEmpty() ? null : row.getParentCode().trim());
        }
        Integer direction = parseBalanceDirection(row.getBalanceDirection());
        if (direction != null) {
            patch.setBalanceDirection(direction);
        }
        Integer voucherType = parseVoucherType(row.getVoucherType());
        if (voucherType != null) {
            patch.setVoucherType(voucherType);
        }
        if (row.getOpeningBalance() != null) {
            patch.setOpeningBalance(row.getOpeningBalance());
        }
        return patch;
    }

    /** Excel 行 → 新建科目 DO（必填字段缺失时抛异常给上层捕获） */
    private ErpAccountingSubjectDO buildNewSubjectFromRow(ErpAccountingSubjectImportExcelVO row) {
        ErpAccountingSubjectDO subject = new ErpAccountingSubjectDO();
        subject.setSubjectCode(row.getSubjectCode().trim());
        if (row.getSubjectName() == null || row.getSubjectName().trim().isEmpty()) {
            throw new IllegalArgumentException("科目名称不能为空");
        }
        subject.setSubjectName(row.getSubjectName().trim());
        subject.setShortName(row.getShortName() != null ? row.getShortName().trim() : null);
        Integer category = parseSubjectCategory(row.getSubjectCategory());
        if (category == null) {
            throw new IllegalArgumentException("科目类型解析失败：" + row.getSubjectCategory());
        }
        subject.setSubjectCategory(category);
        if (row.getParentCode() != null && !row.getParentCode().trim().isEmpty()) {
            subject.setParentCode(row.getParentCode().trim());
        }
        Integer direction = parseBalanceDirection(row.getBalanceDirection());
        if (direction != null) {
            subject.setBalanceDirection(direction);
        }
        Integer voucherType = parseVoucherType(row.getVoucherType());
        if (voucherType != null) {
            subject.setVoucherType(voucherType);
        }
        subject.setOpeningBalance(row.getOpeningBalance() != null ? row.getOpeningBalance() : BigDecimal.ZERO);
        subject.setEnable(true);
        subject.setSort(0);
        // 推断层级 / 末级标记
        inferParentAndLevel(subject);
        if (subject.getIsLeaf() == null) {
            subject.setIsLeaf(true);
        }
        return subject;
    }

    private Integer parseSubjectCategory(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        switch (t) {
            case "1": case "资产": return 1;
            case "2": case "负债": return 2;
            case "3": case "共同": return 3;
            case "4": case "权益": return 4;
            case "5": case "成本": return 5;
            case "6": case "损益": return 6;
            default:
                // S9 修复：default 分支拒绝非法值，仅接受 1~6 范围内的合法 int
                try {
                    int v = Integer.parseInt(t);
                    if (v >= 1 && v <= 6) {
                        return v;
                    }
                    return null;
                } catch (NumberFormatException ignored) {
                    return null;
                }
        }
    }

    private Integer parseBalanceDirection(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        switch (t) {
            case "1": case "借": case "借方": return 1;
            case "2": case "贷": case "贷方": return 2;
            default:
                try { return Integer.parseInt(t); } catch (Exception ignored) { return null; }
        }
    }

    private Integer parseVoucherType(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        switch (t) {
            case "1": case "客户": return 1;
            case "2": case "连锁": return 2;
            default:
                try { return Integer.parseInt(t); } catch (Exception ignored) { return null; }
        }
    }

    // ============== 私有辅助方法 ==============

    /**
     * 根据 parentCode / subjectCode 推断父科目编码 + 层级
     */
    private void inferParentAndLevel(ErpAccountingSubjectDO subject) {
        if (subject.getParentCode() != null && !subject.getParentCode().isEmpty()) {
            ErpAccountingSubjectDO parent = subjectMapper.selectBySubjectCode(subject.getParentCode());
            if (parent != null && parent.getSubjectLevel() != null) {
                subject.setSubjectLevel(parent.getSubjectLevel() + 1);
            }
            return;
        }
        // 无 parentCode：按 subjectCode 长度推断（约定 4 位倍数 1->2->3 级）
        String code = subject.getSubjectCode();
        if (code == null) {
            return;
        }
        if (code.length() <= 4) {
            subject.setSubjectLevel(1);
            subject.setParentCode(null);
        } else if (code.length() % 2 == 0) {
            subject.setSubjectLevel(code.length() / 2 - 1);
            subject.setParentCode(code.substring(0, code.length() - 2));
        } else {
            // 兜底：层级置 null，由调用方处理
            subject.setSubjectLevel(null);
        }
    }

    private ErpAccountingSubjectDO validateParentSubjectIfNecessary(ErpAccountingSubjectDO subject) {
        if (subject.getParentCode() == null) {
            return null;
        }
        ErpAccountingSubjectDO parent = subjectMapper.selectBySubjectCode(subject.getParentCode());
        if (parent == null) {
            throw exception(ACCOUNTING_SUBJECT_NOT_EXISTS);
        }
        if (subject.getSubjectLevel() == null && parent.getSubjectLevel() != null) {
            subject.setSubjectLevel(parent.getSubjectLevel() + 1);
        }
        return parent;
    }

    /**
     * 根据列表构造树形结构（按 parentCode 关联）
     */
    private List<ErpAccountingSubjectRespVO> buildTree(List<ErpAccountingSubjectDO> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        // 顺便填 auxiliaryTypes
        List<Long> ids = list.stream().map(ErpAccountingSubjectDO::getId).collect(Collectors.toList());
        Map<Long, List<String>> auxMap = new HashMap<>();
        for (ErpSubjectAuxiliaryDO aux : subjectAuxiliaryService.getListBySubjectIds(ids)) {
            auxMap.computeIfAbsent(aux.getSubjectId(), k -> new ArrayList<>()).add(aux.getAuxiliaryType());
        }
        Map<String, ErpAccountingSubjectRespVO> codeMap = new LinkedHashMap<>();
        for (ErpAccountingSubjectDO d : list) {
            ErpAccountingSubjectRespVO vo = BeanUtils.toBean(d, ErpAccountingSubjectRespVO.class);
            vo.setAuxiliaryTypes(auxMap.getOrDefault(d.getId(), new ArrayList<>()));
            codeMap.put(d.getSubjectCode(), vo);
        }
        List<ErpAccountingSubjectRespVO> roots = new ArrayList<>();
        for (ErpAccountingSubjectRespVO vo : codeMap.values()) {
            String parentCode = vo.getParentCode();
            if (parentCode == null || parentCode.isEmpty() || !codeMap.containsKey(parentCode)) {
                roots.add(vo);
            } else {
                codeMap.get(parentCode).getChildren().add(vo);
            }
        }
        return roots;
    }

    /**
     * 拓扑排序：确保 parentCode 对应的行排在子行前面（S10 修复）
     */
    private <T> List<T> sortByParentDependency(List<T> rows,
                                                java.util.function.Function<T, String> codeGetter,
                                                java.util.function.Function<T, String> parentCodeGetter) {
        java.util.Map<String, T> codeMap = new java.util.HashMap<>();
        for (T r : rows) {
            String code = codeGetter.apply(r);
            if (code != null) {
                codeMap.put(code.trim(), r);
            }
        }
        List<T> sorted = new java.util.ArrayList<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        for (T r : rows) {
            visitTopo(r, codeMap, visited, sorted, codeGetter, parentCodeGetter);
        }
        return sorted;
    }

    private <T> void visitTopo(T row, java.util.Map<String, T> codeMap,
                               java.util.Set<String> visited, List<T> sorted,
                               java.util.function.Function<T, String> codeGetter,
                               java.util.function.Function<T, String> parentCodeGetter) {
        String code = codeGetter.apply(row);
        if (code == null || visited.contains(code.trim())) {
            return;
        }
        visited.add(code.trim());
        String parentCode = parentCodeGetter.apply(row);
        if (parentCode != null && codeMap.containsKey(parentCode.trim())) {
            visitTopo(codeMap.get(parentCode.trim()), codeMap, visited, sorted, codeGetter, parentCodeGetter);
        }
        sorted.add(row);
    }

}
