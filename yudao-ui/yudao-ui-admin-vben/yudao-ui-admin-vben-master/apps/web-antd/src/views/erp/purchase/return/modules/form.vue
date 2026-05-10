<script lang="ts" setup>
import type { ErpPurchaseInApi } from '#/api/erp/purchase/in';
import type { ErpPurchaseOrderApi } from '#/api/erp/purchase/order';
import type { ErpPurchaseReturnApi } from '#/api/erp/purchase/return';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { Button, Input, message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { getAccountSimpleList } from '#/api/erp/finance/account';
import { getReturnableItemsByInId } from '#/api/erp/purchase/in';
import {
  createPurchaseReturn,
  getPurchaseReturn,
  updatePurchaseReturn,
} from '#/api/erp/purchase/return';

import { useFormSchema } from '../data';
import ItemForm from './item-form.vue';
import PurchaseInSelectModal from './purchase-in-select.vue';
import PurchaseOrderSelect from './purchase-order-select.vue';

import { applyFieldConfig } from '#/views/erp/composables/useFieldConfig';

const emit = defineEmits(['success']);
const formData = ref<
  ErpPurchaseReturnApi.PurchaseReturn & {
    accountId?: number;
    discountPercent?: number;
    fileUrl?: string;
    order?: ErpPurchaseOrderApi.PurchaseOrder;
    orderId?: number;
    orderNo?: string;
    supplierId?: number;
  }
>({
  id: undefined,
  no: undefined,
  accountId: undefined,
  returnTime: undefined,
  remark: undefined,
  fileUrl: undefined,
  discountPercent: 0,
  supplierId: undefined,
  discountPrice: 0,
  totalPrice: 0,
  otherPrice: 0,
  returnMode: 10,
  items: [],
});
const formType = ref(''); // 表单类型：'create' | 'edit' | 'detail'
const itemFormRef = ref<InstanceType<typeof ItemForm>>();
const sourceInSelectVisible = ref(false); // 三期：原入库单选择弹窗

/* eslint-disable unicorn/no-nested-ternary */
const getTitle = computed(() =>
  formType.value === 'create'
    ? $t('ui.actionTitle.create', ['采购退货'])
    : formType.value === 'edit'
      ? $t('ui.actionTitle.edit', ['采购退货'])
      : '采购退货详情',
);

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
    if (formData.value) {
      if (changedFields.includes('otherPrice')) {
        formData.value.otherPrice = values.otherPrice;
      }
      if (changedFields.includes('discountPercent')) {
        formData.value.discountPercent = values.discountPercent;
      }
      // 三期：退货模式切换 → 清空子表（两种模式的子表语义不同）
      if (changedFields.includes('returnMode') && formType.value === 'create') {
        formData.value.returnMode = values.returnMode;
        formData.value.items = [];
        formData.value.orderId = undefined;
        formData.value.orderNo = undefined;
        formApi.setValues({ items: [], orderNo: undefined });
      }
    }
  },
});

/** 更新采购退货项 */
function handleUpdateItems(items: ErpPurchaseReturnApi.PurchaseReturnItem[]) {
  formData.value.items = items;
  formApi.setValues({
    items,
  });
}

