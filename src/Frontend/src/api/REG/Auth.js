import { BACKEND_ROUTE } from "../Helpers"

export async function validateToken(token) {
     if (!token) {
        throw new Error('Invalid or expired token.')
      }
    
      const endpoint = `${BACKEND_ROUTE}/api/users/auth`
      const payload = {token}
    
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      })
    
      if (!res.ok) {
        const resBody = await res.json()
        throw new Error(resBody.error || 'Authentication failed. Access Denied')
      }
    
      return res
}