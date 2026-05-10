<script lang="ts" setup>
import type { ErpSupplierApi } from '#/api/erp/purchase/supplier';

import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message, Tabs } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createSupplier,
  getSupplier,
  updateSupplier,
} from '#/api/erp/purchase/supplier';
import { $t } from '#/locales';

import {
  useAddressFormSchema,
  useBasicFormSchema,
  useCategoryFormSchema,
  useFinanceFormSchema,
  useInvoiceFormSchema,
  useSettleFormSchema,
} from '../data';

import { applyFieldConfig } from '#/views/erp/composables/useFieldConfig';

const emit = defineEmits(['success']);
const formData = ref<ErpSupplierApi.Supplier>();
const activeTab = ref('basic');
const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['供应商'])
    : $t('ui.actionTitle.create', ['供应商']);
});

const formConfig = {
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    labelWidth: 120,
  },
  wrapperClass: 'grid-cols-2',
  layout: 'horizontal' as const,
  showDefaultActions: false,
};

const [BasicForm, basicFormApi] = useVbenForm({
  ...formConfig,
  schema: useBasicFormSchema(),
});

const [CategoryForm, categoryFormApi] = useVbenForm({
  ...formConfig,
  schema: useCategoryFormSchema(),
});

const [SettleForm, settleFormApi] = useVbenForm({
  ...formConfig,
  schema: useSettleFormSchema(),
});

const [AddressForm, addressFormApi] = useVbenForm({
  ...formConfig,
  schema: useAddressFormSchema(),
});

const [InvoiceForm, invoiceFormApi] = useVbenForm({
  ...formConfig,
  schema: useInvoiceFormSchema(),
});

const [FinanceForm, financeFormApi] = useVbenForm({
  ...formConfig,
  schema: useFinanceFormSchema(),
});

const allFormApis = [
  basicFormApi,
  categoryFormApi,
  settleFormApi,
  addressFormApi,
  invoiceFormApi,
  financeFormApi,
];

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    // 验证基础信息表单（必填项在这里）
    const { valid } = await basicFormApi.validate();
    if (!valid) {
      activeTab.value = 'basic';
      return;
    }
    modalApi.lock();
    // 收集所有表单数据
    const allValues = await Promise.all(
      allFormApis.map((api) => api.getValues()),
    );
    const data = Object.assign(
      {},
      ...allValues,
    ) as ErpSupplierApi.Supplier;
    // 将 areaIds 数组拆分回 province/city/district
    if ((data as any).areaIds && (data as any).areaIds.length > 0) {
      (data as any).province = (data as any).areaIds[0] || null;
      (data as any).city = (data as any).areaIds[1] || null;
      (data as any).district = (data as any).areaIds[2] || null;
    }
    delete (data as any).areaIds;
    try {
      await (formData.value?.id ? updateSupplier(data) : createSupplier(data));
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
      activeTab.value = 'basic';
      return;
    }
    // 应用后端字段必填配置（六个 Tab 共用 supplier 模块）
    try {
      const [basicS, categoryS, settleS, addressS, invoiceS, financeS] =
        await Promise.all([
          applyFieldConfig(useBasicFormSchema(), 'supplier'),
          applyFieldConfig(useCategoryFormSchema(), 'supplier'),
          applyFieldConfig(useSettleFormSchema(), 'supplier'),
          applyFieldConfig(useAddressFormSchema(), 'supplier'),
          applyFieldConfig(useInvoiceFormSchema(), 'supplier'),
          applyFieldConfig(useFinanceFormSchema(), 'supplier'),
        ]);
      basicFormApi.setState({ schema: basicS });
      categoryFormApi.setState({ schema: categoryS });
      settleFormApi.setState({ schema: settleS });
      addressFormApi.setState({ schema: addressS });
      invoiceFormApi.setState({ schema: invoiceS });
      financeFormApi.setState({ schema: financeS });
    } catch {
      // 降级：沿用原 schema
    }
    // 加载数据
    const data = modalApi.getData<ErpSupplierApi.Supplier>();
    if (!data || !data.id) {
      return;
    }
    modalApi.lock();
    try {
      const supplierData = await getSupplier(data.id);
      formData.value = supplierData;
      // 等待表单组件完全挂载（force-render 后仍需等待内部初始化）
      await nextTick();
      await new Promise((resolve) => setTimeout(resolve, 100));
      // 逐个设置表单值，避免某个表单报错影响其他
      for (const api of allFormApis) {
        try {
          await api.setValues(formData.value!);
        } catch (e) {
          console.warn('设置表单值失败:', e);
        }
      }
      // 将 province/city/district 组装为 areaIds 供 Cascader 回显
      if (formData.value.province || formData.value.city || formData.value.district) {
        const areaIds = [formData.value.province, formData.value.city, formData.value.district].filter(Boolean);
        try {
          await addressFormApi.setFieldValue('areaIds', areaIds);
        } catch (e) {
          console.warn('设置地区值失败:', e);
        }
      }
    } catch (e) {
      console.error('加载供应商数据失败:', e);
      message.error('加载供应商数据失败');
    } finally {
      modalApi.unlock();
    }
  },
});

defineExpose({
  modalApi,
});
</script>

<template>
  <Modal :title="getTitle" class="w-[800px]">
    <Tabs v-model:activeKey="activeTab" class="mx-4">
      <Tabs.TabPane key="basic" tab="基础信息" force-render>
        <BasicForm />
      </Tabs.TabPane>
      <Tabs.TabPane key="category" tab="分类与采购" force-render>
        <CategoryForm />
      </Tabs.TabPane>
      <Tabs.TabPane key="settle" tab="结算与物流" force-render>
        <SettleForm />
      </Tabs.TabPane>
      <Tabs.TabPane key="address" tab="地址信息" force-render>
        <AddressForm />
      </Tabs.TabPane>
      <Tabs.TabPane key="invoice" tab="开票信息" force-render>
        <InvoiceForm />
      </Tabs.TabPane>
      <Tabs.TabPane key="finance" tab="财务信息" force-render>
        <FinanceForm />
      </Tabs.TabPane>
    </Tabs>
  </Modal>
</template>
