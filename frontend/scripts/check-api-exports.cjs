// 检查 `import { x } from '@/api/*'` 是否都能在对应文件里找到导出
// 用途：防止改/新建 api 模块时漏掉旧导出，导致页面整页加载失败
const fs = require('fs')
const path = require('path')

const SRC = path.join(__dirname, '..', 'src')
const API_DIR = path.join(SRC, 'api')

function walk(dir, out = []) {
  for (const name of fs.readdirSync(dir)) {
    const p = path.join(dir, name)
    const st = fs.statSync(p)
    if (st.isDirectory()) walk(p, out)
    else if (/\.(vue|js)$/.test(name)) out.push(p)
  }
  return out
}

function exportsOf(file) {
  const text = fs.readFileSync(file, 'utf8')
  const names = new Set()
  for (const m of text.matchAll(/export\s+(?:const|let|function|class)\s+([A-Za-z0-9_$]+)/g)) names.add(m[1])
  for (const m of text.matchAll(/export\s*\{([^}]+)\}/g)) {
    for (const part of m[1].split(',')) {
      const n = part.trim().split(/\s+as\s+/).pop().trim()
      if (n) names.add(n)
    }
  }
  return names
}

let bad = 0
for (const file of walk(SRC)) {
  const text = fs.readFileSync(file, 'utf8')
  for (const m of text.matchAll(/import\s*\{([^}]+)\}\s*from\s*['"]@\/api\/([A-Za-z0-9_-]+)['"]/g)) {
    const apiFile = path.join(API_DIR, `${m[2]}.js`)
    if (!fs.existsSync(apiFile)) {
      console.log('MISSING_MODULE', path.relative(SRC, file), `@/api/${m[2]}`)
      bad++
      continue
    }
    const available = exportsOf(apiFile)
    for (const raw of m[1].split(',')) {
      const name = raw.trim().split(/\s+as\s+/)[0].trim()
      if (name && !available.has(name)) {
        console.log('MISSING_EXPORT', path.relative(SRC, file), `@/api/${m[2]} -> ${name}`)
        bad++
      }
    }
  }
}
console.log(bad === 0 ? 'OK: all api imports resolved' : `FAILED: ${bad} unresolved import(s)`)
process.exitCode = bad ? 1 : 0
