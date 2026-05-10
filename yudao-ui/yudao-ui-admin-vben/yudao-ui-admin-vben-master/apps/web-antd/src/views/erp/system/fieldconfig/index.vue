<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Alert,
  Button,
  Empty,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Switch,
  Table,
  Tabs,
} from 'ant-design-vue';

import {
  batchUpdateFieldConfig,
  type ErpFieldConfigApi,
  getFieldConfigList,
  resetFieldConfig,
} from '#/api/erp/config/field';
import { clearFieldConfigCache } from '#/views/erp/composables/useFieldConfig';

import { useModuleTabs } from './data';

defineOptions({ name: 'ErpFieldConfig' });

const moduleTabs = useModuleTabs();
const currentKey = ref(moduleTabs[0]!.key);
const tableData = ref<ErpFieldConfigApi.FieldConfig[]>([]);
const dirty = ref(false);
const loading = ref(false);
const saving = ref(false);

const currentModuleName = computed(
  () => moduleTabs.find((t) => t.key === currentKey.value)?.label ?? '',
);

const tableColumns: TableColumnsType = [
  {
    title: '序号',
    key: 'index',
    width: 60,
    customRender: ({ index }) => index + 1,
  },
  {
    title: '字段名',
    dataIndex: 'fieldName',
    key: 'fieldName',
    width: 220,
  },
  {
    title: '字段中文名',
    dataIndex: 'fieldLabel',
    key: 'fieldLabel',
    width: 220,
  },
  {
    title: '是否必填',
    dataIndex: 'required',
    key: 'required',
    width: 120,
    align: 'center',
  },
  {
    title: '排序',
    dataIndex: 'sort',
    key: 'sort',
    width: 140,
    align: 'center',
  },
];

async function loadData(key: string) {
  loading.value = true;
  try {
    const list = await getFieldConfigList(key);
    tableData.value = (list ?? []).map((row) => ({
      ...row,
      required: Boolean(row.required),
      sort: row.sort ?? 0,
      fieldLabel: row.fieldLabel ?? '',
    }));
    dirty.value = false;
  } finally {
    loading.value = false;
  }
}

function handleTabChange(key: string) {
  if (key === currentKey.value) return;
  if (dirty.value) {
    Modal.confirm({
      title: '有未保存的改动',
      content: '切换后改动会丢失，确认切换吗？',
      okText: '确认切换',
      cancelText: '取消',
      onOk: async () => {
        currentKey.value = key;
        await loadData(key);
      },
    });
  } else {
    currentKey.value = key;
    loadData(key);
  }
}

async function handleSave() {
  saving.value = true;
  try {
    await batchUpdateFieldConfig({
      moduleKey: currentKey.value,
      items: tableData.value.map((row) => ({
        fieldName: row.fieldName,
        fieldLabel: row.fieldLabel,
        required: row.required,
        sort: row.sort,
      })),
    });
    clearFieldConfigCache(currentKey.value);
    message.success('保存成功');
    await loadData(currentKey.value);
  } finally {
    saving.value = false;
  }
}

async function handleReset() {
  await resetFieldConfig(currentKey.value);
  clearFieldConfigCache(currentKey.value);
  message.success('已清空自定义配置，表单将按代码默认规则渲染');
  await loadData(currentKey.value);
}

function markDirty() {
  dirty.value = true;
}

onMounted(() => loadData(currentKey.value));
</script>

<template>
  <Page auto-content-height>
    <div class="bg-white p-4">
      <Alert
        class="mb-3"
        type="info"
        show-icon
        message="说明：这里关闭必填只会移除红星和空值校验；字段本身有格式校验（如数字、邮箱等）时仍会保留，由业务规则决定。后端硬约束不受影响。"
      />
      <div class="mb-4 flex items-center justify-between">
        <Tabs
          :active-key="currentKey"
          size="large"
          class="flex-1"
          @change="(key: string | number) => handleTabChange(String(key))"
        >
          <Tabs.TabPane
            v-for="tab in moduleTabs"
            :key="tab.key"
            :tab="tab.label"
          />
        </Tabs>
        <div class="ml-4 space-x-2">
          <Popconfirm
            :title="`确定清空 ${currentModuleName} 的自定义配置吗？（清空后表单将按代码默认规则渲染）`"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleReset"
          >
            <Button>清空自定义</Button>
          </Popconfirm>
          <Button type="primary" :loading="saving" @click="handleSave">
            保存
          </Button>
        </div>
      </div>

      <Empty
        v-if="!loading && tableData.length === 0"
        description="该模块尚无自定义配置，前端将按代码默认规则渲染"
      />

      <Table
        v-else
        :data-source="tableData"
        :columns="tableColumns"
        :pagination="false"
        :loading="loading"
        row-key="fieldName"
        size="middle"
        bordered
        :scroll="{ y: 'calc(100vh - 320px)' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'fieldLabel'">
            <Input
              v-model:value="record.fieldLabel"
              placeholder="请输入字段中文名"
              allow-clear
              @change="markDirty"
            />
          </template>
          <template v-else-if="column.key === 'required'">
            <Switch
              v-model:checked="record.required"
              checked-children="必填"
              un-checked-children="可空"
              @change="markDirty"
            />
          </template>
          <template v-else-if="column.key === 'sort'">
            <InputNumber
              v-model:value="record.sort"
              :min="0"
              style="width: 100%"
              @change="markDirty"
            />
          </template>
        </template>
      </Table>
    </div>
  </Page>
</template>
