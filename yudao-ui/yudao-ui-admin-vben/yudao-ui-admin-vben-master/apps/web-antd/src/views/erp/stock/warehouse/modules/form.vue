<script lang="ts" setup>
import type { ErpWarehouseApi } from '#/api/erp/stock/warehouse';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createWarehouse,
  getWarehouse,
  updateWarehouse,
} from '#/api/erp/stock/warehouse';
import { $t } from '#/locales';

import { useFormSchema } from '../data';
import BranchSelectModal from './branch-select-modal.vue';

const emit = defineEmits(['success']);
const formData = ref<ErpWarehouseApi.Warehouse>();
const branchTenantIds = ref<number[]>([]);

const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['仓库'])
    : $t('ui.actionTitle.create', ['仓库']);
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    labelWidth: 120,
  },
  wrapperClass: 'grid-cols-3',
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [BranchModal, branchModalApi] = useVbenModal({
  connectedComponent: BranchSelectModal,
  destroyOnClose: true,
});

/** 打开分店选择弹窗 */
function handleOpenBranchModal() {
  branchModalApi.setData(branchTenantIds.value).open();
}

/** 分店选择确认 */
function handleBranchConfirm(ids: number[]) {
  branchTenantIds.value = ids;
}

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    // 提交表单
    const data = (await formApi.getValues()) as ErpWarehouseApi.Warehouse;
    data.branchTenantIds = branchTenantIds.value;
    try {
      await (formData.value?.id
        ? updateWarehouse(data)
        : createWarehouse(data));
      // 关闭并提示
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      branchTenantIds.value = [];
      return;
    }
    // 加载数据
    const data = modalApi.getData<ErpWarehouseApi.Warehouse>();
    if (!data || !data.id) {
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getWarehouse(data.id);
      branchTenantIds.value = formData.value.branchTenantIds || [];
      // 设置到 values
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[900px]">
    <Form class="mx-4" />
    <div class="mx-4 mb-4 flex items-center gap-2">
      <Button type="primary" ghost @click="handleOpenBranchModal">
        分店查询
      </Button>
      <span v-if="branchTenantIds.length" class="text-gray-500 text-sm">
        已选 {{ branchTenantIds.length }} 个分店
      </span>
    </div>
    <BranchModal @confirm="handleBranchConfirm" />
  </Modal>
</template>
