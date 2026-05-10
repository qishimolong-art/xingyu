<script lang="ts" setup>
import type { ErpProductApi } from '#/api/erp/product/product';

import { computed } from 'vue';

import { Alert, Descriptions, DescriptionsItem } from 'ant-design-vue';

const props = defineProps<{ value?: ErpProductApi.Product }>();

const warning = computed(
  () =>
    props.value?.lowStockWarning ||
    (props.value?.stockMin != null &&
      Number(props.value?.currentStock ?? 0) <= Number(props.value.stockMin)),
);

function format(v?: number | string) {
  if (v == null || v === '') return '0';
  return String(v);
}
</script>

<template>
  <div class="mx-2 py-2">
    <Alert
      v-if="warning"
      type="warning"
      show-icon
      message="低于安全库存"
      description="当前库存已达到或低于库存下限，请及时补货。"
      class="mb-4"
    />
    <Descriptions bordered :column="3" size="middle">
      <DescriptionsItem label="当前库存">
        {{ format(value?.currentStock) }}
      </DescriptionsItem>
      <DescriptionsItem label="在途数量">
        {{ format(value?.inTransitStock) }}
      </DescriptionsItem>
      <DescriptionsItem label="可用库存">
        {{ format(value?.availableStock) }}
      </DescriptionsItem>
      <DescriptionsItem label="库存下限">
        {{ format(value?.stockMin) }}
      </DescriptionsItem>
      <DescriptionsItem label="标准库存">
        {{ format(value?.stockStandard) }}
      </DescriptionsItem>
      <DescriptionsItem label="库存上限">
        {{ format(value?.stockMax) }}
      </DescriptionsItem>
    </Descriptions>
    <div class="mt-3 text-xs text-gray-500">
      当前库存由 erp_stock 实时聚合；在途数量 / 可用库存为预留字段，后续接入采购在途后自动计算。
    </div>
  </div>
</template>
