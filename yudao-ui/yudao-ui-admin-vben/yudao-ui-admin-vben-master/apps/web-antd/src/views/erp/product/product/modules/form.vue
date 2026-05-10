<script lang="ts" setup>
import type { ErpProductApi } from '#/api/erp/product/product';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message, Tabs } from 'ant-design-vue';

import {
  createProduct,
  getProductDetail,
  updateProduct,
} from '#/api/erp/product/product';
import { $t } from '#/locales';

import BaseInfo from './tabs/base-info.vue';
import DetailInfo from './tabs/detail-info.vue';
import ExtendInfo from './tabs/extend-info.vue';
import ImageInfo from './tabs/image-info.vue';
import PriceInfo from './tabs/price-info.vue';
import StockInfo from './tabs/stock-info.vue';
import UniversalInfo from './tabs/universal-info.vue';

const emit = defineEmits(['success']);

const activeKey = ref('base');
const formData = ref<ErpProductApi.Product>({} as ErpProductApi.Product);

const baseRef = ref<InstanceType<typeof BaseInfo>>();
const priceRef = ref<InstanceType<typeof PriceInfo>>();
const extendRef = ref<InstanceType<typeof ExtendInfo>>();

const mainImage = ref<string>('');
const detailContent = ref<string>('');
const universals = ref<ErpProductApi.ProductUniversal[]>([]);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', ['配件信息'])
    : $t('ui.actionTitle.create', ['配件信息']),
);

function reset() {
  formData.value = {} as ErpProductApi.Product;
  mainImage.value = '';
  detailContent.value = '';
  universals.value = [];
  activeKey.value = 'base';
}

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    // 1. 基础档案必填项校验
    const valid = await baseRef.value?.validate();
    if (!valid) {
      activeKey.value = 'base';
      return;
    }
    // 2. 汇总各 Tab 数据
    const base = (await baseRef.value?.getValues()) ?? {};
    const price = (await priceRef.value?.getValues()) ?? {};
    const extend = (await extendRef.value?.getValues()) ?? {};
    const payload = {
      ...formData.value,
      ...base,
      ...price,
      ...extend,
      mainImage: mainImage.value,
      detailContent: detailContent.value,
      universals: universals.value ?? [],
    } as ErpProductApi.Product;

    modalApi.lock();
    try {
      await (payload.id ? updateProduct(payload) : createProduct(payload));
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      reset();
      return;
    }
    const data = modalApi.getData<ErpProductApi.Product>();
    if (!data || !data.id) {
      return;
    }
    modalApi.lock();
    try {
      const detail = await getProductDetail(data.id);
      formData.value = detail;
      mainImage.value = detail.mainImage ?? '';
      detailContent.value = detail.detailContent ?? '';
      universals.value = detail.universals ?? [];
      await baseRef.value?.setValues(detail);
      await priceRef.value?.setValues(detail);
      await extendRef.value?.setValues(detail);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-4/5">
    <Tabs v-model:active-key="activeKey" class="mx-4">
      <Tabs.TabPane key="base" tab="基础档案" force-render>
        <BaseInfo ref="baseRef" />
      </Tabs.TabPane>
      <Tabs.TabPane key="price" tab="价格列表" force-render>
        <PriceInfo ref="priceRef" />
      </Tabs.TabPane>
      <Tabs.TabPane key="extend" tab="扩展信息" force-render>
        <ExtendInfo ref="extendRef" />
      </Tabs.TabPane>
      <Tabs.TabPane key="image" tab="配件图片">
        <ImageInfo v-model:model-value="mainImage" />
      </Tabs.TabPane>
      <Tabs.TabPane key="detail" tab="配件详情页">
        <DetailInfo v-model:model-value="detailContent" />
      </Tabs.TabPane>
      <Tabs.TabPane key="stock" tab="库存信息">
        <StockInfo :value="formData" />
      </Tabs.TabPane>
      <Tabs.TabPane key="universal" tab="通用件">
        <UniversalInfo v-model:model-value="universals" />
      </Tabs.TabPane>
    </Tabs>
  </Modal>
</template>
