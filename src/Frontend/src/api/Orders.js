import { apiRequest } from './Client'

export function placeOrder(storeId, lines) {
  return apiRequest('/api/orders', { method: 'POST', body: { storeId, lines } })
}

export function getIncomingOrders() {
  return apiRequest('/api/store/orders/incoming')
}

export function getQueue() {
  return apiRequest('/api/store/queue')
}

export function getOrdersInPreparation() {
  return apiRequest('/api/store/orders/preparing')
}

export function getReadyOrders() {
  return apiRequest('/api/store/orders/ready')
}

export function acceptOrder(orderId) {
  return apiRequest(`/api/store/orders/${orderId}/accept`, { method: 'POST' })
}

export function rejectOrder(orderId, reason) {
  return apiRequest(`/api/store/orders/${orderId}/reject`, { method: 'POST', body: { reason } })
}

export function startPreparing(orderId) {
  return apiRequest(`/api/store/orders/${orderId}/start-preparing`, { method: 'POST' })
}

export function markReady(orderId) {
  return apiRequest(`/api/store/orders/${orderId}/ready`, { method: 'POST' })
}

export function getAllOrdersInProgress() {
  return apiRequest('/api/orders/in-progress', {method: 'GET'})
}
