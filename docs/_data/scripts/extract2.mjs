import { readdirSync, statSync, readFileSync, writeFileSync, mkdirSync } from 'fs';
import { join } from 'path';

const ROOT = process.argv[2];
const OUT = process.argv[3];
mkdirSync(OUT, { recursive: true });

function walk(dir, acc = []) {
  for (const e of readdirSync(dir)) {
    const p = join(dir, e);
    const st = statSync(p);
    if (st.isDirectory()) walk(p, acc);
    else if (e.endsWith('.kt')) acc.push(p);
  }
  return acc;
}

const MOD = '(?:private|internal|public|protected|expect|actual|final|open|abstract|sealed|data|value|inline|operator|infix|external|suspend|tailrec|const|annotation|enum|companion)';
const DECL = new RegExp('^[ \\t]*(?:@[A-Za-z_][\\w.]*(?:\\([^)]*\\))?[ \\t]*)*(?:' + MOD + '[ \\t]+)*(?:fun[ \\t<(]|fun\\b|val[ \\t]|var[ \\t]|class[ \\t<]|interface[ \\t<]|object[ \\t:]|typealias[ \\t])');

function parseName(kind, after) {
  let h = after;
  const cuts = ['=', '{'].map(c => { const i = h.indexOf(c); return i < 0 ? Infinity : i; });
  const p = h.indexOf('(');
  if (kind === 'fun' && p >= 0) cuts.push(p);
  const stop = Math.min(...cuts);
  if (stop < Infinity) h = h.slice(0, stop);
  if (/^(class|interface|object|typealias)$/.test(kind)) {
    // declaration name is the FIRST identifier after the keyword
    const t = h.trim().split(/\s+/)[0] || '';
    h = t.replace(/<.*$/, '').replace(/[:({].*$/, '').replace(/;.*$/, '');
  }
  h = h.replace(/<[^<>]*>/g, ' ').replace(/[()]/g, ' ');
  const toks = h.trim().split(/\s+/).filter(Boolean);
  let last = toks.length ? toks[toks.length - 1] : '';
  last = last.replace(/[^\w.`]/g, '');
  const segs = last.split('.').filter(Boolean);
  const receiver = segs.length > 1 ? segs.slice(0, -1).join('.') : '';
  const name = (segs.length ? segs[segs.length - 1] : '').replace(/[^\w]/g, '');
  return { name, receiver };
}

const rows = [];
const files = walk(ROOT);
for (const f of files) {
  const lines = readFileSync(f, 'utf8').split('\n');
  let pkg = '', since = '', sinceLine = -1, inBlock = false;
  const rel = f.replace(ROOT + '/', '').replace(/\\/g, '/');
  for (let i = 0; i < lines.length; i++) {
    const raw = lines[i];
    // block-comment state first: license headers end with " */" which also looks like a kdoc line
    if (/\/\*/.test(raw) && !/\*\//.test(raw)) inBlock = true;
    if (inBlock) { if (/\*\//.test(raw)) inBlock = false; continue; }
    if (/^[ \t]*\*/.test(raw)) continue;
    const line = raw.replace(/\/\/.*$/, '');
    if (!line.trim()) continue;

    const pm = line.match(/^package[ \t]+([\w.]+)/);
    if (pm) { pkg = pm[1]; continue; }

    let ver = '';
    const sm = line.match(/@SinceKotlin\(\s*"([\d.]+)"\s*\)/);
    if (sm) {
      if (DECL.test(line)) ver = sm[1];
      else { since = sm[1]; sinceLine = i; continue; }
    }
    if (!DECL.test(line)) { if (since && i - sinceLine > 6) since = ''; continue; }
    if (!ver && since && i - sinceLine <= 6) ver = since;

    const s = line.trim().replace(/\s+/g, ' ');
    const kindM = s.match(/\b(fun|val|var|class|interface|object|typealias)\b/);
    const kind = kindM ? kindM[1] : '';
    let name = '', receiver = '';
    if (kind) ({ name, receiver } = parseName(kind, s.slice(s.indexOf(kind) + kind.length)));
    if (kind && name) rows.push({ pkg, kind, name, receiver, since: ver, sig: s.slice(0, 260), file: rel, line: i + 1 });
    if (since) since = '';
  }
}

writeFileSync(join(OUT, 'api.tsv'), rows.map(r =>
  [r.since || '-', r.pkg, r.kind, r.name, r.receiver || '-', r.file + ':' + r.line, r.sig].join('\t')
).join('\n') + '\n');

const bySince = new Map(), byPkg = new Map(), byPkgSince = new Map();
for (const r of rows) {
  const v = r.since || 'undated';
  bySince.set(v, (bySince.get(v) || 0) + 1);
  byPkg.set(r.pkg, (byPkg.get(r.pkg) || 0) + 1);
  byPkgSince.set(r.pkg + '@' + v, (byPkgSince.get(r.pkg + '@' + v) || 0) + 1);
}
const fmt = m => [...m.entries()].sort((a, b) => String(a[0]).localeCompare(String(b[0]))).map(([k, v]) => `${k}\t${v}`).join('\n');
writeFileSync(join(OUT, 'count_by_since.tsv'), fmt(bySince));
writeFileSync(join(OUT, 'count_by_pkg.tsv'), fmt(byPkg));
writeFileSync(join(OUT, 'count_by_pkg_since.tsv'), fmt(byPkgSince));

const ded = new Map();
for (const r of rows) {
  const key = [r.pkg, r.kind, r.name].join('|');
  if (!ded.has(key)) ded.set(key, { pkg: r.pkg, kind: r.kind, name: r.name, recv: r.receiver, n: 0, vers: new Set(), best: null });
  const d = ded.get(key);
  d.n++;
  if (r.since) d.vers.add(r.since);
  if (!d.best || r.sig.length < d.best.length) d.best = r.sig;
}
const dedArr = [...ded.values()].sort((a, b) => a.pkg.localeCompare(b.pkg) || a.name.localeCompare(b.name));
writeFileSync(join(OUT, 'dedup_api.tsv'), ['pkg\tkind\tname\treceiver\tversions\toverloads\tsimplest_sig'].concat(
  dedArr.map(d => [d.pkg, d.kind, d.name, d.recv || '-', [...d.vers].sort().join(',') || '-', d.n, d.best].join('\t'))).join('\n') + '\n');

const SL = join(OUT, 'slices');
mkdirSync(SL, { recursive: true });
const perPkg = new Map();
for (const r of rows) { if (!perPkg.has(r.pkg)) perPkg.set(r.pkg, []); perPkg.get(r.pkg).push(r); }
for (const [pkg, rs] of perPkg) {
  writeFileSync(join(SL, pkg.replace(/\./g, '_') + '.tsv'),
    ['since\tkind\tname\treceiver\tarity\tsig\tloc'].concat(
      rs.map(r => [r.since || '-', r.kind, r.name, r.receiver || '-', (r.sig.match(/,/g) || []).length, r.sig, r.file + ':' + r.line].join('\t'))).join('\n') + '\n');
}

const vb = new Map();
for (const d of dedArr) for (const v of d.vers) {
  if (!vb.has(v)) vb.set(v, []);
  vb.get(v).push(`${d.pkg}\t${d.kind}\t${d.name}\t${d.best}`);
}
writeFileSync(join(OUT, 'api_by_version.tsv'),
  [...vb.entries()].sort((a, b) => a[0].localeCompare(b[0]))
    .flatMap(([v, list]) => list.map(l => v + '\t' + l)).join('\n') + '\n');

console.log(JSON.stringify({ files: files.length, decls: rows.length, dated: rows.filter(r => r.since).length, pkgs: perPkg.size, dedup: dedArr.length }));
