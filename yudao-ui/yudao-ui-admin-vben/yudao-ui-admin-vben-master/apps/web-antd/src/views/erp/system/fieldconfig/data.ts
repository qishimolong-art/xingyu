/** 模块 Tab 定义 */
export function useModuleTabs() {
  return [
    { key: 'purchase_order', label: '采购订单' },
    { key: 'purchase_in', label: '采购入库' },
    { key: 'purchase_return', label: '采购退货' },
    { key: 'supplier', label: '供应商' },
  ];
}
