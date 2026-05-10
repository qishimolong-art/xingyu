<script lang="ts" setup>
import type { ErpCustomerApi } from '#/api/erp/sale/customer';
import type { TableColumnsType } from 'ant-design-vue';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Button,
  Checkbox,
  Empty,
  Form as AForm,
  FormItem,
  Image,
  Input,
  InputNumber,
  message,
  Modal as AModal,
  Popconfirm,
  Spin,
  Table,
  Tabs,
  Tag,
} from 'ant-design-vue';

import {
  createCustomerArea,
  createCustomerBusinessInfo,
  createCustomerContact,
  createCustomerContract,
  createCustomerExtend,
  createCustomerImage,
  createCustomerTask,
  deleteCustomerArea,
  deleteCustomerBusinessInfo,
  deleteCustomerContact,
  deleteCustomerContract,
  deleteCustomerExtend,
  deleteCustomerImage,
  deleteCustomerTask,
  getCustomer,
  getCustomerAreaList,
  getCustomerBusinessInfoList,
  getCustomerContactList,
  getCustomerContractList,
  getCustomerExtendList,
  getCustomerImageList,
  getCustomerTaskList,
  updateCustomerArea,
  updateCustomerBusinessInfo,
  updateCustomerContact,
  updateCustomerContract,
  updateCustomerExtend,
  updateCustomerImage,
  updateCustomerTask,
} from '#/api/erp/sale/customer';

type EditorType =
  | 'area'
  | 'business'
  | 'contact'
  | 'contract'
  | 'extend'
  | 'image'
  | 'task';

const customer = ref<ErpCustomerApi.Customer>();
const contacts = ref<ErpCustomerApi.CustomerContact[]>([]);
const contracts = ref<ErpCustomerApi.CustomerContract[]>([]);
const images = ref<ErpCustomerApi.CustomerImage[]>([]);
const tasks = ref<ErpCustomerApi.CustomerTask[]>([]);
const areas = ref<ErpCustomerApi.CustomerArea[]>([]);
const extendsList = ref<ErpCustomerApi.CustomerExtend[]>([]);
const businessInfos = ref<ErpCustomerApi.CustomerBusinessInfo[]>([]);
const loading = ref(false);
const saving = ref(false);
const editorVisible = ref(false);
const editorType = ref<EditorType>('contact');
const editorRecord = ref<Record<string, any>>({});

const currentCustomerId = computed(() => customer.value?.id);
const title = computed(() =>
  customer.value?.name ? `客户详情 - ${customer.value.name}` : '客户详情',
);
const editorTitle = computed(() => {
  const action = editorRecord.value.id ? '编辑' : '新增';
  const names: Record<EditorType, string> = {
    area: '企业地区',
    business: '工商信息',
    contact: '联系人',
    contract: '客户合同',
    extend: '拓展信息',
    image: '企业图片',
    task: '任务量',
  };
  return `${action}${names[editorType.value]}`;
});

const actionColumn = { title: '操作', dataIndex: 'actions', fixed: 'right' as const, width: 120 };

