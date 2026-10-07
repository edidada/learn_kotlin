// v3 supplemental pass: extension PROPERTY declarations (val/var on a receiver),
// which the main parser recorded under the receiver type name.
// Output: out3/ext_props.tsv  cols: since \t pkg \t kind \t name \t receiver \t sig \t loc
import fs from 'node:fs';
import path from 'node:path';

const ROOT = process.argv[2] || '.';              // 解包后的 sources.jar 根目录（含 commonMain/ 与 jvmMain/）
const SRC = path.resolve(process.argv[3] || path.join(ROOT, 'src'));
const OUT = path.resolve(process.argv[4] || path.join(ROOT, 'out3'));
fs.mkdirSync(OUT, { recursive: true });

function walk(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(e => {
    const fp = path.join(dir, e.name);
    return e.isDirectory() ? walk(fp) : e.name.endsWith('.kt') ? [fp] : [];
  });
}

// public [actual|expect|inline] val|var  Receiver.Type.name : Type
const DECL = /^\s*(?:public|@JvmName|@SinceKotlin|protected)\s*.*?\b(?:public\s+)?(?:actual\s+|expect\s+|inline\s+|external\s+)*(val|var)\s+([A-Za-z_][\w<>.,\s?]*?)\.([A-Za-z_]\w*)\s*[:=]/;
const SINCE = /@SinceKotlin\(\s*"([\d.]+)"/;

const rows = [];
for (const fp of walk(SRC)) {
  const lines = fs.readFileSync(fp, 'utf8').split('\n');
  let pkg = '';
  let inBlock = false;
  let pendingSince = null;
  for (let i = 0; i < lines.length; i++) {
    const L = lines[i];
    if (i === 0 || /^package\s/.test(L)) { const m = L.match(/^package\s+([\w.]+)/); if (m) pkg = m[1]; }
    // block comment state (must be handled BEFORE kdoc tests)
    if (inBlock) { if (L.includes('*/')) { inBlock = false; } continue; }
    if (L.includes('/*')) { inBlock = !L.includes('*/'); continue; }
    const s = L.match(SINCE);
    if (s) { pendingSince = { v: s[1], line: i }; continue; }
    if (/^\s*@\w/.test(L)) continue;           // other annotations
    if (/^\s*(\/\/|$)/.test(L)) { if (pendingSince && i - pendingSince.line > 6) pendingSince = null; continue; }

    const m = L.match(DECL);
    if (!m) { if (pendingSince && i - pendingSince.line > 6) pendingSince = null; continue; }
    const [, kind, recvRaw, name] = m;
    const recv = recvRaw.trim().replace(/\s+/g, '');
    // a declaration line with no annotation within 6 lines => 1.0 (undated)
    const since = pendingSince && i - pendingSince.line <= 6 ? pendingSince.v : '-';
    // collect @WasExperimental / @Deprecated nearby for context
    let flags = [];
    for (let j = Math.max(0, i - 8); j < i; j++) {
      const w = lines[j].match(/@WasExperimental\(([\w.]+)/); if (w) flags.push(`WasExperimental(${w[1]})`);
      if (/@Deprecated\b/.test(lines[j])) flags.push('Deprecated');
      const r = lines[j].match(/@RequiresOptIn\b/); if (r) flags.push('RequiresOptIn');
    }
    rows.push([since, pkg, kind, name, recv, L.trim().slice(0, 200), `${path.relative(SRC, fp)}:${i + 1}`, ...flags]);
    pendingSince = null;
  }
}
const header = 'since\tpkg\tkind\tname\treceiver\tsig\tloc\tflags';
fs.writeFileSync(path.join(OUT, 'ext_props.tsv'), header + '\n' + rows.map(r => r.join('\t')).join('\n') + '\n');
console.log(`rows=${rows.length}`);
const byPkg = {};
for (const r of rows) byPkg[r[1]] = (byPkg[r[1]] || 0) + 1;
console.log(Object.entries(byPkg).sort((a, b) => b[1] - a[1]).map(([k, v]) => `${k}=${v}`).join(' | '));
const since = {};
for (const r of rows) since[r[0]] = (since[r[0]] || 0) + 1;
console.log('since:', Object.entries(since).sort().map(([k, v]) => `${k}=${v}`).join(' '));
