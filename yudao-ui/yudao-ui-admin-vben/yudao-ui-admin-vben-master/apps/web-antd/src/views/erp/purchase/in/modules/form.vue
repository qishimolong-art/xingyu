<script lang="ts" setup>
import type { ErpPurchaseInApi } from '#/api/erp/purchase/in';
import type { ErpPurchaseOrderApi } from '#/api/erp/purchase/order';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createPurchaseIn,
  getPurchaseIn,
  updatePurchaseIn,
} from '#/api/erp/purchase/in';

import { useFormSchema } from '../data';
import ItemForm from './item-form.vue';
import PurchaseOrderSelect from './purchase-order-select.vue';

import { applyFieldConfig } from '#/views/erp/composables/useFieldConfig';

const emit = defineEmits(['success']);
const formData = ref<
  ErpPurchaseInApi.PurchaseIn & {
    customerId?: number;
    fileUrl?: string;
    order?: ErpPurchaseOrderApi.PurchaseOrder;
    orderId?: number;
    orderNo?: string;
  }
>({
  id: undefined,
  no: undefined,
  inTime: undefined,
  remark: undefined,
  fileUrl: undefined,
  supplierId: undefined,
  items: [],
});
const formType = ref(''); // 表单类型：'create' | 'edit' | 'detail'
const itemFormRef = ref<InstanceType<typeof ItemForm>>();

const getTitle = computed(() => {
  if (formType.value === 'create') {
    return $t('ui.actionTitle.create', ['采购入库']);
  } else if (formType.value === 'edit') {
    return $t('ui.actionTitle.edit', ['采购入库']);
  } else {
    return '采购入库详情';
  }
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    labelWidth: 120,
  },
  wrapperClass: 'grid-cols-3',
  layout: 'vertical',
  schema: useFormSchema(formType.value),
  showDefaultActions: false,
});

/** 更新采购入库项 */
const handleUpdateItems = (items: ErpPurchaseInApi.PurchaseInItem[]) => {
  formData.value.items = items;
  formApi.setValues({
    items,
  });
};

/** 选择采购订单 */
const handleUpdateOrder = (order: ErpPurchaseOrderApi.PurchaseOrder) => {
  formData.value = {
    ...formData.value,
    orderId: order.id,
    orderNo: order.no!,
    supplierId: order.supplierId!,
    remark: order.remark!,
    fileUrl: order.fileUrl!,
  };
  // 将订单项设置到入库单项
  order.items!.forEach((item: any) => {
    item.totalCount = item.count;
    item.count = item.totalCount - item.inCount;
    item.orderItemId = item.id;
    item.id = undefined;
    // 包装数兜底为 1（订单项未携带时）
    if (!item.packageQty || item.packageQty <= 0) {
      item.packageQty = 1;
    }
  });
  formData.value.items = order.items!.filter(
    (item) => item.count && item.count > 0,
  ) as ErpPurchaseInApi.PurchaseInItem[];
  formApi.setValues(formData.value, false);
};

/** 创建或更新采购入库 */
const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const itemFormInstance = Array.isArray(itemFormRef.value)
      ? itemFormRef.value[0]
      : itemFormRef.value;
    try {
      itemFormInstance.validate();
    } catch (error: any) {
      message.error(error.message || '子表单验证失败');
      return;
    }

    modalApi.lock();
    // 提交表单
    const data = (await formApi.getValues()) as ErpPurchaseInApi.PurchaseIn;
    try {
      await (formType.value === 'create'
        ? createPurchaseIn(data)
        : updatePurchaseIn(data));
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
      formData.value = {} as ErpPurchaseInApi.PurchaseIn;
      return;
    }
    // 加载数据
    const data = modalApi.getData<{ id?: number; type: string }>();
    formType.value = data.type;
    formApi.setDisabled(formType.value === 'detail');
    const baseSchema = useFormSchema(formType.value);
    const configuredSchema = await applyFieldConfig(baseSchema, 'purchase_in');
    formApi.setState({ schema: configuredSchema });
    if (!data || !data.id) {
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getPurchaseIn(data.id);
      // 设置到 values
      await formApi.setValues(formData.value, false);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal
    :title="getTitle"
    class="w-3/4"
    :show-confirm-button="formType !== 'detail'"
  >
    <Form class="mx-3">
      <template #items>
        <ItemForm
          ref="itemFormRef"
          :items="formData?.items ?? []"
          :disabled="formType === 'detail'"
          @update:items="handleUpdateItems"
        />
      </template>
      <template #orderNo>
        <PurchaseOrderSelect
          :order-no="formData?.orderNo"
          @update:order="handleUpdateOrder"
        />
      </template>
    </Form>
  </Modal>
</template>
