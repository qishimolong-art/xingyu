-- Hide post management from System Management.
-- Keep system_post and system_user_post data for historical compatibility.

UPDATE `system_menu`
SET `visible` = b'0',
    `status` = 1,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` IN (104, 1021, 1022, 1023, 1024, 1025)
  AND `deleted` = b'0';

UPDATE `system_field_definition`
SET `deleted` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `module` = 'system_users'
  AND `field_key` = 'postIds'
  AND `deleted` = b'0';
