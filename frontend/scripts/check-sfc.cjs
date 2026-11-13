// 用 @vue/compiler-sfc 校验 .vue 语法（不触发 esbuild，规避沙箱 spawn 限制）
const fs = require('fs')
const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')

let bad = 0
for (const file of process.argv.slice(2)) {
  const source = fs.readFileSync(file, 'utf8')
  const { descriptor, errors } = parse(source, { filename: file })
  if (errors.length) {
    console.log('PARSE_ERR', file, errors.map(e => e.message).join('; '))
    bad++
    continue
  }
  try {
    compileScript(descriptor, { id: 'check' })
  } catch (e) {
    console.log('SCRIPT_ERR', file, e.message)
    bad++
    continue
  }
  if (descriptor.template) {
    const r = compileTemplate({ source: descriptor.template.content, filename: file, id: 'check' })
    if (r.errors.length) {
      console.log('TEMPLATE_ERR', file, r.errors.map(e => e.message || e).join('; '))
      bad++
      continue
    }
  }
  console.log('OK', file)
}
process.exitCode = bad ? 1 : 0
