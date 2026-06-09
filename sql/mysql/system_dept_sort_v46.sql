-- Department display order initialization.
-- MySQL 5.7 / 8.0 compatible.
-- Execute after backing up the database.
--
-- The department list API sorts by parent_id, sort, name, id.
-- This script sets branch-company sort values by pinyin order within the same
-- parent department, with a gap of 10 between adjacent departments.
-- Example: 阿坝分公司 = 10, 巴中分公司 = 20 when they share the same parent.

UPDATE `system_dept` d
JOIN (
    SELECT
        base.`id`,
        COUNT(other.`id`) * 10 AS `new_sort`
    FROM (
        SELECT `id`, `parent_id`, `name`
        FROM `system_dept`
        WHERE `deleted` = b'0'
          AND `name` LIKE '%分公司'
    ) base
    JOIN (
        SELECT `id`, `parent_id`, `name`
        FROM `system_dept`
        WHERE `deleted` = b'0'
          AND `name` LIKE '%分公司'
    ) other ON other.`parent_id` <=> base.`parent_id`
        AND (
            CAST(CONVERT(other.`name` USING gbk) AS BINARY) < CAST(CONVERT(base.`name` USING gbk) AS BINARY)
            OR (
                CAST(CONVERT(other.`name` USING gbk) AS BINARY) = CAST(CONVERT(base.`name` USING gbk) AS BINARY)
                AND other.`id` <= base.`id`
            )
        )
    GROUP BY base.`id`
) s ON d.`id` = s.`id`
SET d.`sort` = s.`new_sort`,
    d.`updater` = '1',
    d.`update_time` = NOW()
WHERE d.`deleted` = b'0';

SELECT `id`, `name`, `parent_id`, `sort`, `tenant_id`
FROM `system_dept`
WHERE `deleted` = b'0'
  AND `name` LIKE '%分公司'
ORDER BY `parent_id`, `sort`, `name`, `id`;
