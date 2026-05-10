<script lang="ts" setup>
import type { ErpPurchaseInApi } from '#/api/erp/purchase/in';

import { computed, nextTick, onMounted, ref, watch } from 'vue';

import {
  erpCountInputFormatter,
  erpPriceInputFormatter,
  erpPriceMultiply,
} from '@vben/utils';

import { Input, InputNumber, Select } from 'ant-design-vue';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getWarehouseStockCount } from '#/api/erp/stock/stock';
import { getWarehouseSimpleList } from '#/api/erp/stock/warehouse';

import { useFormItemColumns } from '../data';

interface Props {
  items?: ErpPurchaseInApi.PurchaseInItem[];
  disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  items: () => [],
  disabled: false,
});

const emit = defineEmits(['update:items']);

const tableData = ref<ErpPurchaseInApi.PurchaseInItem[]>([]); // 表格数据
const warehouseOptions = ref<any[]>([]); // 仓库下拉选项
let isInternalUpdate = false; // 标记：本次 props.items 变更是否由内部字段编辑触发，是则跳过 reloadData 以避免横向滚动被重置

/** 发出 items 更新
 * @param skipReload 是否跳过 grid.reloadData；仅在"字段编辑"场景传 true（保留滚动位置），"增删行"场景传 false
 */
function emitItemsUpdate(skipReload = false) {
  isInternalUpdate = skipReload;
  emit('update:items', [...tableData.value]);
}

/** 获取表格合计数据 */
const summaries = computed(() => {
  return {
    count: tableData.value.reduce((sum, item) => sum + (item.count || 0), 0),
    totalProductPrice: tableData.value.reduce(
      (sum, item) => sum + (item.totalProductPrice || 0),
      0,
    ),
    taxPrice: tableData.value.reduce(
      (sum, item) => sum + (item.taxPrice || 0),
      0,
    ),
    totalPrice: tableData.value.reduce(
      (sum, item) => sum + (item.totalPrice || 0),
      0,
    ),
  };
});

/** 表格配置 */
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useFormItemColumns(tableData.value, props.disabled),
    data: tableData.value,
    minHeight: 250,
    autoResize: true,
    border: true,
    rowConfig: {
      keyField: 'seq',
      isHover: true,
    },
    pagerConfig: {
      enabled: false,
    },
    toolbarConfig: {
      enabled: false,
    },
  },
});

/** 监听外部传入的列数据 */
watch(
  () => props.items,
  async (items) => {
    if (!items) {
      return;
    }
    // 内部编辑触发的回传，直接跳过 reloadData，避免横向滚动被重置
    if (isInternalUpdate) {
      isInternalUpdate = false;
      return;
    }
    items.forEach((item) => initRow(item));
    tableData.value = [...items];
    await nextTick(); // 特殊：保证 gridApi 已经初始化
    await gridApi.grid.reloadData(tableData.value);
    // 更新表格列配置（目的：原数量、已入库动态列）
    const columns = useFormItemColumns(tableData.value, props.disabled);
    await gridApi.grid.reloadColumn(columns || []);
  },
  {
    immediate: true,
  },
);

/** 处理删除 */
function handleDelete(row: ErpPurchaseInApi.PurchaseInItem) {
  const index = tableData.value.findIndex((item) => item.seq === row.seq);
  if (index !== -1) {
    tableData.value.splice(index, 1);
  }
  // 通知父组件更新（删行需要 reloadData）
  emitItemsUpdate(false);
}

/** 处理仓库变更 */
async function handleWarehouseChange(row: ErpPurchaseInApi.PurchaseInItem) {
  const stockCount = await getWarehouseStockCount({
    productId: row.productId!,
    warehouseId: row.warehouseId!,
  });
  row.stockCount = stockCount || 0;
  handleRowChange(row);
}

/** 处理行数据变更 */
function handleRowChange(row: any) {
  // TODO 芋艿
  const index = tableData.value.findIndex((item) => item.seq === row.seq);
  if (index === -1) {
    tableData.value.push(row);
    emitItemsUpdate(false); // 新增行需要 reloadData
  } else {
    tableData.value[index] = row;
    emitItemsUpdate(true); // 字段编辑跳过 reloadData，保住横向滚动
  }
}

/** 整件数变更 → 自动计算数量 count = wholeQty × packageQty */
function handleWholeQtyChange(row: any) {
  const wholeQty = Number(row.wholeQty) || 0;
  const packageQty = Number(row.packageQty) || 1;
  row.count = wholeQty * packageQty;
  handleRowChange(row);
}

/** 初始化行数据 */
function initRow(row: ErpPurchaseInApi.PurchaseInItem) {
  // 包装数兜底为 1
  if (!row.packageQty || row.packageQty <= 0) {
    row.packageQty = 1;
  }
  if (row.productPrice && row.count) {
    row.totalProductPrice = erpPriceMultiply(row.productPrice, row.count) ?? 0;
    row.taxPrice =
      erpPriceMultiply(row.totalProductPrice, (row.taxPercent || 0) / 100) ?? 0;
    row.totalPrice = row.totalProductPrice + row.taxPrice;
  }
}

