import { getAuthenticatedUserData } from './Auth'
import { ROLE } from '../EnumRoles'

export function homePathFor(role) {
  switch (role) {
    case ROLE.CUSTOMER: return '/customer/home'
    case ROLE.STORE: return '/store/orders'
    case ROLE.OPERATOR: return '/operator/home'
    case ROLE.COURIER: return '/courier/home'
    default: return '/account'
  }
}

export async function getHomePath(token) {
  const res = await getAuthenticatedUserData(token)
  const user = await res.json()
  return homePathFor(user.role)
}
