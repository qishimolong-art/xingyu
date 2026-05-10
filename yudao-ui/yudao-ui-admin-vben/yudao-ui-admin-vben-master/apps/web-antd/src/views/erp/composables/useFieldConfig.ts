import type { VbenFormSchema } from '#/adapter/form';

import {
  type ErpFieldConfigApi,
  getFieldConfigList,
} from '#/api/erp/config/field';

/**
 * 字段配置缓存：TTL 5 分钟。
 * - 仅降低"短时间内多次打开同一表单"的请求次数
 * - 跨浏览器/跨用户的变更通过 TTL 自然失效，避免改了管理页另一台机器一直看旧数据
 * - 管理页保存/重置后会主动 clearFieldConfigCache(moduleKey) 立即失效
 */
const TTL_MS = 5 * 60 * 1000;
const cache = new Map<
  string,
  { t: number; p: Promise<ErpFieldConfigApi.FieldConfig[]> }
>();

/** 清除字段配置缓存，不传参则清除全部 */
export function clearFieldConfigCache(moduleKey?: string) {
  if (moduleKey) {
    cache.delete(moduleKey);
  } else {
    cache.clear();
  }
}

async function loadConfig(moduleKey: string) {
  const now = Date.now();
  const hit = cache.get(moduleKey);
  if (hit && now - hit.t < TTL_MS) {
    return hit.p;
  }
  const p = getFieldConfigList(moduleKey).catch((err) => {
    // 请求失败时清掉本次缓存，避免卡死
    const current = cache.get(moduleKey);
    if (current && current.p === p) {
      cache.delete(moduleKey);
    }
    throw err;
  });
  cache.set(moduleKey, { t: now, p });
  return p;
}

/**
 * 判断一个 rules 值是否"看起来代表必填"。
 * - 字符串形态：'required' 或以 Required 结尾的预设字符串（如 'selectRequired'）
 * - 对象形态：{ required: true, ... }
 * 返回 true 时表示这是一条"前端必填"规则，关闭必填时可安全删除。
 * 返回 false 时（如 zod 规则链 z.string().min(1)），可能是业务硬约束，保留不动。
 */
function isRequiredRule(rules: unknown): boolean {
  if (rules == null) return false;
  if (typeof rules === 'string') {
    return rules === 'required' || /Required$/i.test(rules);
  }
  if (typeof rules === 'object' && 'required' in (rules as object)) {
    return Boolean((rules as { required?: unknown }).required);
  }
  return false;
}

/**
 * 把字段配置应用到 schema 上：
 * - config.required===true → schema.rules = 'required'
 * - config.required===false → 若原 rules 明显是必填（字符串 'required' / xxxRequired / { required: true }）
 *   则移除；zod 规则链等可能承载业务硬约束的配置保留不动
 * - config.required 为其他值（null/undefined）：不处理
 * - schema 里不在 config 中的字段：维持原 rules 不变
 *
 * 后端接口不可用时降级为原 schema，保证页面仍可正常使用。
 */
export async function applyFieldConfig(
  schema: VbenFormSchema[],
  moduleKey: string,
): Promise<VbenFormSchema[]> {
  let config: ErpFieldConfigApi.FieldConfig[] = [];
  try {
    config = await loadConfig(moduleKey);
  } catch {
    return schema;
  }
  const configMap = new Map(config.map((c) => [c.fieldName, c]));
  return schema.map((item) => {
    if (!item.fieldName) {
      return item;
    }
    const cfg = configMap.get(item.fieldName);
    if (!cfg) {
      return item;
    }
    const newItem = { ...item };
    if (cfg.required === true) {
      newItem.rules = 'required';
    } else if (cfg.required === false && isRequiredRule(newItem.rules)) {
      delete newItem.rules;
    }
    return newItem;
  });
}