/** 表单校验 */
function validate() {
  for (let i = 0; i < tableData.value.length; i++) {
    const item = tableData.value[i];
    if (item) {
      if (!item.warehouseId) {
        throw new Error(`第 ${i + 1} 行：仓库不能为空`);
      }
      if (!item.count || item.count <= 0) {
        throw new Error(`第 ${i + 1} 行：产品数量不能为空`);
      }
    }
  }
}

defineExpose({
  validate,
});

/** 初始化 */
onMounted(async () => {
  warehouseOptions.value = await getWarehouseSimpleList();
});
</script>

<template>
  <Grid class="w-full">
    <template #warehouseId="{ row }">
      <Select
        v-model:value="row.warehouseId"
        :options="warehouseOptions"
        :field-names="{ label: 'name', value: 'id' }"
        placeholder="请选择仓库"
        :disabled="disabled"
        show-search
        class="w-full"
        @change="handleWarehouseChange(row)"
      />
    </template>
    <template #productId="{ row }">
      <div class="flex flex-col leading-tight">
        <span>{{ row.productName || '-' }}</span>
        <span v-if="row.productCode" class="text-xs text-gray-400">
          {{ row.productCode }}
        </span>
      </div>
    </template>
    <template #count="{ row }">
      <InputNumber
        v-if="!disabled"
        v-model:value="row.count"
        :min="0"
        :precision="2"
        @change="handleRowChange(row)"
      />
      <span v-else>{{ erpCountInputFormatter(row.count) || '-' }}</span>
    </template>
    <template #productPrice="{ row }">
      <InputNumber
        v-if="!disabled"
        v-model:value="row.productPrice"
        :min="0"
        :precision="2"
        @change="handleRowChange(row)"
      />
      <span v-else>{{ erpPriceInputFormatter(row.productPrice) || '-' }}</span>
    </template>
    <template #remark="{ row }">
      <Input v-if="!disabled" v-model:value="row.remark" class="w-full" />
      <span v-else>{{ row.remark || '-' }}</span>
    </template>
    <template #wholeQty="{ row }">
      <InputNumber
        v-if="!disabled"
        v-model:value="row.wholeQty"
        :min="0"
        :precision="0"
        placeholder="整件数"
        class="w-full"
        @change="handleWholeQtyChange(row)"
      />
      <span v-else>{{ row.wholeQty || '-' }}</span>
    </template>
    <template #warehousePosition="{ row }">
      <Input
        v-if="!disabled"
        v-model:value="row.warehousePosition"
        placeholder="货架位"
        class="w-full"
      />
      <span v-else>{{ row.warehousePosition || '-' }}</span>
    </template>
    <template #barCode="{ row }">
      <Input
        v-if="!disabled"
        v-model:value="row.barCode"
        placeholder="条形码"
        class="w-full"
      />
      <span v-else>{{ row.barCode || '-' }}</span>
    </template>
    <template #batchNo="{ row }">
      <Input
        v-if="!disabled"
        v-model:value="row.batchNo"
        placeholder="批次"
        class="w-full"
      />
      <span v-else>{{ row.batchNo || '-' }}</span>
    </template>
    <template #drawingNo="{ row }">
      <Input
        v-if="!disabled"
        v-model:value="row.drawingNo"
        placeholder="图号"
        class="w-full"
      />
      <span v-else>{{ row.drawingNo || '-' }}</span>
    </template>
    <template #taxPercent="{ row }">
      <InputNumber
        v-if="!disabled"
        v-model:value="row.taxPercent"
        :min="0"
        :max="100"
        :precision="2"
        @change="handleRowChange(row)"
      />
      <span v-else>{{ row.taxPercent || '-' }}</span>
    </template>
    <template #actions="{ row }">
      <TableAction
        :actions="[
          {
            label: '删除',
            type: 'link',
            danger: true,
            popConfirm: {
              title: '确认删除该产品吗？',
              confirm: handleDelete.bind(null, row),
            },
          },
        ]"
      />
    </template>

    <template #bottom>
      <div class="mt-2 rounded border border-border bg-muted p-2">
        <div class="flex justify-between text-sm text-muted-foreground">
          <span class="font-medium text-foreground">合计：</span>
          <div class="flex space-x-4">
            <span>数量：{{ erpCountInputFormatter(summaries.count) }}</span>
            <span>
              金额：{{ erpPriceInputFormatter(summaries.totalProductPrice) }}
            </span>
            <span>税额：{{ erpPriceInputFormatter(summaries.taxPrice) }}</span>
            <span>
              税额合计：{{ erpPriceInputFormatter(summaries.totalPrice) }}
            </span>
          </div>
        </div>
      </div>
    </template>
  </Grid>
</template>
