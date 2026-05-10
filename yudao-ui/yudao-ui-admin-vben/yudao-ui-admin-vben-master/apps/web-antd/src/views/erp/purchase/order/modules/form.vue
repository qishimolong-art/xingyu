<script lang="ts" setup>
import type { ErpPurchaseOrderApi } from '#/api/erp/purchase/order';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { Button, message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { getAccountSimpleList } from '#/api/erp/finance/account';
import {
  createPurchaseOrder,
  getPurchaseOrder,
  updatePurchaseOrder,
} from '#/api/erp/purchase/order';
import { $t } from '#/locales';

import { applyFieldConfig } from '#/views/erp/composables/useFieldConfig';

import { useFormSchema } from '../data';
import ImportForm from './import-form.vue';
import PurchaseOrderItemForm from './item-form.vue';

const emit = defineEmits(['success']);
const formData = ref<ErpPurchaseOrderApi.PurchaseOrder>();
const formType = ref(''); // 表单类型：'create' | 'edit' | 'detail'
const itemFormRef = ref<InstanceType<typeof PurchaseOrderItemForm>>();
const userStore = useUserStore();

/** 导入弹窗 */
const [ImportFormModal, importFormModalApi] = useVbenModal({
  connectedComponent: ImportForm,
});

/** 打开导入弹窗 */
function handleImport() {
  importFormModalApi.open();
}

/** 导入成功回调 */
function handleImportSuccess(data: any) {
  formApi.setValues(data);
}

const getTitle = computed(() => {
  if (formType.value === 'create') {
    return $t('ui.actionTitle.create', ['采购订单']);
  } else if (formType.value === 'edit') {
    return $t('ui.actionTitle.edit', ['采购订单']);
  } else {
    return '采购订单详情';
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
  handleValuesChange: (values, changedFields) => {
    // 目的：同步到 item-form 组件，触发整体的价格计算
    if (formData.value && changedFields.includes('discountPercent')) {
      formData.value.discountPercent = values.discountPercent;
    }
  },
});

/** 更新采购订单项 */
function handleUpdateItems(items: ErpPurchaseOrderApi.PurchaseOrderItem[]) {
  formData.value = modalApi.getData<ErpPurchaseOrderApi.PurchaseOrder>();
  formData.value.items = items;
  formApi.setValues({
    items,
  });
}

/** 更新优惠金额 */
function handleUpdateDiscountPrice(discountPrice: number) {
  formApi.setValues({
    discountPrice,
  });
}

/** 更新总金额 */
function handleUpdateTotalPrice(totalPrice: number) {
  formApi.setValues({
    totalPrice,
  });
}

/** 创建或更新采购订单 */
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
    const data =
      (await formApi.getValues()) as ErpPurchaseOrderApi.PurchaseOrder;
    data.items = formData.value?.items?.map((item) => ({
      ...item,
      // 解决新增采购订单报错
      id: undefined,
    }));
    // 将文件数组转换为字符串
    if (data.fileUrl && Array.isArray(data.fileUrl)) {
      data.fileUrl = data.fileUrl.length > 0 ? data.fileUrl[0] : '';
    }
    try {
      await (formType.value === 'create'
        ? createPurchaseOrder(data)
        : updatePurchaseOrder(data));
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
      return;
    }
    // 加载数据
    const data = modalApi.getData<{ id?: number; type: string }>();
    formType.value = data.type;
    formApi.setDisabled(formType.value === 'detail');
    const baseSchema = useFormSchema(formType.value);
    const configuredSchema = await applyFieldConfig(baseSchema, 'purchase_order');
    formApi.setState({ schema: configuredSchema });
    if (!data || !data.id) {
      // 新增时，默认选中账户
      const accountList = await getAccountSimpleList();
      const defaultAccount = accountList.find((item) => item.defaultStatus);
      if (defaultAccount) {
        await formApi.setValues({ accountId: defaultAccount.id });
      }
      // 新增时设置创建时间为当前时间
      await formApi.setValues({ orderTime: Date.now() });
      // 新增时自动填充采购员和部门
      const userInfo = userStore.userInfo;
      if (userInfo) {
        await formApi.setValues({
          purchaser: userInfo.id,
          deptId: userInfo.deptId,
        });
      }
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getPurchaseOrder(data.id);
      // 设置到 values
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
    class="w-3/4"
    :show-confirm-button="formType !== 'detail'"
  >
    <div v-if="formType === 'create'" class="mx-3 mb-2 flex justify-end">
      <Button type="primary" ghost @click="handleImport">导入</Button>
    </div>
    <Form class="mx-3">
      <template #items>
        <PurchaseOrderItemForm
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
  <ImportFormModal @success="handleImportSuccess" />
</template>
