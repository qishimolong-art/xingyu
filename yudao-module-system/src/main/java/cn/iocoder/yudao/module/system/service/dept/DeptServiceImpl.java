package cn.iocoder.yudao.module.system.service.dept;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptBatchUpdateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptUpdateSortReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserDeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.logger.SystemOperateLogService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.ConstraintViolationException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.system.enums.LogRecordConstants.*;

/**
 * 部门 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class DeptServiceImpl implements DeptService {

    private static final int DEFAULT_SORT_STEP = 10;
    private static final Set<String> DEPT_ORDER_FIELDS = new HashSet<>(Arrays.asList(
            "id", "name", "leaderUserId", "sort", "status", "createTime"));

    @Resource
    private DeptMapper deptMapper;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private UserDeptMapper userDeptMapper;
    @Resource
    private DeptErpBizDataReferenceService deptErpBizDataReferenceService;
    @Resource
    private SystemOperateLogService operateLogService;

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public Long createDept(DeptSaveReqVO createReqVO) {
        if (createReqVO.getParentId() == null) {
            createReqVO.setParentId(DeptDO.PARENT_ID_ROOT);
        }
        // 校验父部门的有效性
        validateParentDept(null, createReqVO.getParentId());
        // 校验部门名的唯一性
        validateDeptNameUnique(null, createReqVO.getParentId(), createReqVO.getName());

        // 插入部门
        fillDefaultSort(createReqVO);
        DeptDO dept = BeanUtils.toBean(createReqVO, DeptDO.class);
        deptMapper.insert(dept);
        operateLogService.recordCreate(SYSTEM_DEPT_TYPE, SYSTEM_DEPT_CREATE_SUB_TYPE, dept.getId(), dept);
        return dept.getId();
    }

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public void updateDept(DeptSaveReqVO updateReqVO) {
        if (updateReqVO.getParentId() == null) {
            updateReqVO.setParentId(DeptDO.PARENT_ID_ROOT);
        }
        // 校验自己存在
        validateDeptExists(updateReqVO.getId());
        DeptDO oldDept = deptMapper.selectById(updateReqVO.getId());
        // 校验父部门的有效性
        validateParentDept(updateReqVO.getId(), updateReqVO.getParentId());
        // 校验部门名的唯一性
        validateDeptNameUnique(updateReqVO.getId(), updateReqVO.getParentId(), updateReqVO.getName());

        // 更新部门
        fillDefaultSortForUpdate(updateReqVO);
        DeptDO updateObj = BeanUtils.toBean(updateReqVO, DeptDO.class);
        if (updateReqVO.getLeaderUserId() == null) {
            LambdaUpdateWrapper<DeptDO> updateWrapper = new LambdaUpdateWrapper<DeptDO>()
                    .eq(DeptDO::getId, updateReqVO.getId())
                    .set(DeptDO::getName, updateObj.getName())
                    .set(DeptDO::getParentId, updateObj.getParentId())
                    .set(DeptDO::getSort, updateObj.getSort())
                    .set(DeptDO::getLeaderUserId, null)
                    .set(DeptDO::getStatus, updateObj.getStatus());
            deptMapper.update(null, updateWrapper);
            operateLogService.recordUpdate(SYSTEM_DEPT_TYPE, SYSTEM_DEPT_UPDATE_SUB_TYPE,
                    updateReqVO.getId(), oldDept, deptMapper.selectById(updateReqVO.getId()));
            return;
        }
        deptMapper.updateById(updateObj);
        operateLogService.recordUpdate(SYSTEM_DEPT_TYPE, SYSTEM_DEPT_UPDATE_SUB_TYPE,
                updateReqVO.getId(), oldDept, deptMapper.selectById(updateReqVO.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST, allEntries = true)
    public void updateDeptSort(DeptUpdateSortReqVO reqVO) {
        Long parentId = null;
        boolean parentIdInitialized = false;
        Map<Long, DeptDO> oldDeptMap = new LinkedHashMap<>();
        for (DeptUpdateSortReqVO.Item item : reqVO.getItems()) {
            DeptDO dept = deptMapper.selectById(item.getId());
            if (dept == null) {
                throw exception(DEPT_NOT_FOUND);
            }
            oldDeptMap.put(dept.getId(), dept);
            if (!parentIdInitialized) {
                parentId = dept.getParentId();
                parentIdInitialized = true;
            } else if (!Objects.equals(parentId, dept.getParentId())) {
                throw exception(DEPT_SORT_PARENT_NOT_SAME);
            }
        }
        for (DeptUpdateSortReqVO.Item item : reqVO.getItems()) {
            deptMapper.updateById(new DeptDO().setId(item.getId()).setSort(item.getSort()));
            operateLogService.recordUpdate(SYSTEM_DEPT_TYPE, SYSTEM_DEPT_UPDATE_SORT_SUB_TYPE,
                    item.getId(), oldDeptMap.get(item.getId()), deptMapper.selectById(item.getId()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST, allEntries = true)
    public void batchUpdateDept(DeptBatchUpdateReqVO reqVO) {
        boolean updateParentId = Boolean.TRUE.equals(reqVO.getUpdateParentId());
        boolean updateLeaderUserId = Boolean.TRUE.equals(reqVO.getUpdateLeaderUserId());
        boolean updateStatus = Boolean.TRUE.equals(reqVO.getUpdateStatus());
        if (!updateParentId && !updateLeaderUserId && !updateStatus) {
            throw exception(DEPT_BATCH_UPDATE_FIELD_EMPTY);
        }
        List<DeptDO> depts = deptMapper.selectByIds(reqVO.getIds());
        if (depts.size() != new HashSet<>(reqVO.getIds()).size()) {
            throw exception(DEPT_NOT_FOUND);
        }
        if (updateParentId) {
            Long parentId = Optional.ofNullable(reqVO.getParentId()).orElse(DeptDO.PARENT_ID_ROOT);
            validateBatchUpdateParent(depts, parentId);
            validateBatchUpdateNameUnique(depts, parentId);
            for (DeptDO dept : depts) {
                validateDeptNameUnique(dept.getId(), parentId, dept.getName());
            }
        }
        if (updateLeaderUserId && reqVO.getLeaderUserId() != null) {
            validateLeaderUser(reqVO.getLeaderUserId());
        }
        if (updateStatus && reqVO.getStatus() == null) {
            throw exception(DEPT_BATCH_UPDATE_FIELD_EMPTY);
        }
        for (DeptDO dept : depts) {
            DeptDO updateObj = new DeptDO().setId(dept.getId());
            if (updateParentId) {
                updateObj.setParentId(Optional.ofNullable(reqVO.getParentId()).orElse(DeptDO.PARENT_ID_ROOT));
            }
            if (updateLeaderUserId) {
                updateObj.setLeaderUserId(reqVO.getLeaderUserId());
            }
            if (updateStatus) {
                updateObj.setStatus(reqVO.getStatus());
            }
            if (updateLeaderUserId && reqVO.getLeaderUserId() == null) {
                LambdaUpdateWrapper<DeptDO> updateWrapper = new LambdaUpdateWrapper<DeptDO>()
                        .eq(DeptDO::getId, dept.getId())
                        .set(updateParentId, DeptDO::getParentId, updateObj.getParentId())
                        .set(DeptDO::getLeaderUserId, null)
                        .set(updateStatus, DeptDO::getStatus, updateObj.getStatus());
                deptMapper.update(null, updateWrapper);
            } else {
                deptMapper.updateById(updateObj);
            }
            operateLogService.recordUpdate(SYSTEM_DEPT_TYPE, SYSTEM_DEPT_BATCH_UPDATE_SUB_TYPE,
                    dept.getId(), dept, deptMapper.selectById(dept.getId()));
        }
    }

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public void deleteDept(Long id) {
        // 校验是否存在
        validateDeptCanDelete(Collections.singletonList(id));
        DeptDO dept = deptMapper.selectById(id);
        // 校验是否有子部门
        // 删除部门
        deptMapper.deleteById(id);
        operateLogService.recordDelete(SYSTEM_DEPT_TYPE, SYSTEM_DEPT_DELETE_SUB_TYPE, id, dept);
    }

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public void deleteDeptList(List<Long> ids) {
        // 校验是否有子部门
        validateDeptCanDelete(ids);
        List<DeptDO> depts = deptMapper.selectByIds(ids);

        // 批量删除部门
        deptMapper.deleteByIds(ids);
        depts.forEach(dept -> operateLogService.recordDelete(SYSTEM_DEPT_TYPE, SYSTEM_DEPT_BATCH_DELETE_SUB_TYPE,
                dept.getId(), dept));
    }

    private void validateDeptCanDelete(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw exception(DEPT_NOT_FOUND);
        }
        Set<Long> deptIds = new HashSet<>(ids);
        validateDeptExists(deptIds);
        validateDeptNoChildren(deptIds);
        validateDeptNoUserReference(deptIds);
        validateDeptNoErpBizData(deptIds);
    }

    private void validateDeptExists(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<DeptDO> depts = deptMapper.selectByIds(ids);
        if (depts.size() != ids.size()) {
            throw exception(DEPT_NOT_FOUND);
        }
    }

    private void validateDeptNoChildren(Collection<Long> ids) {
        for (Long id : ids) {
            if (deptMapper.selectCountByParentId(id) > 0) {
                throw exception(DEPT_EXITS_CHILDREN);
            }
        }
    }

    private void validateDeptNoUserReference(Collection<Long> ids) {
        if (CollUtil.isNotEmpty(userMapper.selectListByDeptIds(ids))
                || CollUtil.isNotEmpty(userDeptMapper.selectListByDeptIds(ids))) {
            throw exception(DEPT_EXISTS_USER);
        }
    }

    private void validateDeptNoErpBizData(Collection<Long> ids) {
        if (deptErpBizDataReferenceService.existsByDeptIds(ids)) {
            throw exception(DEPT_EXISTS_BIZ_DATA);
        }
    }

    @VisibleForTesting
    void validateDeptExists(Long id) {
        if (id == null) {
            return;
        }
        DeptDO dept = deptMapper.selectById(id);
        if (dept == null) {
            throw exception(DEPT_NOT_FOUND);
        }
    }

    @VisibleForTesting
    void validateParentDept(Long id, Long parentId) {
        if (parentId == null || DeptDO.PARENT_ID_ROOT.equals(parentId)) {
            return;
        }
        // 1. 不能设置自己为父部门
        if (Objects.equals(id, parentId)) {
            throw exception(DEPT_PARENT_ERROR);
        }
        // 2. 父部门不存在
        DeptDO parentDept = deptMapper.selectById(parentId);
        if (parentDept == null) {
            throw exception(DEPT_PARENT_NOT_EXITS);
        }
        // 3. 递归校验父部门，如果父部门是自己的子部门，则报错，避免形成环路
        if (id == null) { // id 为空，说明新增，不需要考虑环路
            return;
        }
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            // 3.1 校验环路
            parentId = parentDept.getParentId();
            if (Objects.equals(id, parentId)) {
                throw exception(DEPT_PARENT_IS_CHILD);
            }
            // 3.2 继续递归下一级父部门
            if (parentId == null || DeptDO.PARENT_ID_ROOT.equals(parentId)) {
                break;
            }
            parentDept = deptMapper.selectById(parentId);
            if (parentDept == null) {
                break;
            }
        }
    }

    @VisibleForTesting
    void validateDeptNameUnique(Long id, Long parentId, String name) {
        DeptDO dept = deptMapper.selectByParentIdAndName(parentId, name);
        if (dept == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的部门
        if (id == null) {
            throw exception(DEPT_NAME_DUPLICATE);
        }
        if (ObjectUtil.notEqual(dept.getId(), id)) {
            throw exception(DEPT_NAME_DUPLICATE);
        }
    }

    @Override
    public DeptDO getDept(Long id) {
        return deptMapper.selectById(id);
    }

    @Override
    public List<DeptDO> getDeptList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return deptMapper.selectByIds(ids);
    }

    @Override
    public List<DeptDO> getDeptList(DeptListReqVO reqVO) {
        Set<Long> leaderUserIds = resolveLeaderUserIds(reqVO.getLeaderUserName());
        if (leaderUserIds != null && leaderUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<DeptDO> list = deptMapper.selectList(reqVO, leaderUserIds);
        sortDeptList(list, reqVO);
        return list;
    }

    @Override
    public List<DeptDO> getDeptListByName(String name) {
        if (StrUtil.isBlank(name)) {
            return Collections.emptyList();
        }
        return deptMapper.selectListByName(name.trim());
    }

    private Set<Long> resolveLeaderUserIds(String leaderUserName) {
        if (StrUtil.isBlank(leaderUserName)) {
            return null;
        }
        List<AdminUserDO> users = userMapper.selectListByNickname(leaderUserName.trim());
        return users.stream().map(AdminUserDO::getId).collect(Collectors.toSet());
    }

    private void sortDeptList(List<DeptDO> list, DeptListReqVO reqVO) {
        list.sort((dept1, dept2) -> {
            int parentCompare = compareNullableLong(dept1.getParentId(), dept2.getParentId());
            if (parentCompare != 0) {
                return parentCompare;
            }
            int orderCompare = compareByRequestOrder(dept1, dept2, reqVO);
            if (orderCompare != 0) {
                return orderCompare;
            }
            return compareByDefaultOrder(dept1, dept2);
        });
    }

    private int compareByRequestOrder(DeptDO dept1, DeptDO dept2, DeptListReqVO reqVO) {
        if (!isValidOrder(reqVO)) {
            return 0;
        }
        int compare;
        switch (reqVO.getOrderField().trim()) {
            case "id":
                compare = compareNullableLong(dept1.getId(), dept2.getId());
                break;
            case "name":
                compare = compareNullableString(dept1.getName(), dept2.getName());
                break;
            case "leaderUserId":
                compare = compareNullableLong(dept1.getLeaderUserId(), dept2.getLeaderUserId());
                break;
            case "sort":
                compare = compareNullableIntegerNullLast(dept1.getSort(), dept2.getSort());
                break;
            case "status":
                compare = compareNullableInteger(dept1.getStatus(), dept2.getStatus());
                break;
            case "createTime":
                compare = compareNullableComparable(dept1.getCreateTime(), dept2.getCreateTime());
                break;
            default:
                compare = 0;
        }
        return "desc".equalsIgnoreCase(reqVO.getOrderDirection()) ? -compare : compare;
    }

    private boolean isValidOrder(DeptListReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getOrderField())) {
            return false;
        }
        return DEPT_ORDER_FIELDS.contains(reqVO.getOrderField().trim())
                && ("asc".equalsIgnoreCase(reqVO.getOrderDirection())
                || "desc".equalsIgnoreCase(reqVO.getOrderDirection()));
    }

    private int compareByDefaultOrder(DeptDO dept1, DeptDO dept2) {
        int sortCompare = Integer.compare(Optional.ofNullable(dept1.getSort()).orElse(Integer.MAX_VALUE),
                Optional.ofNullable(dept2.getSort()).orElse(Integer.MAX_VALUE));
        if (sortCompare != 0) {
            return sortCompare;
        }
        int nameCompare = compareNullableString(dept1.getName(), dept2.getName());
        if (nameCompare != 0) {
            return nameCompare;
        }
        return compareNullableLong(dept1.getId(), dept2.getId());
    }

    private int compareNullableLong(Long value1, Long value2) {
        if (Objects.equals(value1, value2)) {
            return 0;
        }
        if (value1 == null) {
            return -1;
        }
        if (value2 == null) {
            return 1;
        }
        return Long.compare(value1, value2);
    }

    private int compareNullableInteger(Integer value1, Integer value2) {
        if (Objects.equals(value1, value2)) {
            return 0;
        }
        if (value1 == null) {
            return -1;
        }
        if (value2 == null) {
            return 1;
        }
        return Integer.compare(value1, value2);
    }

    private int compareNullableIntegerNullLast(Integer value1, Integer value2) {
        if (Objects.equals(value1, value2)) {
            return 0;
        }
        if (value1 == null) {
            return 1;
        }
        if (value2 == null) {
            return -1;
        }
        return Integer.compare(value1, value2);
    }

    private <T extends Comparable<T>> int compareNullableComparable(T value1, T value2) {
        if (Objects.equals(value1, value2)) {
            return 0;
        }
        if (value1 == null) {
            return -1;
        }
        if (value2 == null) {
            return 1;
        }
        return value1.compareTo(value2);
    }

    private int compareNullableString(String value1, String value2) {
        if (Objects.equals(value1, value2)) {
            return 0;
        }
        if (value1 == null) {
            return -1;
        }
        if (value2 == null) {
            return 1;
        }
        return value1.trim().compareToIgnoreCase(value2.trim());
    }

    private void fillDefaultSort(DeptSaveReqVO createReqVO) {
        if (createReqVO.getSort() != null) {
            return;
        }
        DeptDO lastDept = deptMapper.selectFirstByParentIdOrderBySortDesc(createReqVO.getParentId());
        Integer maxSort = lastDept != null ? lastDept.getSort() : null;
        createReqVO.setSort(Optional.ofNullable(maxSort).orElse(0) + DEFAULT_SORT_STEP);
    }

    private void fillDefaultSortForUpdate(DeptSaveReqVO updateReqVO) {
        if (updateReqVO.getSort() != null) {
            return;
        }
        DeptDO lastDept = deptMapper.selectFirstByParentIdOrderBySortDescExcludeId(
                updateReqVO.getParentId(), updateReqVO.getId());
        Integer maxSort = lastDept != null ? lastDept.getSort() : null;
        updateReqVO.setSort(Optional.ofNullable(maxSort).orElse(0) + DEFAULT_SORT_STEP);
    }

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST, allEntries = true)
    public DeptImportRespVO importDeptList(List<DeptImportExcelVO> importDepts, boolean isUpdateSupport) {
        if (CollUtil.isEmpty(importDepts)) {
            throw exception(DEPT_IMPORT_LIST_IS_EMPTY);
        }
        DeptImportRespVO respVO = DeptImportRespVO.builder().createNames(new ArrayList<>())
                .updateNames(new ArrayList<>()).failureNames(new LinkedHashMap<>()).build();
        AtomicInteger index = new AtomicInteger(1);
        importDepts.forEach(importDept -> {
            int currentIndex = index.getAndIncrement();
            String failureKey = StrUtil.blankToDefault(importDept.getName(), "第 " + currentIndex + " 行");
            try {
                importOneDept(importDept, isUpdateSupport, respVO);
            } catch (ConstraintViolationException | ServiceException | IllegalArgumentException ex) {
                respVO.getFailureNames().put(failureKey, ex.getMessage());
            }
        });
        return respVO;
    }

    private void importOneDept(DeptImportExcelVO importDept, boolean isUpdateSupport, DeptImportRespVO respVO) {
        Integer status = parseImportStatus(importDept.getStatus());
        Long parentId = resolveImportParentId(importDept.getParentName());
        Long leaderUserId = resolveImportLeaderUserId(importDept.getLeaderUserName());
        DeptSaveReqVO reqVO = new DeptSaveReqVO()
                .setName(importDept.getName())
                .setParentId(parentId)
                .setSort(importDept.getSort())
                .setLeaderUserId(leaderUserId)
                .setStatus(status);
        ValidationUtils.validate(reqVO);

        DeptDO existDept = deptMapper.selectByParentIdAndName(parentId, reqVO.getName());
        if (existDept == null) {
            validateParentDept(null, parentId);
            validateDeptNameUnique(null, parentId, reqVO.getName());
            fillDefaultSort(reqVO);
            DeptDO dept = BeanUtils.toBean(reqVO, DeptDO.class);
            deptMapper.insert(dept);
            respVO.getCreateNames().add(reqVO.getName());
            operateLogService.recordCreate(SYSTEM_DEPT_TYPE, SYSTEM_DEPT_IMPORT_SUB_TYPE, dept.getId(), dept);
            return;
        }
        if (!isUpdateSupport) {
            throw exception(DEPT_NAME_DUPLICATE);
        }
        validateParentDept(existDept.getId(), parentId);
        fillDefaultSort(reqVO);
        LambdaUpdateWrapper<DeptDO> updateWrapper = new LambdaUpdateWrapper<DeptDO>()
                .eq(DeptDO::getId, existDept.getId())
                .set(DeptDO::getParentId, reqVO.getParentId())
                .set(DeptDO::getSort, reqVO.getSort())
                .set(DeptDO::getLeaderUserId, reqVO.getLeaderUserId())
                .set(DeptDO::getStatus, reqVO.getStatus());
        deptMapper.update(new DeptDO().setName(reqVO.getName()), updateWrapper);
        respVO.getUpdateNames().add(reqVO.getName());
        operateLogService.recordUpdate(SYSTEM_DEPT_TYPE, SYSTEM_DEPT_IMPORT_SUB_TYPE,
                existDept.getId(), existDept, deptMapper.selectById(existDept.getId()));
    }

    private Integer parseImportStatus(String statusText) {
        if (StrUtil.isBlank(statusText)) {
            throw new IllegalArgumentException("状态不能为空");
        }
        String status = statusText.trim();
        if ("启用".equals(status)) {
            return CommonStatusEnum.ENABLE.getStatus();
        }
        if ("禁用".equals(status)) {
            return CommonStatusEnum.DISABLE.getStatus();
        }
        throw new IllegalArgumentException("状态值只能填写“启用”或“禁用”");
    }

    private Long resolveImportParentId(String parentName) {
        if (StrUtil.isBlank(parentName)) {
            return DeptDO.PARENT_ID_ROOT;
        }
        List<DeptDO> parents = deptMapper.selectListByName(parentName.trim());
        if (CollUtil.isEmpty(parents)) {
            throw new IllegalArgumentException("上级部门「" + parentName.trim() + "」不存在");
        }
        if (parents.size() > 1) {
            throw new IllegalArgumentException("上级部门名称存在重复，请先调整重名部门");
        }
        return parents.get(0).getId();
    }

    private Long resolveImportLeaderUserId(String leaderUserName) {
        if (StrUtil.isBlank(leaderUserName)) {
            return null;
        }
        List<AdminUserDO> users = userMapper.selectListByNicknameEq(leaderUserName.trim());
        if (CollUtil.isEmpty(users)) {
            throw new IllegalArgumentException("负责人「" + leaderUserName.trim() + "」不存在");
        }
        if (users.size() > 1) {
            throw new IllegalArgumentException("负责人名称存在重复，请先保证负责人名称唯一");
        }
        AdminUserDO user = users.get(0);
        if (!CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
            throw exception(USER_IS_DISABLE, user.getNickname());
        }
        return user.getId();
    }

    private void validateLeaderUser(Long leaderUserId) {
        AdminUserDO user = userMapper.selectById(leaderUserId);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
            throw exception(USER_IS_DISABLE, user.getNickname());
        }
    }

    private void validateBatchUpdateParent(List<DeptDO> depts, Long parentId) {
        if (DeptDO.PARENT_ID_ROOT.equals(parentId)) {
            return;
        }
        validateParentDept(null, parentId);
        Set<Long> selectedIds = depts.stream().map(DeptDO::getId).collect(Collectors.toSet());
        if (selectedIds.contains(parentId)) {
            throw exception(DEPT_BATCH_UPDATE_PARENT_IS_SELECTED_CHILD);
        }
        Set<Long> childIds = convertSet(getChildDeptList(selectedIds), DeptDO::getId);
        if (childIds.contains(parentId)) {
            throw exception(DEPT_BATCH_UPDATE_PARENT_IS_SELECTED_CHILD);
        }
    }

    private void validateBatchUpdateNameUnique(List<DeptDO> depts, Long parentId) {
        Set<String> names = new HashSet<>();
        for (DeptDO dept : depts) {
            if (Objects.equals(dept.getParentId(), parentId)) {
                continue;
            }
            if (!names.add(dept.getName())) {
                throw exception(DEPT_NAME_DUPLICATE);
            }
        }
    }

    @Override
    public List<DeptDO> getChildDeptList(Collection<Long> ids) {
        List<DeptDO> children = new LinkedList<>();
        // 遍历每一层
        Collection<Long> parentIds = ids;
        for (int i = 0; i < Short.MAX_VALUE; i++) { // 使用 Short.MAX_VALUE 避免 bug 场景下，存在死循环
            // 查询当前层，所有的子部门
            List<DeptDO> depts = deptMapper.selectListByParentId(parentIds);
            // 1. 如果没有子部门，则结束遍历
            if (CollUtil.isEmpty(depts)) {
                break;
            }
            // 2. 如果有子部门，继续遍历
            children.addAll(depts);
            parentIds = convertSet(depts, DeptDO::getId);
        }
        return children;
    }

    @Override
    public List<DeptDO> getDeptListByLeaderUserId(Long id) {
        return deptMapper.selectListByLeaderUserId(id);
    }

    @Override
    @DataPermission(enable = false) // 禁用数据权限，避免建立不正确的缓存
    @Cacheable(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST, key = "#id")
    public Set<Long> getChildDeptIdListFromCache(Long id) {
        List<DeptDO> children = getChildDeptList(id);
        return convertSet(children, DeptDO::getId);
    }

    @Override
    public void validateDeptList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 获得科室信息
        Map<Long, DeptDO> deptMap = getDeptMap(ids);
        // 校验
        ids.forEach(id -> {
            DeptDO dept = deptMap.get(id);
            if (dept == null) {
                throw exception(DEPT_NOT_FOUND);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus())) {
                throw exception(DEPT_NOT_ENABLE, dept.getName());
            }
        });
    }

}
