<script lang="ts" setup>
import type { ErpProductApi } from '#/api/erp/product/product';
import type { ErpSaleCartApi } from '#/api/erp/sale/cart';

import { computed, nextTick, ref, watch } from 'vue';

import { erpCountInputFormatter, erpPriceInputFormatter, erpPriceMultiply } from '@vben/utils';

import { Input, InputNumber } from 'ant-design-vue';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getStockCount } from '#/api/erp/stock/stock';
import ProductSelectModal from '#/views/erp/product/product/modules/product-select-modal.vue';

import { useFormItemColumns } from '../data';

interface Props {
  items?: ErpSaleCartApi.SaleCartItem[];
  disabled?: boolean;
  discountPercent?: number;
}

const props = withDefaults(defineProps<Props>(), { items: () => [], disabled: false, discountPercent: 0 });
const emit = defineEmits(['update:items', 'update:discount-price', 'update:total-price']);
const tableData = ref<ErpSaleCartApi.SaleCartItem[]>([]);
const productSelectRef = ref<InstanceType<typeof ProductSelectModal>>();
let isInternalUpdate = false;

const summaries = computed(() => ({
  count: tableData.value.reduce((sum, item) => sum + (item.count || 0), 0),
  totalPrice: tableData.value.reduce((sum, item) => sum + (item.totalPrice || 0), 0),
  taxPrice: tableData.value.reduce((sum, item) => sum + (item.taxPrice || 0), 0),
}));

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useFormItemColumns(props.disabled),
    data: tableData.value,
    minHeight: 280,
    autoResize: true,
    border: true,
    rowConfig: { keyField: 'seq', isHover: true },
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
  },
});

watch(
  () => props.items,
  async (items) => {
    if (!items) return;
    if (isInternalUpdate) {
      isInternalUpdate = false;
      return;
    }
    items.forEach(initRow);
    tableData.value = [...items];
    await nextTick();
    await gridApi.grid.reloadData(tableData.value);
  },
  { immediate: true },
);

watch(
  () => [tableData.value, props.discountPercent],
  () => {
    const totalPrice = summaries.value.totalPrice;
    const discountPrice = erpPriceMultiply(totalPrice, (props.discountPercent || 0) / 100) || 0;
    emit('update:discount-price', discountPrice);
    emit('update:total-price', totalPrice - discountPrice);
  },
  { deep: true },
);

function emitItemsUpdate(skipReload = false) {
  isInternalUpdate = skipReload;
  emit('update:items', [...tableData.value]);
}

function handleOpenSelect() {
  productSelectRef.value?.open();
}

async function handleProductsSelected(products: ErpProductApi.Product[]) {
  for (const p of products) {
    const stockCount = (await getStockCount(p.id!)) || 0;
    const row: any = {
      id: undefined,
      productId: p.id,
      productCode: p.code,
      productName: p.name,
      productUnitId: p.unitId,
      productUnitName: p.unitName,
      productBarCode: p.barCode,
      warehouseId: p.defaultWarehouseId,
      productPrice: p.salePrice || 0,
      stockCount,
      count: 1,
      taxPercent: 0,
      brand: p.brand,
      vehicleModel: p.vehicleModel,
      originPlace: p.originPlace,
      standard: p.standard,
    };
    initRow(row);
    tableData.value.push(row);
  }
  emitItemsUpdate(false);
}

function handleDelete(row: any) {
  const index = tableData.value.findIndex((item: any) => item.seq === row.seq);
  if (index !== -1) tableData.value.splice(index, 1);
  emitItemsUpdate(false);
}

function handleRowChange(row: any) {
  initRow(row);
  const index = tableData.value.findIndex((item: any) => item.seq === row.seq);
  if (index === -1) {
    tableData.value.push(row);
    emitItemsUpdate(false);
  } else {
    tableData.value[index] = row;
    emitItemsUpdate(true);
  }
}

