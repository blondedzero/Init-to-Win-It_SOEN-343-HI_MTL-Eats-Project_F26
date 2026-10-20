import { BACKEND_ROUTE } from '../Helpers'

export async function login(email, password) {
  if (!email || !password) {
    throw new Error('Please fill all required fields.')
  }

  const endpoint = `${BACKEND_ROUTE}/api/users/login`
  const payload = {email, password}

  const res = await fetch(endpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  if (!res.ok) {
    const resBody = await res.json()
    throw new Error(resBody.error || 'Login failed. Please try again.')
  }

  return res
}

export function logout() {
  localStorage.removeItem('authToken')
  sessionStorage.removeItem('authToken')
}