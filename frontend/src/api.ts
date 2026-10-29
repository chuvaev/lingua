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
  if (!res.ok) throw await toError(res, 'Ошибка загрузки карточек')
  return res.json()
}

export class ApiError extends Error {
  constructor(message: string, readonly code?: string, readonly suggestions: string[] = []) {
    super(message)
  }
}

interface ApiProblem {
  status: number
  detail?: string
  code?: string
  suggestions?: string[]
}

const messages: Record<string, string> = {
  WORD_ALREADY_EXISTS: 'Такое слово уже есть в словаре',
  WORD_MISSPELLED: 'Возможно, в слове опечатка',
}

async function toError(res: Response, fallback: string): Promise<ApiError> {
  try {
    const problem: ApiProblem = await res.json()
    const message = (problem.code && messages[problem.code]) || problem.detail || fallback
    return new ApiError(message, problem.code, problem.suggestions ?? [])
  } catch {
    return new ApiError(`${fallback}: ${res.status}`)
  }
}

export async function addCard(word: string, translation: string, language = 'en', force = false): Promise<WordCard> {
  const res = await fetch('/api/learning/cards', {
    method: 'POST',
    headers,
    body: JSON.stringify({ word, translation, language, force }),
  })
  if (!res.ok) throw await toError(res, 'Не удалось сохранить слово')
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
