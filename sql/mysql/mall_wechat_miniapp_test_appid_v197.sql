-- 小程序测试号配置同步：修复微信手机号授权 invalid appid(40013)
-- 执行后需重启后端，确保 WxMaService 使用新的 AppID/AppSecret。

UPDATE `system_social_client`
SET `client_id` = 'wx6b354a2cceda1b8c',
    `client_secret` = 'a8523d0d6bdca0ce64b248c8bf7facc4',
    `status` = 0,
    `update_time` = NOW()
WHERE `social_type` = 34
  AND `user_type` = 1
  AND `tenant_id` = 1
  AND `deleted` = b'0';

UPDATE `system_tenant`
SET `websites` = REPLACE(`websites`, 'wxc4598c446f8a9cb3', 'wx6b354a2cceda1b8c'),
    `update_time` = NOW()
WHERE `id` = 1
  AND `deleted` = b'0'
  AND `websites` LIKE '%wxc4598c446f8a9cb3%';
