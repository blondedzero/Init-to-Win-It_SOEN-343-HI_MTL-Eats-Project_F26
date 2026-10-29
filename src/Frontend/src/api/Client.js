import { BACKEND_ROUTE } from './Helpers'
import { logCommand, logError } from '../UI/Console/consoleLog'

export function getAuthToken() {
  return localStorage.getItem('authToken') || sessionStorage.getItem('authToken')
}

export async function apiRequest(path, { method = 'GET', body } = {}) {
  logCommand(`${method} ${path}`)

  const headers = { Authorization: `Bearer ${getAuthToken()}` }
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let res
  try {
    res = await fetch(`${BACKEND_ROUTE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    const error = new Error('Could not reach the server.')
    logError(error.message)
    throw error
  }

  const resBody = await res.json().catch(() => ({}))
  if (!res.ok) {
    const error = new Error(resBody.error || `Request failed (${res.status})`)
    error.status = res.status
    logError(`${res.status} ${error.message}`)
    throw error
  }
  return resBody
}
