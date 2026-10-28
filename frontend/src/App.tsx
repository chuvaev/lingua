import { useEffect, useState, type FormEvent } from 'react'
import { addCard, listCards, streamChat, type WordCard } from './api'

export default function App() {
  return (
    <main className="layout">
      <header>
        <h1>Lingua</h1>
        <p className="muted">Словарь и разговорная практика с репетитором</p>
      </header>
      <div className="columns">
        <Deck />
        <Chat />
      </div>
    </main>
  )
}

function Deck() {
  const [cards, setCards] = useState<WordCard[]>([])
  const [word, setWord] = useState('')
  const [translation, setTranslation] = useState('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    listCards().then(setCards).catch((e: Error) => setError(e.message))
  }, [])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!word.trim() || !translation.trim()) {
      setError('Заполни слово и перевод')
      return
    }
    try {
      const card = await addCard(word.trim(), translation.trim())
      setCards((prev) => [card, ...prev])
      setWord('')
      setTranslation('')
      setError(null)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <section className="panel">
      <h2>Мои слова</h2>
      <form onSubmit={onSubmit} className="row">
        <input value={word} onChange={(e) => setWord(e.target.value)} placeholder="Слово" />
        <input value={translation} onChange={(e) => setTranslation(e.target.value)} placeholder="Перевод" />
        <button type="submit">Добавить</button>
      </form>
      {error && <p className="error">{error}</p>}
      <ul className="cards">
        {cards.map((c) => (
          <li key={c.id}>
            <strong>{c.word}</strong> <span className="muted">— {c.translation}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}

interface Message {
  role: 'user' | 'tutor'
  text: string
}

function Chat() {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const text = input.trim()
    if (!text || busy) return
    setInput('')
    setError(null)
    setBusy(true)
    setMessages((prev) => [...prev, { role: 'user', text }, { role: 'tutor', text: '' }])
    try {
      await streamChat(text, (token) =>
        setMessages((prev) => {
          const next = [...prev]
          const last = next[next.length - 1]
          next[next.length - 1] = { ...last, text: last.text + token }
          return next
        }),
      )
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel">
      <h2>Репетитор</h2>
      <div className="chat">
        {messages.length === 0 && <p className="muted">Напиши что-нибудь по-английски — репетитор поправит ошибки.</p>}
        {messages.map((m, i) => (
          <p key={i} className={`bubble ${m.role}`}>
            {m.text || (busy ? '…' : '')}
          </p>
        ))}
      </div>
      <form onSubmit={onSubmit} className="row">
        <input value={input} onChange={(e) => setInput(e.target.value)} placeholder="Сообщение" disabled={busy} />
        <button type="submit" disabled={busy}>
          {busy ? 'Думает…' : 'Отправить'}
        </button>
      </form>
      {error && <p className="error">{error}</p>}
    </section>
  )
}
