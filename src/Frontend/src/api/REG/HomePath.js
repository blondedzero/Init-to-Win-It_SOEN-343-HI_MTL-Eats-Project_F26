import { getAuthenticatedUserData } from './Auth'
import { ROLE } from '../EnumRoles'

export function homePathFor(role) {
  switch (role) {
    case ROLE[0]: return '/homeCustomer'
    case ROLE[1]: return '/store/orders'
    default: return '/account'
  }
}

export async function getHomePath(token) {
  const res = await getAuthenticatedUserData(token)
  const user = await res.json()
  return homePathFor(user.role)
}
