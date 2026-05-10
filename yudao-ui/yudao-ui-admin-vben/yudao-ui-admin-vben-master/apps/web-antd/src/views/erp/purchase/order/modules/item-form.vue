<script lang="ts" setup>
import type { ErpProductApi } from '#/api/erp/product/product';
import type { ErpPurchaseOrderApi } from '#/api/erp/purchase/order';

import { computed, nextTick, onMounted, ref, watch } from 'vue';

import {
  erpCountInputFormatter,
  erpPriceInputFormatter,
  erpPriceMultiply,
} from '@vben/utils';

import { Input, InputNumber, Select, Switch, Tag } from 'ant-design-vue';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getSupplierSimpleList } from '#/api/erp/purchase/supplier';
import { getStockCount } from '#/api/erp/stock/stock';
import { getWarehouseSimpleList } from '#/api/erp/stock/warehouse';
import ProductSelectModal from '#/views/erp/product/product/modules/product-select-modal.vue';

import { useFormItemColumns } from '../data';

interface Props {
  items?: ErpPurchaseOrderApi.PurchaseOrderItem[];
  disabled?: boolean;
  discountPercent?: number;
}

const props = withDefaults(defineProps<Props>(), {
  items: () => [],
  disabled: false,
  discountPercent: 0,
});

const emit = defineEmits([
  'update:items',
  'update:discount-price',
  'update:total-price',
]);

const tableData = ref<ErpPurchaseOrderApi.PurchaseOrderItem[]>([]); // 表格数据
const productSelectRef = ref<InstanceType<typeof ProductSelectModal>>();
const warehouseOptions = ref<any[]>([]); // 仓库下拉选项
const supplierOptions = ref<any[]>([]); // 供应商下拉选项
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
  };
});

/** 表格配置 */
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useFormItemColumns(props.disabled),
    data: tableData.value,
    minHeight: 250,
    autoResize: true,
    border: true,
    rowConfig: {
      keyField: 'seq',
      isHover: true,
    },
    rowClassName: ({ row }: { row: any }) =>
      row?.gift ? 'erp-gift-row' : '',
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
  },
  {
    immediate: true,
  },
);

/** 计算 discountPrice、totalPrice 价格 */
watch(
  () => [tableData.value, props.discountPercent],
  () => {
    if (!tableData.value || tableData.value.length === 0) {
      return;
    }
    const totalPrice = tableData.value.reduce(
      (prev, curr) => prev + (curr.totalProductPrice || 0),
      0,
    );
    const discountPrice =
      props.discountPercent === null
        ? 0
        : erpPriceMultiply(totalPrice, props.discountPercent / 100);
    const finalTotalPrice = totalPrice - discountPrice!;
    // 通知父组件更新
    emit('update:discount-price', discountPrice);
    emit('update:total-price', finalTotalPrice);
  },
  { deep: true },
);

/** 打开配件选择弹窗 */
function handleOpenSelect() {
  productSelectRef.value?.open();
}

/** 弹窗勾选配件后批量追加 */
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
      productPrice: p.purchasePrice || 0,
      stockCount,
      count: 1,
      totalProductPrice: undefined,
      remark: undefined,
      // 产品带出字段
      vehicleModel: p.vehicleModel,
      originPlace: p.originPlace,
      standard: p.standard,
      featureCode: p.featureCode,
      drawingNo: p.drawingNo,
      factoryCode: p.factoryCode,
      brand: p.brand,
      warehouseId: (p as any).defaultWarehouseId || undefined,
      warehousePosition: undefined,
      batchNo: undefined,
      supplierId: undefined,
      arrivalCount: 0,
    };
    initRow(row);
    tableData.value.push(row);
  }
  emitItemsUpdate();
}

/** 处理删除 */
function handleDelete(row: ErpPurchaseOrderApi.PurchaseOrderItem) {
  const index = tableData.value.findIndex((item) => item.seq === row.seq);
  if (index !== -1) {
    tableData.value.splice(index, 1);
  }
  emitItemsUpdate();
}

/** 处理行数据变更 */
function handleRowChange(row: any) {
  initRow(row);
  const index = tableData.value.findIndex((item) => item.seq === row.seq);
  if (index === -1) {
    tableData.value.push(row);
    emitItemsUpdate(false); // 新增行需要 reloadData
  } else {
    tableData.value[index] = row;
    emitItemsUpdate(true); // 字段编辑跳过 reloadData，保住横向滚动
  }
}

/** 初始化行数据 */
function initRow(row: ErpPurchaseOrderApi.PurchaseOrderItem) {
  // 赠品行：强制 productPrice=0
  if (row.gift) {
    row.productPrice = 0;
  }
  if (row.productPrice && row.count) {
    row.totalProductPrice = erpPriceMultiply(row.productPrice, row.count) ?? 0;
  }
}

