import { apiRequest } from './Client'

export const STORESTATUS = {
  OPEN: 'OPEN',
  CLOSED: 'CLOSED',
  PAUSED: 'PAUSED',
}

export function getOpenStores() {
  return apiRequest('/api/stores')
}

export function getProducts(storeId) {
  return apiRequest(`/api/stores/${storeId}/products`)
}


export function getMyStore() {
  return apiRequest('/api/stores/me')
}

export function changeStoreStatus(status) {
  return apiRequest('/api/stores/me/status', { method: 'POST', body: { status } })
}

export function getMyProducts() {
  return apiRequest('/api/stores/me/products')
}

export function getCategories() {
  return apiRequest('/api/stores/categories')
}

export function addProduct(product) {
  return apiRequest('/api/stores/me/products', { method: 'POST', body: product })
}

export function updateProduct(productId, product) {
  return apiRequest(`/api/stores/me/products/${productId}`, { method: 'PUT', body: product })
}

export function removeProduct(productId) {
  return apiRequest(`/api/stores/me/products/${productId}`, { method: 'DELETE' })
}

export function restoreProduct(productId) {
  return apiRequest(`/api/stores/me/products/${productId}/restore`, { method: 'POST' })
}

export function getOrderHistory() {
  return apiRequest('/api/stores/me/orders/history')
}
