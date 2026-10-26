# 浏览器内置打印字体

TTF 是 PDF 的原始资源，禁止在转换时覆盖。WOFF2 与清单预先生成并提交，Maven 只打包文件，线上无需 Python 或字体工具。

在 code 目录执行：

```powershell
python -m venv target/print-font-tools
target/print-font-tools/Scripts/python.exe -m pip install -r scripts/print-fonts/requirements.txt
target/print-font-tools/Scripts/python.exe scripts/print-fonts/generate.py
target/print-font-tools/Scripts/python.exe scripts/print-fonts/generate.py --check
```

固定 FontTools 4.60.1、Brotli 1.1.0、quality=9，关闭可选 glyf/loca 预转换，保留完整字符集和原始字形/提示指令。生成时逐文件校验原 TTF SHA-256、全部字形轮廓/提示、字符映射、字宽及布局表；清单从 Java 原字体注册表映射生成，不维护第二套手工映射。

只有字体来源或工具参数改变时才重新生成。生成的内容哈希决定浏览器 URL，更新字体必须同时更新全部资源、manifest.json 和许可证清单。后端先发布，前端随后发布；回滚前端后再回滚后端。旧 `/font/file` 接口继续提供 TTF。
