"""Generate/check bundled web fonts. Never runs during Maven build or server startup."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re

import brotli
import fontTools
from fontTools.ttLib import TTFont, woff2

ROOT = Path(__file__).resolve().parents[2]
FONT_ROOT = ROOT / 'yudao-module-erp/src/main/resources/fonts/cloud-print'
REGISTRY = ROOT / ('yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/'
                   'service/cloudprint/ErpCloudPrintFontRegistry.java')


def digest(data):
    return hashlib.sha256(data).hexdigest()


def check_equivalence(source, compressed):
    original = TTFont(source, recalcTimestamp=False, recalcBBoxes=False)
    restored = TTFont(io.BytesIO(compressed), recalcTimestamp=False, recalcBBoxes=False)
    assert original.getGlyphOrder() == restored.getGlyphOrder(), source
    assert original.getBestCmap() == restored.getBestCmap(), source
    for tag in ('cmap', 'hmtx', 'vmtx', 'hhea', 'vhea', 'GSUB', 'GPOS', 'GDEF', 'OS/2', 'kern'):
        if tag in original:
            assert original.getTableData(tag) == restored.getTableData(tag), (source, tag)
    for name in original.getGlyphOrder():
        before, after = original['glyf'][name], restored['glyf'][name]
        a = before.getCoordinates(original['glyf'])
        b = after.getCoordinates(restored['glyf'])
        assert a == b, (source, name, 'outline')
        if hasattr(before, 'program'):
            assert before.program.getBytecode() == after.program.getBytecode(), (source, name, 'hinting')
    original.close()
    restored.close()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    assert fontTools.__version__ == '4.60.1', 'Use requirements.txt'
    assert brotli.__version__ == '1.1.0', 'Use requirements.txt'
    # Explicit quality keeps generation repeatable and avoids Brotli's very slow default 11.
    compress = woff2.brotli.compress
    def compress_web(data, **options):
        options['quality'] = 9
        return compress(data, **options)
    woff2.brotli.compress = compress_web
    pairs = re.findall(r'families.put\("([^"]+)", new FontFamily\("[^"]+",\s*'
                       r'resource\("([^"]+)"\), resource\("([^"]+)"\)\)\);',
                       REGISTRY.read_text(encoding='utf-8'))
    assert len(pairs) == 5, 'Registry mapping changed: update generator'
    pinned = dict((name, sha) for sha, name in re.findall(
        r'([0-9a-f]{64})\s+(\S+\.ttf)',
        (FONT_ROOT / 'THIRD-PARTY-FONTS.md').read_text(encoding='utf-8')))
    web_root = FONT_ROOT / 'web'
    if not args.check:
        web_root.mkdir(exist_ok=True)
    resources, source_ids = {}, {}
    for name in sorted({name for _, regular, bold in pairs for name in (regular, bold)}):
        source = FONT_ROOT / name
        assert digest(source.read_bytes()) == pinned[name], ('Source font changed', name)
        output = web_root / name.replace('.ttf', '.woff2')
        if not args.check:
            print(f'Compressing {name}', flush=True)
            woff2.compress(source, output, transform_tables=set())
        print(f'Checking {name}', flush=True)
        compressed = output.read_bytes()
        check_equivalence(source, compressed)
        resource_id = digest(compressed)
        source_ids[name] = resource_id
        resources[resource_id] = {
            'sha256': resource_id, 'fileName': output.name, 'size': len(compressed),
            'source': name, 'sourceSha256': pinned[name],
            'url': '/erp/print/font/resource/' + resource_id + '.woff2',
        }
        print(f'{name}: {source.stat().st_size} -> {len(compressed)}; glyph/layout verified', flush=True)
    families = {key: {'400': source_ids[regular], '700': source_ids[bold]}
                for key, regular, bold in pairs}
    manifest = {'tools': {'fonttools': fontTools.__version__, 'Brotli': brotli.__version__, 'quality': '9', 'transformTables': 'none'},
                'families': families, 'fallback': dict(families['microsoft_yahei']),
                'resources': resources}
    manifest['version'] = digest(json.dumps(manifest, sort_keys=True).encode())
    manifest_path = web_root / 'manifest.json'
    if args.check:
        assert manifest == json.loads(manifest_path.read_text(encoding='utf-8')), 'Manifest out of date'
    else:
        manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('OK: 9 original hashes, all glyphs, layout, widths, manifest', flush=True)


if __name__ == '__main__':
    main()
