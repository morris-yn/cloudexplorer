import type { SimpleMap } from "@commons/api/base/type";

export interface FormView {
  /**
   *输入类型
   */
  inputType: string;

  attrs: string;
  /**
   * 字段名称
   */
  field: string;
  /**
   * 提示
   */
  label: string;

  leftLabel: boolean;

  hideLabel: boolean;
  /**
   * 值
   */
  value: unknown;
  /**
   * 是否必填
   */
  required: boolean;
  /**
   * 默认值
   */
  defaultValue: string;

  defaultJsonValue: boolean;
  /**
   *描述
   */
  description: string;
  /**
   * 值字段
   */
  valueField?: string;
  /**
   * 文本字段
   */
  textField?: string;
  /**
   * 格式化文本字段
   */
  formatTextField?: boolean;
  /**
   * 关联显示
   */
  relationShows: Array<string>;
  /**
   * 关联处理器
   */
  relationTrigger: Array<string>;
  /**
   * 处理器
   */
  clazz?: string | null;
  /**
   * 执行函数
   */
  method?: string | null;

  serviceMethod?: boolean;

  group?: number | null;

  step?: number | null;

  confirmGroup?: number | null;

  confirmSpecial?: boolean;

  confirmPosition?: string | null;

  confirmItemSpan?: number | null;

  regexp?: string | null;

  regexpDescription?: string | null;
  encrypted?: boolean;
  index: number;

  extraInfo?: string;
  hint?: string;
  style?: string;

  execModule?: string;
  /**
   * 其他字段
   */
  [propName: string]: any;
}

export interface GroupAnnotation {
  group: number;
  name: string;
  description: string;
  inline?: boolean;
}

export interface StepAnnotation {
  step: number;
  name: string;
  description: string;
}

export interface ConfirmAnnotation {
  group: number;
  name: string;
  description: string;
  items: number;
}

export interface FormViewObject {
  forms: Array<FormView>;
  groupAnnotationMap?: SimpleMap<GroupAnnotation>;
  stepAnnotationMap?: SimpleMap<StepAnnotation>;
  confirmAnnotationMap?: SimpleMap<ConfirmAnnotation>;
}
