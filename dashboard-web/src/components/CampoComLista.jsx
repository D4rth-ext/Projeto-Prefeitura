import { useRef, useState } from 'react'

const normalizar = texto => texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim()

export default function CampoComLista({ id, nomeLista, itens, valor, alterar, selecionar = alterar, obrigatorio = false }) {
  const listaId = `${id}-opcoes`
  const input = useRef(null)
  const [aberto, setAberto] = useState(false)
  const [filtrar, setFiltrar] = useState(false)
  const [ativo, setAtivo] = useState(-1)
  const opcoes = filtrar ? itens.filter(nome => normalizar(nome).includes(normalizar(valor))) : itens
  const fechar = () => { setAberto(false); setAtivo(-1) }
  const escolher = nome => { selecionar(nome); fechar() }

  function teclado(e) {
    if (e.key === 'Escape' && aberto) { e.preventDefault(); fechar(); return }
    if (e.key === 'Enter' && aberto && ativo >= 0 && opcoes[ativo]) {
      e.preventDefault()
      escolher(opcoes[ativo])
      return
    }
    if (!['ArrowDown', 'ArrowUp'].includes(e.key)) return
    e.preventDefault()
    setAberto(true)
    if (!opcoes.length) return
    const proximo = e.key === 'ArrowDown' ? Math.min(ativo + 1, opcoes.length - 1) : ativo <= 0 ? opcoes.length - 1 : ativo - 1
    setAtivo(proximo)
    requestAnimationFrame(() => document.getElementById(`${listaId}-${proximo}`)?.scrollIntoView({ block: 'nearest' }))
  }

  return <div className="seletor-setor" onBlur={e => { if (!e.currentTarget.contains(e.relatedTarget)) fechar() }}>
    <input ref={input} id={id} type="text" role="combobox" aria-expanded={aberto} aria-controls={listaId} aria-autocomplete="list"
      aria-activedescendant={aberto && ativo >= 0 ? `${listaId}-${ativo}` : undefined}
      autoComplete="off" value={valor} required={obrigatorio}
      onClick={() => setAberto(true)} onKeyDown={teclado}
      onChange={e => { alterar(e.target.value); setFiltrar(true); setAberto(true); setAtivo(-1) }} />
    <button type="button" aria-label={`${aberto ? 'Fechar' : 'Abrir'} lista de ${nomeLista}`} aria-expanded={aberto} aria-controls={listaId}
      onClick={() => { input.current.focus(); setFiltrar(false); setAtivo(-1); setAberto(!aberto) }}>
      <i className={`ti ${aberto ? 'ti-chevron-up' : 'ti-chevron-down'}`} aria-hidden="true" />
    </button>
    <div className="seletor-setor-lista" hidden={!aberto}>
      <ul id={listaId} role="listbox" aria-label={`Sugestões de ${nomeLista}`}>
        {aberto && opcoes.map((nome, i) => <li key={nome} id={`${listaId}-${i}`} role="option" aria-selected={ativo === i}
          onMouseDown={e => e.preventDefault()} onClick={() => escolher(nome)}>{nome}</li>)}
      </ul>
      {aberto && !opcoes.length && <p role="status">Nenhuma sugestão. Você pode usar o nome digitado.</p>}
    </div>
  </div>
}