const contactColumns: TableColumnsType<any> = [
  { title: '联系人', dataIndex: 'name', width: 120 },
  { title: '手机号', dataIndex: 'mobile', width: 130 },
  { title: '电话', dataIndex: 'telephone', width: 130 },
  { title: '职务', dataIndex: 'position', width: 120 },
  { title: '邮箱', dataIndex: 'email', width: 180 },
  { title: '地址', dataIndex: 'address', width: 220 },
  { title: '标记', dataIndex: 'flags', width: 260 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  actionColumn,
];
const contractColumns: TableColumnsType<any> = [
  { title: '合同编号', dataIndex: 'contractNo', width: 150 },
  { title: '合同类型', dataIndex: 'contractType', width: 120 },
  { title: '合同日期', dataIndex: 'contractDate', width: 170 },
  { title: '生效日期', dataIndex: 'startTime', width: 170 },
  { title: '终止日期', dataIndex: 'endTime', width: 170 },
  { title: '结算方式', dataIndex: 'settleMethod', width: 120 },
  { title: '运输方式', dataIndex: 'transportMethod', width: 120 },
  { title: '铺底金额', dataIndex: 'baseAmount', width: 120 },
  { title: '任务量', dataIndex: 'taskAmount', width: 120 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  actionColumn,
];
const taskColumns: TableColumnsType<any> = [
  { title: '年份', dataIndex: 'year', width: 100 },
  { title: '月份', dataIndex: 'month', width: 100 },
  { title: '任务级别', dataIndex: 'taskLevel', width: 140 },
  { title: '任务量', dataIndex: 'taskAmount', width: 140 },
  { title: '备注', dataIndex: 'remark', width: 220 },
  actionColumn,
];
const areaColumns: TableColumnsType<any> = [
  { title: '地图地址', dataIndex: 'mapAddress', width: 220 },
  { title: '详细地址', dataIndex: 'detailAddress', width: 240 },
  { title: '经度', dataIndex: 'longitude', width: 140 },
  { title: '纬度', dataIndex: 'latitude', width: 140 },
  { title: '默认', dataIndex: 'defaulted', width: 90 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  actionColumn,
];
const extendColumns: TableColumnsType<any> = [
  { title: '字段标识', dataIndex: 'extendKey', width: 150 },
  { title: '字段名称', dataIndex: 'extendName', width: 150 },
  { title: '字段值', dataIndex: 'extendValue', width: 240 },
  { title: '字段类型', dataIndex: 'extendType', width: 120 },
  { title: '排序', dataIndex: 'sort', width: 90 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  actionColumn,
];
const businessColumns: TableColumnsType<any> = [
  { title: '统一社会信用代码', dataIndex: 'creditCode', width: 190 },
  { title: '法人', dataIndex: 'legalPerson', width: 120 },
  { title: '注册资本', dataIndex: 'registeredCapital', width: 140 },
  { title: '成立日期', dataIndex: 'establishDate', width: 140 },
  { title: '经营状态', dataIndex: 'businessStatus', width: 120 },
  { title: '经营范围', dataIndex: 'businessScope', width: 260 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  actionColumn,
];

const [Modal, modalApi] = useVbenModal({
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      resetDetail();
      return;
    }
    const data = modalApi.getData<ErpCustomerApi.Customer>();
    if (data?.id) await loadDetail(data.id);
  },
});

function resetDetail() {
  customer.value = undefined;
  contacts.value = [];
  contracts.value = [];
  images.value = [];
  tasks.value = [];
  areas.value = [];
  extendsList.value = [];
  businessInfos.value = [];
  editorVisible.value = false;
}

async function loadDetail(customerId: number) {
  loading.value = true;
  modalApi.lock();
  try {
    const [
      customerData,
      contactList,
      contractList,
      imageList,
      taskList,
      areaList,
      extendList,
      businessList,
    ] = await Promise.all([
      getCustomer(customerId),
      getCustomerContactList(customerId),
      getCustomerContractList(customerId),
      getCustomerImageList(customerId),
      getCustomerTaskList(customerId),
      getCustomerAreaList(customerId),
      getCustomerExtendList(customerId),
      getCustomerBusinessInfoList(customerId),
    ]);
    customer.value = customerData;
    contacts.value = contactList;
    contracts.value = contractList;
    images.value = imageList;
    tasks.value = taskList;
    areas.value = areaList;
    extendsList.value = extendList;
    businessInfos.value = businessList;
  } finally {
    loading.value = false;
    modalApi.unlock();
  }
}

async function refreshCurrentDetail() {
  if (currentCustomerId.value) await loadDetail(currentCustomerId.value);
}

function valueOrDash(value?: number | string) {
  return value || '-';
}

function openEditor(type: EditorType, record?: Record<string, any>) {
  if (!currentCustomerId.value) return;
  editorType.value = type;
  editorRecord.value = {
    customerId: currentCustomerId.value,
    ...(type === 'extend' || type === 'image' ? { sort: 0 } : {}),
    ...(type === 'task' ? { year: new Date().getFullYear() } : {}),
    ...(record || {}),
  };
  editorVisible.value = true;
}

async function handleSaveEditor() {
  saving.value = true;
  try {
    const data = editorRecord.value as any;
    if (editorType.value === 'contact') {
      await (data.id ? updateCustomerContact(data) : createCustomerContact(data));
    } else if (editorType.value === 'contract') {
      await (data.id ? updateCustomerContract(data) : createCustomerContract(data));
    } else if (editorType.value === 'image') {
      await (data.id ? updateCustomerImage(data) : createCustomerImage(data));
    } else if (editorType.value === 'task') {
      await (data.id ? updateCustomerTask(data) : createCustomerTask(data));
    } else if (editorType.value === 'area') {
      await (data.id ? updateCustomerArea(data) : createCustomerArea(data));
    } else if (editorType.value === 'extend') {
      await (data.id ? updateCustomerExtend(data) : createCustomerExtend(data));
    } else {
      await (data.id ? updateCustomerBusinessInfo(data) : createCustomerBusinessInfo(data));
    }
    editorVisible.value = false;
    message.success('保存成功');
    await refreshCurrentDetail();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(type: EditorType, id?: number) {
  if (!id) return;
  if (type === 'contact') await deleteCustomerContact(id);
  else if (type === 'contract') await deleteCustomerContract(id);
  else if (type === 'image') await deleteCustomerImage(id);
  else if (type === 'task') await deleteCustomerTask(id);
  else if (type === 'area') await deleteCustomerArea(id);
  else if (type === 'extend') await deleteCustomerExtend(id);
  else await deleteCustomerBusinessInfo(id);
  message.success('删除成功');
  await refreshCurrentDetail();
}
</script>

<template>
  <Modal class="!w-[85vw]" :footer="false" :title="title">
    <Spin :spinning="loading">
      <Tabs>
        <Tabs.TabPane key="base" tab="基础信息">
          <div class="grid grid-cols-1 gap-3 md:grid-cols-3">
            <div class="detail-cell"><span>客户编码</span><strong>{{ valueOrDash(customer?.code) }}</strong></div>
            <div class="detail-cell"><span>客户名称</span><strong>{{ valueOrDash(customer?.name) }}</strong></div>
            <div class="detail-cell"><span>简称</span><strong>{{ valueOrDash(customer?.shortName) }}</strong></div>
            <div class="detail-cell"><span>联系人</span><strong>{{ valueOrDash(customer?.contact) }}</strong></div>
            <div class="detail-cell"><span>手机号</span><strong>{{ valueOrDash(customer?.mobile) }}</strong></div>
            <div class="detail-cell"><span>电话</span><strong>{{ valueOrDash(customer?.telephone) }}</strong></div>
            <div class="detail-cell"><span>邮箱</span><strong>{{ valueOrDash(customer?.email) }}</strong></div>
            <div class="detail-cell"><span>开户行</span><strong>{{ valueOrDash(customer?.bankName) }}</strong></div>
            <div class="detail-cell"><span>银行账号</span><strong>{{ valueOrDash(customer?.bankAccount) }}</strong></div>
            <div class="detail-cell md:col-span-3"><span>备注</span><strong>{{ valueOrDash(customer?.remark) }}</strong></div>
          </div>
        </Tabs.TabPane>

        <Tabs.TabPane key="contacts" tab="联系人">
          <div class="mb-3 flex justify-end"><Button type="primary" @click="openEditor('contact')">新增联系人</Button></div>
          <Table :columns="contactColumns" :data-source="contacts" :pagination="false" :row-key="(row) => row.id" :scroll="{ x: 'max-content', y: 'calc(70vh - 220px)' }" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'flags'">
                <div class="flex flex-wrap gap-1">
                  <Tag v-if="record.primaryContact" color="blue">主联系人</Tag>
                  <Tag v-if="record.receiverContact" color="green">收货</Tag>
                  <Tag v-if="record.settleContact" color="orange">结算</Tag>
                  <Tag v-if="record.messageContact" color="purple">消息</Tag>
                </div>
              </template>
              <template v-else-if="column.dataIndex === 'actions'">
                <Button size="small" type="link" @click="openEditor('contact', record)">编辑</Button>
                <Popconfirm title="确认删除该联系人？" @confirm="handleDelete('contact', record.id)">
                  <Button danger size="small" type="link">删除</Button>
                </Popconfirm>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="contracts" tab="客户合同">
          <div class="mb-3 flex justify-end"><Button type="primary" @click="openEditor('contract')">新增合同</Button></div>
          <Table :columns="contractColumns" :data-source="contracts" :pagination="false" :row-key="(row) => row.id" :scroll="{ x: 'max-content', y: 'calc(70vh - 220px)' }" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'actions'">
                <Button size="small" type="link" @click="openEditor('contract', record)">编辑</Button>
                <Popconfirm title="确认删除该合同？" @confirm="handleDelete('contract', record.id)">
                  <Button danger size="small" type="link">删除</Button>
                </Popconfirm>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="areas" tab="企业地区">
          <div class="mb-3 flex justify-end"><Button type="primary" @click="openEditor('area')">新增地区</Button></div>
          <Table :columns="areaColumns" :data-source="areas" :pagination="false" :row-key="(row) => row.id" :scroll="{ x: 'max-content', y: 'calc(70vh - 220px)' }" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'defaulted'">
                <Tag v-if="record.defaulted" color="blue">默认</Tag>
              </template>
              <template v-else-if="column.dataIndex === 'actions'">
                <Button size="small" type="link" @click="openEditor('area', record)">编辑</Button>
                <Popconfirm title="确认删除该地区？" @confirm="handleDelete('area', record.id)">
                  <Button danger size="small" type="link">删除</Button>
                </Popconfirm>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="images" tab="企业图片">
          <div class="mb-3 flex justify-end"><Button type="primary" @click="openEditor('image')">新增图片</Button></div>
          <div v-if="images.length" class="grid grid-cols-2 gap-4 md:grid-cols-4">
            <div v-for="image in images" :key="image.id" class="overflow-hidden rounded border border-gray-200">
              <Image :src="image.imageUrl" class="h-36 w-full object-cover" />
              <div class="px-3 py-2 text-sm">
                <div class="font-medium">{{ image.imageName || image.imageType || '图片' }}</div>
                <div class="mt-1 text-gray-500">{{ image.remark }}</div>
                <div class="mt-2 flex justify-end gap-2">
                  <Button size="small" type="link" @click="openEditor('image', image)">编辑</Button>
                  <Popconfirm title="确认删除该图片？" @confirm="handleDelete('image', image.id)">
                    <Button danger size="small" type="link">删除</Button>
                  </Popconfirm>
                </div>
              </div>
            </div>
          </div>
          <Empty v-else />
        </Tabs.TabPane>

        <Tabs.TabPane key="extends" tab="拓展信息">
          <div class="mb-3 flex justify-end"><Button type="primary" @click="openEditor('extend')">新增拓展信息</Button></div>
          <Table :columns="extendColumns" :data-source="extendsList" :pagination="false" :row-key="(row) => row.id" :scroll="{ x: 'max-content', y: 'calc(70vh - 220px)' }" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'actions'">
                <Button size="small" type="link" @click="openEditor('extend', record)">编辑</Button>
                <Popconfirm title="确认删除该拓展信息？" @confirm="handleDelete('extend', record.id)">
                  <Button danger size="small" type="link">删除</Button>
                </Popconfirm>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="tasks" tab="任务量">
          <div class="mb-3 flex justify-end"><Button type="primary" @click="openEditor('task')">新增任务量</Button></div>
          <Table :columns="taskColumns" :data-source="tasks" :pagination="false" :row-key="(row) => row.id" :scroll="{ x: 'max-content', y: 'calc(70vh - 220px)' }" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'actions'">
                <Button size="small" type="link" @click="openEditor('task', record)">编辑</Button>
                <Popconfirm title="确认删除该任务量？" @confirm="handleDelete('task', record.id)">
                  <Button danger size="small" type="link">删除</Button>
                </Popconfirm>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="business" tab="工商信息">
          <div class="mb-3 flex justify-end"><Button type="primary" @click="openEditor('business')">新增工商信息</Button></div>
          <Table :columns="businessColumns" :data-source="businessInfos" :pagination="false" :row-key="(row) => row.id" :scroll="{ x: 'max-content', y: 'calc(70vh - 220px)' }" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'actions'">
                <Button size="small" type="link" @click="openEditor('business', record)">编辑</Button>
                <Popconfirm title="确认删除该工商信息？" @confirm="handleDelete('business', record.id)">
                  <Button danger size="small" type="link">删除</Button>
                </Popconfirm>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>
      </Tabs>
    </Spin>

    <AModal v-model:open="editorVisible" :confirm-loading="saving" :title="editorTitle" destroy-on-close @ok="handleSaveEditor">
      <AForm :label-col="{ style: { width: '96px' } }" :model="editorRecord">
        <template v-if="editorType === 'contact'">
          <FormItem label="联系人" required><Input v-model:value="editorRecord.name" /></FormItem>
          <FormItem label="手机号"><Input v-model:value="editorRecord.mobile" /></FormItem>
          <FormItem label="电话"><Input v-model:value="editorRecord.telephone" /></FormItem>
          <FormItem label="职务"><Input v-model:value="editorRecord.position" /></FormItem>
          <FormItem label="邮箱"><Input v-model:value="editorRecord.email" /></FormItem>
          <FormItem label="地址"><Input v-model:value="editorRecord.address" /></FormItem>
          <FormItem label="标记">
            <div class="grid grid-cols-2 gap-2">
              <Checkbox v-model:checked="editorRecord.primaryContact">主联系人</Checkbox>
              <Checkbox v-model:checked="editorRecord.receiverContact">收货联系人</Checkbox>
              <Checkbox v-model:checked="editorRecord.settleContact">结算联系人</Checkbox>
              <Checkbox v-model:checked="editorRecord.messageContact">消息联系人</Checkbox>
            </div>
          </FormItem>
        </template>
        <template v-else-if="editorType === 'contract'">
          <FormItem label="合同编号"><Input v-model:value="editorRecord.contractNo" /></FormItem>
          <FormItem label="合同类型"><Input v-model:value="editorRecord.contractType" /></FormItem>
          <FormItem label="合同日期"><Input v-model:value="editorRecord.contractDate" placeholder="yyyy-MM-dd HH:mm:ss" /></FormItem>
          <FormItem label="生效日期"><Input v-model:value="editorRecord.startTime" placeholder="yyyy-MM-dd HH:mm:ss" /></FormItem>
          <FormItem label="终止日期"><Input v-model:value="editorRecord.endTime" placeholder="yyyy-MM-dd HH:mm:ss" /></FormItem>
          <FormItem label="结算方式"><Input v-model:value="editorRecord.settleMethod" /></FormItem>
          <FormItem label="运输方式"><Input v-model:value="editorRecord.transportMethod" /></FormItem>
          <FormItem label="铺底金额"><InputNumber v-model:value="editorRecord.baseAmount" class="w-full" :precision="2" /></FormItem>
          <FormItem label="任务量"><InputNumber v-model:value="editorRecord.taskAmount" class="w-full" :precision="2" /></FormItem>
        </template>
        <template v-else-if="editorType === 'area'">
          <FormItem label="地图地址"><Input v-model:value="editorRecord.mapAddress" /></FormItem>
          <FormItem label="详细地址"><Input v-model:value="editorRecord.detailAddress" /></FormItem>
          <FormItem label="经度"><InputNumber v-model:value="editorRecord.longitude" class="w-full" :precision="10" /></FormItem>
          <FormItem label="纬度"><InputNumber v-model:value="editorRecord.latitude" class="w-full" :precision="10" /></FormItem>
          <FormItem label="默认地址"><Checkbox v-model:checked="editorRecord.defaulted">设为默认</Checkbox></FormItem>
        </template>
        <template v-else-if="editorType === 'image'">
          <FormItem label="图片类型"><Input v-model:value="editorRecord.imageType" /></FormItem>
          <FormItem label="图片名称"><Input v-model:value="editorRecord.imageName" /></FormItem>
          <FormItem label="图片地址" required><Input v-model:value="editorRecord.imageUrl" /></FormItem>
          <FormItem label="排序"><InputNumber v-model:value="editorRecord.sort" class="w-full" :precision="0" /></FormItem>
          <FormItem label="默认图"><Checkbox v-model:checked="editorRecord.defaulted">设为默认</Checkbox></FormItem>
        </template>
        <template v-else-if="editorType === 'extend'">
          <FormItem label="字段标识" required><Input v-model:value="editorRecord.extendKey" /></FormItem>
          <FormItem label="字段名称"><Input v-model:value="editorRecord.extendName" /></FormItem>
          <FormItem label="字段值"><Input v-model:value="editorRecord.extendValue" /></FormItem>
          <FormItem label="字段类型"><Input v-model:value="editorRecord.extendType" /></FormItem>
          <FormItem label="排序"><InputNumber v-model:value="editorRecord.sort" class="w-full" :precision="0" /></FormItem>
        </template>
        <template v-else-if="editorType === 'business'">
          <FormItem label="信用代码"><Input v-model:value="editorRecord.creditCode" /></FormItem>
          <FormItem label="法人"><Input v-model:value="editorRecord.legalPerson" /></FormItem>
          <FormItem label="注册资本"><Input v-model:value="editorRecord.registeredCapital" /></FormItem>
          <FormItem label="成立日期"><Input v-model:value="editorRecord.establishDate" /></FormItem>
          <FormItem label="经营状态"><Input v-model:value="editorRecord.businessStatus" /></FormItem>
          <FormItem label="经营范围"><Input.TextArea v-model:value="editorRecord.businessScope" :rows="3" /></FormItem>
        </template>
        <template v-else>
          <FormItem label="年份" required><InputNumber v-model:value="editorRecord.year" class="w-full" :precision="0" /></FormItem>
          <FormItem label="月份"><InputNumber v-model:value="editorRecord.month" class="w-full" :max="12" :min="1" :precision="0" /></FormItem>
          <FormItem label="任务级别"><Input v-model:value="editorRecord.taskLevel" /></FormItem>
          <FormItem label="任务量"><InputNumber v-model:value="editorRecord.taskAmount" class="w-full" :precision="2" /></FormItem>
        </template>
        <FormItem label="备注"><Input.TextArea v-model:value="editorRecord.remark" :rows="3" /></FormItem>
      </AForm>
    </AModal>
  </Modal>
</template>

<style scoped>
.detail-cell {
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  padding: 10px 12px;
}

.detail-cell span {
  color: hsl(var(--muted-foreground));
  display: block;
  font-size: 12px;
  line-height: 18px;
}

.detail-cell strong {
  display: block;
  font-size: 14px;
  font-weight: 500;
  line-height: 24px;
  min-height: 24px;
  word-break: break-all;
}
</style>