/** 更新其他费用 */
function handleUpdateOtherPrice(otherPrice: number) {
  formApi.setValues({
    otherPrice,
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

/** 选择采购订单 */
function handleUpdateOrder(order: ErpPurchaseOrderApi.PurchaseOrder) {
  formData.value = {
    ...formData.value,
    orderId: order.id,
    orderNo: order.no!,
    supplierId: order.supplierId!,
    accountId: order.accountId!,
    remark: order.remark!,
    discountPercent: order.discountPercent!,
    fileUrl: order.fileUrl!,
  };
  // 将订单项设置到退货单项
  order.items!.forEach((item: any) => {
    item.totalCount = item.count;
    item.count = item.inCount - item.returnCount;
    item.orderItemId = item.id;
    item.id = undefined;
  });
  formData.value.items = order.items!.filter(
    (item) => item.count && item.count > 0,
  ) as ErpPurchaseReturnApi.PurchaseReturnItem[];
  formApi.setValues(formData.value, false);
}

/** 按单退货：选择原入库单后，把可退项带入子表 */
async function handleSelectSourceIn(purchaseIn: ErpPurchaseInApi.PurchaseIn) {
  if (!purchaseIn.id) return;
  const items = await getReturnableItemsByInId(purchaseIn.id);
  // 只保留可退数量 > 0 的行
  const usable = items.filter((it) => (it.returnableCount ?? 0) > 0);
  formData.value = {
    ...formData.value,
    supplierId: purchaseIn.supplierId,
    // orderId 从原入库单带出（可能为空）
    orderId: undefined,
    orderNo: undefined,
  };
  // 把可退项映射为退货单子表行
  formData.value.items = usable.map((it) => ({
    sourceInId: it.sourceInId,
    sourceInItemId: it.sourceInItemId,
    sourceInNo: it.sourceInNo,
    productId: it.productId,
    productUnitId: it.productUnitId,
    warehouseId: it.warehouseId,
    productPrice: it.productPrice,
    count: it.returnableCount, // 默认可退全部，用户可改
    inCount: it.inCount,
    returnableCount: it.returnableCount,
    taxPercent: it.taxPercent,
    packageQty: it.packageQty,
    wholeQty: it.wholeQty,
    warehousePosition: it.warehousePosition,
    drawingNo: it.drawingNo,
    batchNo: it.batchNo,
    barCode: it.barCode,
    brand: it.brand,
    vehicleModel: it.vehicleModel,
    originPlace: it.originPlace,
    businessEntity: it.businessEntity,
    remark: it.remark,
    // 初始化价格字段
    totalProductPrice: (it.productPrice ?? 0) * (it.returnableCount ?? 0),
    totalPrice: (it.productPrice ?? 0) * (it.returnableCount ?? 0),
  })) as any;
  formApi.setValues(
    { ...formData.value, sourceInNo: usable[0]?.sourceInNo },
    false,
  );
}

/** 创建或更新采购退货 */
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
      (await formApi.getValues()) as ErpPurchaseReturnApi.PurchaseReturn;
    try {
      await (formType.value === 'create'
        ? createPurchaseReturn(data)
        : updatePurchaseReturn(data));
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
      formData.value = {} as ErpPurchaseReturnApi.PurchaseReturn;
      return;
    }
    // 加载数据
    const data = modalApi.getData<{ id?: number; type: string }>();
    formType.value = data.type;
    formApi.setDisabled(formType.value === 'detail');
    const baseSchema = useFormSchema(formType.value);
    const configuredSchema = await applyFieldConfig(baseSchema, 'purchase_return');
    formApi.setState({ schema: configuredSchema });
    if (!data || !data.id) {
      // 新增时，默认选中账户
      const accountList = await getAccountSimpleList();
      const defaultAccount = accountList.find((item) => item.defaultStatus);
      if (defaultAccount) {
        await formApi.setValues({ accountId: defaultAccount.id });
      }
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getPurchaseReturn(data.id);
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
          :discount-percent="formData?.discountPercent ?? 0"
          :other-price="formData?.otherPrice ?? 0"
          @update:items="handleUpdateItems"
          @update:discount-price="handleUpdateDiscountPrice"
          @update:other-price="handleUpdateOtherPrice"
          @update:total-price="handleUpdateTotalPrice"
        />
      </template>
      <template #orderNo>
        <PurchaseOrderSelect
          :order-no="formData?.orderNo"
          @update:order="handleUpdateOrder"
        />
      </template>
      <template #sourceInNo>
        <div class="flex items-center gap-2">
          <Input
            :value="formData?.items?.[0]?.sourceInNo"
            placeholder="请选择原采购入库单"
            :disabled="true"
            readonly
          />
          <Button
            v-if="formData?.returnMode === 10 && formType !== 'detail'"
            type="primary"
            @click="sourceInSelectVisible = true"
          >
            选择
          </Button>
        </div>
        <PurchaseInSelectModal
          v-model:open="sourceInSelectVisible"
          @select="handleSelectSourceIn"
        />
      </template>
    </Form>
  </Modal>
</template>
