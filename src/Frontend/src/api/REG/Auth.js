import { BACKEND_ROUTE } from "../Helpers"

export async function validateAuthToken(token) {
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

export async function getAuthenticatedUserData(token) {
      if (!token) {
        throw new Error('Invalid or expired token.')
      }
      
      const endpoint = `${BACKEND_ROUTE}/api/users/data`
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
        const error = new Error(resBody.error || 'Authentication failed. Access Denied')
        error.status = res.status
        throw error
      }
    
      return res
}