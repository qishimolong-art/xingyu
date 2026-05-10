<script lang="ts" setup>
import type { ErpProductApi } from '#/api/erp/product/product';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, Table } from 'ant-design-vue';

const rows = defineModel<ErpProductApi.ProductUniversal[]>('modelValue', {
  default: () => [],
});

const columns = [
  { title: '通用件编码', dataIndex: 'universalCode', width: 180 },
  { title: '通用件名称', dataIndex: 'universalName', width: 220 },
  { title: '适用车型', dataIndex: 'universalVehicle' },
  { title: '操作', dataIndex: 'op', width: 80, align: 'center' as const },
];

function handleAdd() {
  const list = rows.value ? [...rows.value] : [];
  list.push({ universalCode: '', universalName: '', universalVehicle: '' });
  rows.value = list;
}

function handleRemove(index: number) {
  const list = rows.value ? [...rows.value] : [];
  list.splice(index, 1);
  rows.value = list;
}

function updateField(
  index: number,
  field: keyof ErpProductApi.ProductUniversal,
  val: any,
) {
  const list = rows.value ? [...rows.value] : [];
  const row = { ...(list[index] ?? {}) } as ErpProductApi.ProductUniversal;
  (row as any)[field] = val;
  list[index] = row;
  rows.value = list;
}
</script>

<template>
  <div class="mx-2 py-2">
    <div class="mb-3 flex items-center justify-between">
      <div class="text-sm text-gray-500">
        通用件用于"当前配件缺货时可替代销售"的场景；一条记录对应一个可替代车型。
      </div>
      <Button type="primary" @click="handleAdd">
        <template #icon><IconifyIcon icon="lucide:plus" /></template>
        新增
      </Button>
    </div>
    <Table
      :columns="columns"
      :data-source="rows"
      :pagination="false"
      row-key="universalCode"
      size="middle"
      bordered
    >
      <template #bodyCell="{ column, index, record }">
        <template v-if="column.dataIndex === 'universalCode'">
          <Input
            :value="record.universalCode"
            placeholder="请输入编码，如 P000002"
            @update:value="(v) => updateField(index, 'universalCode', v)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'universalName'">
          <Input
            :value="record.universalName"
            placeholder="请输入名称"
            @update:value="(v) => updateField(index, 'universalName', v)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'universalVehicle'">
          <Input
            :value="record.universalVehicle"
            placeholder="请输入适用车型"
            @update:value="(v) => updateField(index, 'universalVehicle', v)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'op'">
          <Button type="link" danger @click="handleRemove(index)">删除</Button>
        </template>
      </template>
    </Table>
  </div>
</template>
