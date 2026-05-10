<script lang="ts" setup>
import type { ErpProductApi } from '#/api/erp/product/product';

import { useVbenForm } from '#/adapter/form';

import { useBaseFormSchema } from '../../data';

const props = defineProps<{ value?: ErpProductApi.Product }>();
const emit = defineEmits<{ (e: 'change', values: Record<string, any>): void }>();

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' } },
  wrapperClass: 'grid-cols-2',
  layout: 'horizontal',
  schema: useBaseFormSchema(),
  showDefaultActions: false,
  handleValuesChange(values) {
    emit('change', values);
  },
});

defineExpose({
  async validate() {
    const { valid } = await formApi.validate();
    return valid;
  },
  getValues: () => formApi.getValues(),
  setValues: (data: Record<string, any>) => formApi.setValues(data),
});

if (props.value) {
  formApi.setValues(props.value);
}
</script>

<template>
  <Form class="mx-2" />
</template>
