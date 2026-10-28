import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'

const source = readFileSync(new URL('../public/sw.js', import.meta.url), 'utf8')
function worker(fetch = async () => 'online') {
  const handlers = {}
  const cache = { addAll: async () => {} }
  vm.runInNewContext(source, {
    self: { location: { origin: 'https://manutencao.test' }, clients: { claim: async () => {} }, addEventListener: (name, handler) => { handlers[name] = handler } },
    URL, fetch,
    caches: { open: async () => cache, keys: async () => [], match: async () => 'offline', delete: async () => true }
  })
  return handlers
}
function request(path, { method = 'GET', mode = 'cors', authorization = false } = {}) {
  return { url: `https://manutencao.test${path}`, method, mode, headers: { has: name => name === 'Authorization' && authorization } }
}
test('requisições à API e operações de escrita não são interceptadas', () => {
  const handlers = worker()
  for (const req of [request('/api/v1/os'), request('/api/v1/auth/login', { method: 'POST' }), request('/qualquer', { method: 'PATCH' })]) {
    handlers.fetch({ request: req, respondWith: () => assert.fail('Requisição não deve entrar no cache') })
  }
})
test('requisições autenticadas nunca são interceptadas', () => {
  worker().fetch({ request: request('/offline.html', { authorization: true }), respondWith: () => assert.fail('Resposta autenticada não pode ser armazenada') })
})
test('navegação online usa a rede', async () => {
  let response
  worker().fetch({ request: request('/ordens-servico/1', { mode: 'navigate' }), respondWith: r => { response = r } })
  assert.equal(await response, 'online')
})
test('navegação offline exibe a página pública de recuperação', async () => {
  let response
  worker(async () => { throw new TypeError('offline') }).fetch({ request: request('/agenda', { mode: 'navigate' }), respondWith: r => { response = r } })
  assert.equal(await response, 'offline')
})
test('ícones do manifesto existem e têm as dimensões declaradas', () => {
  const manifest = JSON.parse(readFileSync(new URL('../public/manifest.webmanifest', import.meta.url), 'utf8'))
  assert.equal(manifest.display, 'standalone')
  for (const icon of manifest.icons) {
    const png = readFileSync(new URL(`../public${icon.src}`, import.meta.url))
    assert.equal(`${png.readUInt32BE(16)}x${png.readUInt32BE(20)}`, icon.sizes)
  }
})
