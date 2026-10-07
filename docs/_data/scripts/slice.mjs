import { readFileSync, writeFileSync, mkdirSync } from 'fs';
import { join } from 'path';

const W = process.argv[2]; // e.g. C:/Users/.../Temp/kstdlib
const A = join(W, 'out');       // 2.2.10
const B = join(W, 'out2110');   // 2.1.10
const SL = join(A, 'slices');
mkdirSync(SL, { recursive: true });

const load = p => readFileSync(p, 'utf8').trim().split('\n').map(l => l.split('\t'));

const rows = load(join(A, 'api.tsv'));      // since pkg kind name loc sig
const older = load(join(B, 'api.tsv'));

// ---- slices per package
const byPkg = new Map();
for (const r of rows) {
  const pkg = r[1] || '-';
  if (!byPkg.has(pkg)) byPkg.set(pkg, []);
  byPkg.get(pkg).push(r);
}
for (const [pkg, rs] of byPkg) {
  const safe = pkg.replace(/\./g, '_');
  writeFileSync(join(SL, safe + '.tsv'),
    ['since\tkind\tname\tarity\tsig\tloc', ...rs.map(r => {
      const sig = r[5] || '';
      const arity = (sig.match(/,/g) || []).length;
      return [r[0], r[2], r[3], arity, sig, r[4]].join('\t');
    })].join('\n') + '\n');
}

// ---- dedup: pkg|kind|name -> overloads + versions + simplest sig
const ded = new Map();
for (const r of rows) {
  const key = [r[1], r[2], r[3]].join('|');
  if (!ded.has(key)) ded.set(key, { pkg: r[1], kind: r[2], name: r[3], n: 0, vers: new Set(), best: null });
  const d = ded.get(key);
  d.n++;
  if (r[0] !== '-') d.vers.add(r[0]);
  const sig = r[5] || '';
  if (!d.best || sig.length < d.best.length) d.best = sig;
}
const dedArr = [...ded.values()].sort((a, b) => a.pkg.localeCompare(b.pkg) || a.name.localeCompare(b.name));
writeFileSync(join(A, 'dedup_api.tsv'), ['pkg\tkind\tname\tversions\toverloads\tsimplest_sig'].concat(
  dedArr.map(d => [d.pkg, d.kind, d.name, [...d.vers].sort().join(',') || '-', d.n, d.best].join('\t'))).join('\n') + '\n');

// ---- new / removed between 2.1.10 and 2.2.10
const keyOf = r => [r[1], r[2], r[3]].join('|');
const oldSet = new Set(older.map(keyOf));
const newSet = new Set(rows.map(keyOf));
const added = dedArr.filter(d => !oldSet.has([d.pkg, d.kind, d.name].join('|')));
const removedKey = [...oldSet].filter(k => !newSet.has(k));
const olderDed = new Map();
for (const r of older) { const k = keyOf(r); if (!olderDed.has(k)) olderDed.set(k, r); }
writeFileSync(join(A, 'added_vs_2110.tsv'), ['pkg\tkind\tname\tsince\tsig'].concat(
  added.map(d => [d.pkg, d.kind, d.name, [...d.vers].sort().join(',') || '-', d.best].join('\t'))).join('\n') + '\n');
writeFileSync(join(A, 'removed_vs_2110.tsv'), ['pkg\tkind\tname\tsig'].concat(
  removedKey.map(k => { const r = olderDed.get(k); return [r[1], r[2], r[3], r[5]].join('\t'); })).join('\n') + '\n');

// ---- unique API names per introduced version
const verBuckets = new Map();
for (const d of dedArr) d.vers.delete('-');
for (const d of dedArr) {
  for (const v of [...d.vers]) {
    if (!verBuckets.has(v)) verBuckets.set(v, []);
    verBuckets.get(v).push(`${d.pkg}  ${d.kind}  ${d.name}`);
  }
}
const verTxt = [...verBuckets.entries()].sort((a, b) => a[0].localeCompare(b[0])).map(([v, list]) =>
  `### @SinceKotlin("${v}") — ${list.length} 个声明（去重后 ${new Set(list).size} 个 API 名）\n` +
  [...new Set(list)].sort().join('\n')).join('\n\n');
writeFileSync(join(A, 'api_by_version.md'), verTxt + '\n');

console.log(JSON.stringify({
  pkgs: byPkg.size, dedup_apis: dedArr.length,
  added_vs_2110: added.length, removed_vs_2110: removedKey.length,
  versions: [...verBuckets.keys()].sort()
}));
