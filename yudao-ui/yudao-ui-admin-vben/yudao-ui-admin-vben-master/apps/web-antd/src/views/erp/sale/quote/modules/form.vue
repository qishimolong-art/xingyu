<script lang="ts" setup>
import type { ErpSaleQuoteApi } from '#/api/erp/sale/quote';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { getAccountSimpleList } from '#/api/erp/finance/account';
import {
  createSaleQuote,
  getSaleQuote,
  updateSaleQuote,
} from '#/api/erp/sale/quote';
import { $t } from '#/locales';

import { useFormSchema } from '../data';
import ItemForm from './item-form.vue';

const emit = defineEmits(['success']);
const formData = ref<ErpSaleQuoteApi.SaleQuote>();
const formType = ref('');
const itemFormRef = ref<InstanceType<typeof ItemForm>>();

const getTitle = computed(() => {
  if (formType.value === 'create') return '新增报价订单';
  if (formType.value === 'edit') return '编辑报价订单';
  return '报价订单详情';
});

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, labelWidth: 120 },
  wrapperClass: 'grid-cols-3',
  layout: 'vertical',
  schema: useFormSchema(formType.value),
  showDefaultActions: false,
  handleValuesChange: (values, changedFields) => {
    if (formData.value && changedFields.includes('discountPercent')) {
      formData.value.discountPercent = values.discountPercent;
    }
  },
});

function handleUpdateItems(items: ErpSaleQuoteApi.SaleQuoteItem[]) {
  formData.value = modalApi.getData<ErpSaleQuoteApi.SaleQuote>() || {};
  formData.value.items = items;
  formApi.setValues({ items });
}

function handleUpdateDiscountPrice(discountPrice: number) {
  formApi.setValues({ discountPrice });
}

function handleUpdateTotalPrice(totalPrice: number) {
  formApi.setValues({ totalPrice });
}

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const itemFormInstance = Array.isArray(itemFormRef.value)
      ? itemFormRef.value[0]
      : itemFormRef.value;
    try {
      itemFormInstance?.validate();
    } catch (error: any) {
      message.error(error.message || '子表单验证失败');
      return;
    }

    modalApi.lock();
    const data = (await formApi.getValues()) as ErpSaleQuoteApi.SaleQuote;
    try {
      await (formType.value === 'create'
        ? createSaleQuote(data)
        : updateSaleQuote(data));
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
      return;
    }
    const data = modalApi.getData<{ id?: number; type: string }>();
    formType.value = data.type;
    formApi.setDisabled(formType.value === 'detail');
    formApi.updateSchema(useFormSchema(formType.value));
    if (!data.id) {
      formData.value = { items: [], discountPercent: 0, quoteTime: Date.now() };
      const accountList = await getAccountSimpleList();
      const defaultAccount = accountList.find((item) => item.defaultStatus);
      await formApi.setValues({
        quoteTime: Date.now(),
        discountPercent: 0,
        otherPrice: 0,
        accountId: defaultAccount?.id,
      });
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getSaleQuote(data.id);
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal
    :title="getTitle"
    class="!w-[85vw]"
    :show-confirm-button="formType !== 'detail'"
  >
    <Form class="mx-3">
      <template #items>
        <ItemForm
          ref="itemFormRef"
          :items="formData?.items ?? []"
          :disabled="formType === 'detail'"
          :discount-percent="formData?.discountPercent ?? 0"
          @update:items="handleUpdateItems"
          @update:discount-price="handleUpdateDiscountPrice"
          @update:total-price="handleUpdateTotalPrice"
        />
      </template>
    </Form>
  </Modal>
</template>
