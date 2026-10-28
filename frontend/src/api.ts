// Пока пользователь фиксированный; после авторизации здесь будет токен.
const USER_ID = 'ilya'
const headers = { 'Content-Type': 'application/json', 'X-User-Id': USER_ID }

export interface WordCard {
  id: number
  word: string
  translation: string
  language: string
  createdAt: string
}

export async function listCards(): Promise<WordCard[]> {
  const res = await fetch('/api/learning/cards', { headers })
  if (!res.ok) throw new Error(`Ошибка загрузки карточек: ${res.status}`)
  return res.json()
}

export async function addCard(word: string, translation: string, language = 'en'): Promise<WordCard> {
  const res = await fetch('/api/learning/cards', {
    method: 'POST',
    headers,
    body: JSON.stringify({ word, translation, language }),
  })
  if (!res.ok) throw new Error(`Не удалось сохранить слово: ${res.status}`)
  return res.json()
}

/**
 * Читает SSE-поток от ai-orchestrator и отдаёт токены по мере прихода.
 * EventSource не умеет POST, поэтому разбираем поток вручную через fetch.
 */
export async function streamChat(message: string, onToken: (token: string) => void): Promise<void> {
  const res = await fetch('/api/ai/chat', {
    method: 'POST',
    headers: { ...headers, Accept: 'text/event-stream' },
    body: JSON.stringify({ message }),
  })
  if (!res.ok || !res.body) throw new Error(`Репетитор недоступен: ${res.status}`)

  const reader = res.body.pipeThrough(new TextDecoderStream()).getReader()
  let buffer = ''
  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += value
    const events = buffer.split('\n\n')
    buffer = events.pop() ?? ''
    for (const event of events) {
      const data = event
        .split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5))
        .join('\n')
      if (data) onToken(data)
    }
  }
}