/** 处理赠品切换 */
function handleGiftChange(row: any) {
  if (row.gift) {
    row.productPrice = 0;
  }
  initRow(row);
  handleRowChange(row);
}

/** 表单校验 */
function validate() {
  if (tableData.value.length === 0) {
    throw new Error('请先添加采购产品');
  }
  for (let i = 0; i < tableData.value.length; i++) {
    const item = tableData.value[i];
    if (item) {
      if (!item.productId) {
        throw new Error(`第 ${i + 1} 行：产品不能为空`);
      }
      if (!item.count || item.count <= 0) {
        throw new Error(`第 ${i + 1} 行：产品数量不能为空`);
      }
      // 赠品行不校验单价
      if (!item.gift && (!item.productPrice || item.productPrice <= 0)) {
        throw new Error(`第 ${i + 1} 行：产品单价不能为空`);
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
  supplierOptions.value = await getSupplierSimpleList();
});
</script>

<template>
  <div class="w-full">
    <div v-if="!disabled" class="mb-2 flex justify-end">
      <TableAction
        :actions="[
          {
            label: '添加采购产品',
            type: 'primary',
            onClick: handleOpenSelect,
          },
        ]"
      />
    </div>
    <Grid class="w-full">
      <template #productId="{ row }">
        <div class="flex flex-col leading-tight">
          <span>{{ row.productName || '-' }}</span>
        </div>
      </template>
      <template #count="{ row }">
        <InputNumber
          v-if="!disabled"
          v-model:value="row.count"
          :min="0"
          :precision="3"
          class="w-full"
          @change="handleRowChange(row)"
        />
        <span v-else>{{ erpCountInputFormatter(row.count) || '-' }}</span>
      </template>
      <template #productPrice="{ row }">
        <InputNumber
          v-if="!disabled && !row.gift"
          v-model:value="row.productPrice"
          :min="0"
          :precision="2"
          class="w-full"
          @change="handleRowChange(row)"
        />
        <span v-else-if="row.gift" class="text-gray-400">0（赠品）</span>
        <span v-else>{{ erpPriceInputFormatter(row.productPrice) || '-' }}</span>
      </template>
      <template #warehouseId="{ row }">
        <Select
          v-model:value="row.warehouseId"
          :options="warehouseOptions"
          :field-names="{ label: 'name', value: 'id' }"
          placeholder="请选择仓库"
          :disabled="disabled"
          show-search
          allow-clear
          class="w-full"
          @change="handleRowChange(row)"
        />
      </template>
      <template #warehousePosition="{ row }">
        <Input
          v-if="!disabled"
          v-model:value="row.warehousePosition"
          class="w-full"
          @change="handleRowChange(row)"
        />
        <span v-else>{{ row.warehousePosition || '-' }}</span>
      </template>
      <template #drawingNo="{ row }">
        <Input
          v-if="!disabled"
          v-model:value="row.drawingNo"
          class="w-full"
          @change="handleRowChange(row)"
        />
        <span v-else>{{ row.drawingNo || '-' }}</span>
      </template>
      <template #batchNo="{ row }">
        <Input
          v-if="!disabled"
          v-model:value="row.batchNo"
          class="w-full"
          @change="handleRowChange(row)"
        />
        <span v-else>{{ row.batchNo || '-' }}</span>
      </template>
      <template #remark="{ row }">
        <Input v-if="!disabled" v-model:value="row.remark" class="w-full" />
        <span v-else>{{ row.remark || '-' }}</span>
      </template>
      <template #supplierId="{ row }">
        <Select
          v-model:value="row.supplierId"
          :options="supplierOptions"
          :field-names="{ label: 'name', value: 'id' }"
          placeholder="请选择供应商"
          :disabled="disabled"
          show-search
          allow-clear
          class="w-full"
          @change="handleRowChange(row)"
        />
      </template>
      <template #gift="{ row }">
        <div class="flex items-center gap-1">
          <Switch
            v-model:checked="row.gift"
            :disabled="disabled"
            size="small"
            @change="handleGiftChange(row)"
          />
          <Tag v-if="row.gift" color="red" class="!m-0 !px-1.5 font-bold">
            赠
          </Tag>
        </div>
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
            </div>
          </div>
        </div>
      </template>
    </Grid>
  </div>

  <ProductSelectModal
    ref="productSelectRef"
    @success="handleProductsSelected"
  />
</template>

<style scoped>
:deep(.erp-gift-row) {
  background-color: rgba(255, 77, 79, 0.08) !important;
}
:deep(.erp-gift-row:hover td) {
  background-color: rgba(255, 77, 79, 0.16) !important;
}
</style>
