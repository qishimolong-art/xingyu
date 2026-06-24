-- 部门重复记录清理脚本
-- 场景：软删除部门后重新导入，导致 (parent_id, name) 组合存在多条 deleted=0 的有效记录
-- 清理策略：保留 id 最大的那条（最新插入的），将其余重复记录软删除（deleted=1）
-- 幂等：可重复执行，不影响已清理数据

-- 第一步：查看重复记录（诊断用，不修改数据）
SELECT parent_id, name, COUNT(*) AS cnt
FROM system_dept
WHERE deleted = 0
GROUP BY parent_id, name
HAVING COUNT(*) > 1;

-- 第二步：清理重复记录，保留 id 最大的那条
UPDATE system_dept d
    INNER JOIN (
        SELECT parent_id, name, MAX(id) AS max_id
        FROM system_dept
        WHERE deleted = 0
        GROUP BY parent_id, name
        HAVING COUNT(*) > 1
    ) dup ON d.parent_id = dup.parent_id AND d.name = dup.name
SET d.deleted = 1,
    d.updater  = 'system_cleanup',
    d.update_time = NOW()
WHERE d.deleted = 0
  AND d.id != dup.max_id;

-- 第三步：验证清理结果（执行后应返回空结果集）
SELECT parent_id, name, COUNT(*) AS cnt
FROM system_dept
WHERE deleted = 0
GROUP BY parent_id, name
HAVING COUNT(*) > 1;
