# ERP 内置打印字体清单

字体仅用于 ERP 打印模板、浏览器本地打印和后端 PDF 云打印，均按 SIL Open Font License 1.1 分发。

## 固定版本

- Noto Sans SC / Noto Serif SC：Google Fonts 仓库提交 `2eb0b48d5f760f62e286216f0859a8c540dbc1bd`，由官方可变 TTF 固定为静态字重。
- LXGW WenKai / LXGW WenKai GB：官方 `v1.522` 发布版静态 TTF。

## 模板字体映射

| 模板键 | 中文名称 | 400 | 700 |
| --- | --- | --- | --- |
| `simsun` | 宋体 | NotoSerifSC-Regular.ttf | NotoSerifSC-Bold.ttf |
| `simhei` | 黑体 | NotoSansSC-Medium.ttf | NotoSansSC-Bold.ttf |
| `microsoft_yahei` | 微软雅黑 | NotoSansSC-Regular.ttf | NotoSansSC-Medium.ttf |
| `kaiti` | 楷体 | LXGWWenKaiGB-Regular.ttf | LXGWWenKaiGB-Medium.ttf |
| `fangsong` | 仿宋 | LXGWWenKai-Light.ttf | LXGWWenKai-Regular.ttf |

默认及缺字回退为 Noto Sans SC Regular/Bold。

## SHA-256

```text
526ec70cbb0118e871d481f8179e03ff045f0e4d72d080dcca87950c4ab27cca  LXGWWenKai-Light.ttf
39ad71264b588165b469e35e6afb162a378dacd1f95348160240ba9038ac3009  LXGWWenKai-Regular.ttf
b885c51ec0d3f325974013801dfcefda1a9ba0bf385c607cf5f2582dafa2e5ab  LXGWWenKaiGB-Medium.ttf
295568c131648062107543aa159c97dd49564be791136c2abf74cad83eba3f7f  LXGWWenKaiGB-Regular.ttf
9db05b8ad1cdd8f3408c994e2bd0b89d75e59cce9e731dc1d80a9bd6a8e71a66  NotoSansSC-Bold.ttf
f073bb2c48e94be777e0ff18fabfb4e37e747df7394b294cb3206ae31edc1c5d  NotoSansSC-Regular.ttf
73bf4d2027a3e58f35509d5d1fda30738176618aa79cc83c4e0c8d2b09894178  NotoSansSC-Medium.ttf
7757703c59eae2ab6b40b857d3747b4b48b9b5ca435e20dcb21651cdcbce9acb  NotoSerifSC-Bold.ttf
43eecf78d8453095c2afacc7adee52fdb44c9082f6b64124be7dfedde48f855a  NotoSerifSC-Regular.ttf
```

许可证原文位于 `licenses/`。来源：

- https://github.com/notofonts/noto-cjk
- https://github.com/google/fonts
- https://github.com/lxgw/LxgwWenKai
- https://github.com/lxgw/LxgwWenKaiGB
