<script lang="ts" setup>
import type { ErpPurchaseInApi } from '#/api/erp/purchase/in';

import { ref, watch } from 'vue';

import { Input, Modal, Table } from 'ant-design-vue';

import { getPurchaseInPage } from '#/api/erp/purchase/in';

const props = defineProps<{ open: boolean }>();
const emit = defineEmits(['update:open', 'select']);

const keyword = ref('');
const loading = ref(false);
const dataSource = ref<ErpPurchaseInApi.PurchaseIn[]>([]);

const columns = [
  { title: '入库单号', dataIndex: 'no', width: 180 },
  { title: '供应商', dataIndex: 'supplierName', width: 150 },
  { title: '入库时间', dataIndex: 'inTime', width: 160 },
  { title: '合计数量', dataIndex: 'totalCount', width: 100 },
  { title: '合计金额', dataIndex: 'totalPrice', width: 120 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' },
];

async function loadData() {
  loading.value = true;
  try {
    // status=20 表示已审批；后端 ErpAuditStatus.APPROVE 为 20
    const page = await getPurchaseInPage({
      pageNo: 1,
      pageSize: 50,
      no: keyword.value,
      status: 20,
    } as any);
    dataSource.value = page.list;
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      keyword.value = '';
      loadData();
    }
  },
);

function handleSelect(row: ErpPurchaseInApi.PurchaseIn) {
  emit('select', row);
  emit('update:open', false);
}
</script>

<template>
  <Modal
    :open="open"
    title="选择原采购入库单"
    class="!w-[85vw]"
    :footer="null"
    @update:open="(v) => emit('update:open', v)"
  >
    <div class="mb-3 flex gap-2">
      <Input
        v-model:value="keyword"
        placeholder="输入入库单号搜索"
        allow-clear
        class="max-w-xs"
        @press-enter="loadData"
      />
      <a-button type="primary" :loading="loading" @click="loadData">
        搜索
      </a-button>
    </div>
    <Table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="middle"
      :scroll="{ x: 'max-content', y: 'calc(70vh - 180px)' }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a @click="handleSelect(record)">选择</a>
        </template>
      </template>
    </Table>
  </Modal>
</template>
