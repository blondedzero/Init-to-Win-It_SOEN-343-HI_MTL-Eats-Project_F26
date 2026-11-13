export const STORESTATUS = {
    OPEN: 'OPEN',
    CLOSED: 'CLOSED',
    PAUSED: 'PAUSED',
}
import { apiRequest } from './Client'

export function getOpenStores() {
  return apiRequest('/api/stores')
}

export function getProducts(storeId) {
  return apiRequest(`/api/stores/${storeId}/products`)
}