function initRow(row: any) {
  if (!row.seq) row.seq = row.id || `${row.productId || 'new'}-${Math.random()}`;
  row.totalPrice = erpPriceMultiply(row.productPrice || 0, row.count || 0) || 0;
  row.taxPrice = erpPriceMultiply(row.totalPrice, (row.taxPercent || 0) / 100) || 0;
}

function validate() {
  if (tableData.value.length === 0) throw new Error('请先添加手推车产品');
  tableData.value.forEach((item, index) => {
    if (!item.productId) throw new Error(`第 ${index + 1} 行：产品不能为空`);
    if (!item.warehouseId) throw new Error(`第 ${index + 1} 行：仓库不能为空`);
    if (!item.count || item.count <= 0) throw new Error(`第 ${index + 1} 行：产品数量不能为空`);
    if ((item.stockCount || 0) < item.count) throw new Error(`第 ${index + 1} 行：库存不足`);
  });
}

defineExpose({ validate });
</script>

<template>
  <Grid class="w-full">
    <template #productId="{ row }"><div class="flex flex-col leading-tight"><span>{{ row.productName || '-' }}</span><span v-if="row.productCode" class="text-xs text-gray-400">{{ row.productCode }}</span></div></template>
    <template #count="{ row }"><InputNumber v-if="!disabled" v-model:value="row.count" :min="0" :precision="3" @change="handleRowChange(row)" /><span v-else>{{ erpCountInputFormatter(row.count) || '-' }}</span></template>
    <template #productPrice="{ row }"><InputNumber v-if="!disabled" v-model:value="row.productPrice" :min="0" :precision="2" @change="handleRowChange(row)" /><span v-else>{{ erpPriceInputFormatter(row.productPrice) || '-' }}</span></template>
    <template #taxPercent="{ row }"><InputNumber v-if="!disabled" v-model:value="row.taxPercent" :min="0" :max="100" :precision="2" @change="handleRowChange(row)" /><span v-else>{{ row.taxPercent || '-' }}</span></template>
    <template #brand="{ row }"><Input v-if="!disabled" v-model:value="row.brand" @change="handleRowChange(row)" /><span v-else>{{ row.brand || '-' }}</span></template>
    <template #vehicleModel="{ row }"><Input v-if="!disabled" v-model:value="row.vehicleModel" @change="handleRowChange(row)" /><span v-else>{{ row.vehicleModel || '-' }}</span></template>
    <template #originPlace="{ row }"><Input v-if="!disabled" v-model:value="row.originPlace" @change="handleRowChange(row)" /><span v-else>{{ row.originPlace || '-' }}</span></template>
    <template #standard="{ row }"><Input v-if="!disabled" v-model:value="row.standard" @change="handleRowChange(row)" /><span v-else>{{ row.standard || '-' }}</span></template>
    <template #remark="{ row }"><Input v-if="!disabled" v-model:value="row.remark" @change="handleRowChange(row)" /><span v-else>{{ row.remark || '-' }}</span></template>
    <template #actions="{ row }"><TableAction :actions="[{ label: '删除', type: 'link', danger: true, popConfirm: { title: '确认删除该产品吗？', confirm: handleDelete.bind(null, row) } }]" /></template>
    <template #bottom>
      <div class="mt-2 rounded border border-border bg-muted p-2"><div class="flex justify-between text-sm text-muted-foreground"><span class="font-medium text-foreground">合计：</span><div class="flex gap-4"><span>数量：{{ erpCountInputFormatter(summaries.count) }}</span><span>金额：{{ erpPriceInputFormatter(summaries.totalPrice) }}</span><span>税额：{{ erpPriceInputFormatter(summaries.taxPrice) }}</span></div></div></div>
      <TableAction v-if="!disabled" class="mt-2 flex justify-center" :actions="[{ label: '添加手推车产品', type: 'primary', onClick: handleOpenSelect }]" />
    </template>
  </Grid>
  <ProductSelectModal ref="productSelectRef" @success="handleProductsSelected" />
</template>
