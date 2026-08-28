-- 商城商品分类图片补齐（v191）
-- 安全说明：
--   - 仅更新 product_category.pic_url。
--   - 通过 id + name + deleted = b'0' 精确匹配，不影响历史已删除分类。
--   - 图片资源位于前端静态目录 /static/img/category/。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_mall_product_category_images_v191;

CREATE TEMPORARY TABLE tmp_mall_product_category_images_v191 (
  id bigint NOT NULL PRIMARY KEY,
  name varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  pic_url varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_mall_product_category_images_v191 (id, name, pic_url)
VALUES
(84, '车用润滑油', '/static/img/category/auto-lubricant.jpg'),
(91, '汽机油', '/static/img/category/auto-lubricant.jpg'),
(92, '柴机油', '/static/img/category/auto-lubricant.jpg'),
(93, '摩机油', '/static/img/category/auto-lubricant.jpg'),

(85, '工业润滑油', '/static/img/category/industrial-lubricant.webp'),
(94, '液压油', '/static/img/category/industrial-lubricant.webp'),
(95, '齿轮油', '/static/img/category/industrial-lubricant.webp'),
(96, '透平油', '/static/img/category/industrial-lubricant.webp'),
(97, '压缩机油', '/static/img/category/industrial-lubricant.webp'),
(98, '导热油', '/static/img/category/industrial-lubricant.webp'),
(99, '导轨油', '/static/img/category/industrial-lubricant.webp'),
(100, '润滑脂', '/static/img/category/industrial-lubricant.webp'),
(101, '金属加工液', '/static/img/category/industrial-lubricant.webp'),
(102, '传动油', '/static/img/category/industrial-lubricant.webp'),
(103, '主轴油', '/static/img/category/industrial-lubricant.webp'),
(104, '油膜轴承油', '/static/img/category/industrial-lubricant.webp'),
(105, '食品级润滑油', '/static/img/category/industrial-lubricant.webp'),
(106, '涡轮蜗杆油', '/static/img/category/industrial-lubricant.webp'),
(107, '燃气轮机油', '/static/img/category/industrial-lubricant.webp'),
(108, '气缸油', '/static/img/category/industrial-lubricant.webp'),
(109, '其他工业油', '/static/img/category/industrial-lubricant.webp'),

(86, '车用辅油', '/static/img/category/auto-fluids.png'),
(110, '变速箱油', '/static/img/category/auto-fluids.png'),
(111, '齿轮油', '/static/img/category/auto-fluids.png'),
(112, '制动液', '/static/img/category/auto-fluids.png'),
(113, '防冻液', '/static/img/category/auto-fluids.png'),
(114, '尿素溶液', '/static/img/category/auto-fluids.png'),
(115, '车用润滑脂', '/static/img/category/auto-fluids.png'),

(87, '养护品', '/static/img/category/car-care.png'),
(116, '发动机系统养护', '/static/img/category/car-care.png'),
(117, '燃油系统养护', '/static/img/category/car-care.png'),
(118, '空调系统养护', '/static/img/category/car-care.png'),
(119, '刹车系统养护', '/static/img/category/car-care.png'),
(120, '冷却系统养护', '/static/img/category/car-care.png'),
(121, '车身与内外饰养护', '/static/img/category/car-care.png'),
(122, '机修辅料', '/static/img/category/car-care.png'),

(88, '轮胎', '/static/img/category/tires.png'),
(123, '乘用车轮胎', '/static/img/category/tires.png'),
(124, '商用车轮胎', '/static/img/category/tires.png'),
(125, '工程机械轮胎', '/static/img/category/tires.png'),
(126, '摩电轮胎', '/static/img/category/tires.png'),

(89, '易损件', '/static/img/category/wearing-parts.webp'),
(127, '滤清器', '/static/img/category/wearing-parts.webp'),
(128, '汽车电池', '/static/img/category/wearing-parts.webp'),
(129, '制动系统', '/static/img/category/wearing-parts.webp'),
(130, '悬挂系统', '/static/img/category/wearing-parts.webp'),
(131, '雨刷与照明', '/static/img/category/wearing-parts.webp'),
(132, '点火系统', '/static/img/category/wearing-parts.webp'),
(133, '传动皮带与张紧器', '/static/img/category/wearing-parts.webp'),
(134, '密封件与垫片', '/static/img/category/wearing-parts.webp'),
(135, '摩电配件', '/static/img/category/wearing-parts.webp'),
(136, '其他易损件', '/static/img/category/wearing-parts.webp'),

(90, '设备、工具及其他', '/static/img/category/repair-equipment.png'),
(137, '润滑油加注设备', '/static/img/category/repair-equipment.png'),
(138, '维修工具与诊断仪', '/static/img/category/repair-tools.png'),
(139, '仓储与物流耗材', '/static/img/category/repair-equipment.png'),
(140, '促销品', '/static/img/category/repair-equipment.png'),
(141, '广宣物料', '/static/img/category/repair-equipment.png');

UPDATE product_category category
JOIN tmp_mall_product_category_images_v191 image_map
  ON image_map.id = category.id
 AND image_map.name COLLATE utf8mb4_unicode_ci = category.name COLLATE utf8mb4_unicode_ci
SET category.pic_url = image_map.pic_url,
    category.updater = '1',
    category.update_time = NOW()
WHERE category.deleted = b'0'
  AND category.pic_url <> image_map.pic_url;

SELECT COUNT(*) AS expected_active_category_count
FROM tmp_mall_product_category_images_v191;

SELECT COUNT(*) AS updated_active_category_count
FROM product_category category
JOIN tmp_mall_product_category_images_v191 image_map
  ON image_map.id = category.id
 AND image_map.name COLLATE utf8mb4_unicode_ci = category.name COLLATE utf8mb4_unicode_ci
WHERE category.deleted = b'0'
  AND category.pic_url = image_map.pic_url;

SELECT image_map.id,
       image_map.name,
       image_map.pic_url,
       category.deleted
FROM tmp_mall_product_category_images_v191 image_map
LEFT JOIN product_category category
  ON category.id = image_map.id
 AND category.name COLLATE utf8mb4_unicode_ci = image_map.name COLLATE utf8mb4_unicode_ci
WHERE category.id IS NULL
   OR category.deleted <> b'0'
   OR category.pic_url <> image_map.pic_url
ORDER BY image_map.id;

DROP TEMPORARY TABLE IF EXISTS tmp_mall_product_category_images_v191;
