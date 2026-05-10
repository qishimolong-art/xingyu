<script lang="ts" setup>
import type { InableItem } from '#/api/erp/purchase/order';

import { ref } from 'vue';

import { message } from 'ant-design-vue';

import {
  erpCountInputFormatter,
  erpPriceInputFormatter,
} from '@vben/utils';

import {
  Checkbox,
  InputNumber,
  Modal,
  Select,
  Table,
} from 'ant-design-vue';

import { getInableItemsByOrderId } from '#/api/erp/purchase/order';
import { createPurchaseInFromOrder } from '#/api/erp/purchase/in';
import { getWarehouseSimpleList } from '#/api/erp/stock/warehouse';

const emit = defineEmits(['success']);

const visible = ref(false);
const loading = ref(false);
const orderId = ref<number>();
const items = ref<(InableItem & { checked?: boolean; inQty?: number })[]>([]);
const warehouseOptions = ref<any[]>([]);

/** 打开弹窗 */
async function open(id: number) {
  orderId.value = id;
  visible.value = true;
  loading.value = true;
  try {
    warehouseOptions.value = await getWarehouseSimpleList();
    const data = await getInableItemsByOrderId(id);
    items.value = (data || []).map((item) => ({
      ...item,
      checked: true,
      inQty: item.inableCount,
    }));
  } finally {
    loading.value = false;
  }
}

/** 确认入库 */
async function handleConfirm() {
  const selectedItems = items.value.filter((item) => item.checked);
  if (selectedItems.length === 0) {
    message.warning('请至少勾选一项');
    return;
  }
  // 校验
  for (const item of selectedItems) {
    if (!item.inQty || item.inQty <= 0) {
      message.warning(`商品[${item.productName}]入库数量必须大于0`);
      return;
    }
    if (item.inQty > (item.inableCount || 0)) {
      message.warning(
        `商品[${item.productName}]入库数量(${item.inQty})超过可入库数量(${item.inableCount})`,
      );
      return;
    }
    if (!item.warehouseId) {
      message.warning(`商品[${item.productName}]请选择仓库`);
      return;
    }
  }
  loading.value = true;
  try {
    await createPurchaseInFromOrder({
      orderId: orderId.value!,
      items: selectedItems.map((item) => ({
        orderItemId: item.orderItemId!,
        count: item.inQty!,
        warehouseId: item.warehouseId!,
      })),
    });
    message.success('入库成功');
    visible.value = false;
    emit('success');
  } finally {
    loading.value = false;
  }
}

const columns = [
  {
    title: '',
    dataIndex: 'checked',
    width: 50,
    key: 'checked',
  },
  {
    title: '产品编码',
    dataIndex: 'productCode',
    width: 120,
  },
  {
    title: '产品名称',
    dataIndex: 'productName',
    width: 160,
  },
  {
    title: '单位',
    dataIndex: 'productUnitName',
    width: 60,
  },
  {
    title: '单价',
    dataIndex: 'productPrice',
    width: 100,
    key: 'productPrice',
  },
  {
    title: '订单数量',
    dataIndex: 'orderCount',
    width: 100,
    key: 'orderCount',
  },
  {
    title: '已入库',
    dataIndex: 'inCount',
    width: 90,
    key: 'inCount',
  },
  {
    title: '可入库',
    dataIndex: 'inableCount',
    width: 90,
    key: 'inableCount',
  },
  {
    title: '本次入库',
    dataIndex: 'inQty',
    width: 120,
    key: 'inQty',
  },
  {
    title: '仓库',
    dataIndex: 'warehouseId',
    width: 160,
    key: 'warehouseId',
  },
  {
    title: '赠品',
    dataIndex: 'gift',
    width: 60,
    key: 'gift',
  },
];

defineExpose({ open });
</script>

<template>
  <Modal
    v-model:open="visible"
    class="!w-[85vw]"
    title="分批入库"
    :confirm-loading="loading"
    @ok="handleConfirm"
  >
    <Table
      :columns="columns"
      :data-source="items"
      :loading="loading"
      :pagination="false"
      :scroll="{ x: 'max-content', y: 'calc(70vh - 120px)' }"
      row-key="orderItemId"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'checked'">
          <Checkbox v-model:checked="record.checked" />
        </template>
        <template v-else-if="column.key === 'productPrice'">
          {{ erpPriceInputFormatter(record.productPrice) }}
        </template>
        <template v-else-if="column.key === 'orderCount'">
          {{ erpCountInputFormatter(record.orderCount) }}
        </template>
        <template v-else-if="column.key === 'inCount'">
          {{ erpCountInputFormatter(record.inCount) }}
        </template>
        <template v-else-if="column.key === 'inableCount'">
          {{ erpCountInputFormatter(record.inableCount) }}
        </template>
        <template v-else-if="column.key === 'inQty'">
          <InputNumber
            v-model:value="record.inQty"
            :min="0"
            :max="record.inableCount"
            :precision="3"
            size="small"
            class="w-full"
          />
        </template>
        <template v-else-if="column.key === 'warehouseId'">
          <Select
            v-model:value="record.warehouseId"
            :options="warehouseOptions"
            :field-names="{ label: 'name', value: 'id' }"
            placeholder="选择仓库"
            size="small"
            show-search
            allow-clear
            class="w-full"
          />
        </template>
        <template v-else-if="column.key === 'gift'">
          <span v-if="record.gift" class="text-orange-500">是</span>
          <span v-else>否</span>
        </template>
      </template>
    </Table>
  </Modal>
</template>
