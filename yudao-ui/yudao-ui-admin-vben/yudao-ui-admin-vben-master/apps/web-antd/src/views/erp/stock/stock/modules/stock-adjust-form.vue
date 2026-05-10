<script lang="ts" setup>
import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { adjustStock, type ErpStockApi } from '#/api/erp/stock/stock';

const emit = defineEmits(['success']);

interface AdjustModalData {
  productId: number;
  warehouseId: number;
  productCode?: string;
  productName?: string;
  warehouseName?: string;
  count?: number;
}

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    labelWidth: 120,
  },
  layout: 'horizontal',
  showDefaultActions: false,
  schema: [
    {
      fieldName: 'productId',
      label: 'productId',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'warehouseId',
      label: 'warehouseId',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'productName',
      label: '产品',
      component: 'Input',
      componentProps: { disabled: true },
      formItemClass: 'col-span-2',
    },
    {
      fieldName: 'warehouseName',
      label: '仓库',
      component: 'Input',
      componentProps: { disabled: true },
    },
    {
      fieldName: 'currentCount',
      label: '当前库存数',
      component: 'Input',
      componentProps: { disabled: true },
    },
    {
      fieldName: 'targetCount',
      label: '调整后库存数',
      component: 'InputNumber',
      rules: 'required',
      componentProps: {
        precision: 3,
        min: 0,
        class: 'w-full',
        placeholder: '请输入调整后的库存数量',
      },
    },
    {
      fieldName: 'reason',
      label: '调整原因',
      component: 'Select',
      componentProps: {
        allowClear: true,
        placeholder: '请选择调整原因',
        options: [
          { label: '盘点调整', value: '盘点调整' },
          { label: '报损', value: '报损' },
          { label: '报溢', value: '报溢' },
          { label: '其他', value: '其他' },
        ],
      },
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: {
        rows: 3,
        placeholder: '请输入备注',
      },
      formItemClass: 'col-span-2',
    },
  ],
  wrapperClass: 'grid-cols-2',
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = (await formApi.getValues()) as {
      productId: number | string;
      warehouseId: number | string;
      targetCount: number | string;
      reason?: string;
      remark?: string;
    };
    modalApi.lock();
    try {
      const payload: ErpStockApi.StockAdjustReqVO = {
        productId: Number(values.productId),
        warehouseId: Number(values.warehouseId),
        targetCount: Number(values.targetCount),
        reason: values.reason,
        remark: values.remark,
      };
      await adjustStock(payload);
      await modalApi.close();
      emit('success');
      message.success('调整成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      return;
    }
    const data = modalApi.getData<AdjustModalData>();
    if (!data) {
      return;
    }
    const productLabel = [data.productCode, data.productName]
      .filter(Boolean)
      .join(' ');
    await formApi.setValues({
      productId: data.productId,
      warehouseId: data.warehouseId,
      productName: productLabel || '-',
      warehouseName: data.warehouseName ?? '-',
      currentCount: data.count ?? 0,
      targetCount: data.count ?? 0,
      reason: '盘点调整',
      remark: undefined,
    });
  },
});
</script>

<template>
  <Modal title="编辑库存数量" class="w-[600px]">
    <Form class="mx-4" />
  </Modal>
</template>
