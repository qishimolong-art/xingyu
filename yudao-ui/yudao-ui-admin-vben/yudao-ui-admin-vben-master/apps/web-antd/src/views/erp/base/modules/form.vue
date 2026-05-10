<script lang="ts" setup>
import type { ErpBaseDataApi } from '#/api/erp/base';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createBaseData,
  getBaseData,
  updateBaseData,
} from '#/api/erp/base';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

interface ModalData {
  type: string;
  title: string;
  row?: ErpBaseDataApi.BaseData;
}

const emit = defineEmits(['success']);
const formData = ref<ErpBaseDataApi.BaseData>();
const modalData = ref<ModalData>();
const getTitle = computed(() => {
  const title = modalData.value?.title || '';
  return formData.value?.id
    ? $t('ui.actionTitle.edit', [title])
    : $t('ui.actionTitle.create', [title]);
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-2',
    labelWidth: 80,
  },
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    const data = (await formApi.getValues()) as ErpBaseDataApi.BaseData;
    data.type = modalData.value?.type || '';
    try {
      await (formData.value?.id
        ? updateBaseData(data)
        : createBaseData(data));
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
      modalData.value = undefined;
      return;
    }
    const data = modalApi.getData<ModalData>();
    modalData.value = data;
    if (!data?.row?.id) {
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getBaseData(data.row.id);
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-1/2">
    <Form class="mx-4" />
  </Modal>
</template>
