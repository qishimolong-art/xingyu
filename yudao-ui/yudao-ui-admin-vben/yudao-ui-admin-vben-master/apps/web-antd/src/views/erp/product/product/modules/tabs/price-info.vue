<script lang="ts" setup>
import { useVbenForm } from '#/adapter/form';

import { usePriceFormSchema } from '../../data';

const emit = defineEmits<{ (e: 'change', values: Record<string, any>): void }>();

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' } },
  wrapperClass: 'grid-cols-2',
  layout: 'horizontal',
  schema: usePriceFormSchema(),
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
</script>

<template>
  <Form class="mx-2" />
</template>
