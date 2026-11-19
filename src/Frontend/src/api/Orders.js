import { apiRequest } from './Client'

export const ORDERSTATUS = {
    PLACED: 'PLACED',
    ACCEPTED: 'ACCEPTED',
    PREPARING: 'PREPARING',
    REJECTED: 'REJECTED',
    CANCELED: 'CANCELED',
    READY: 'READY',
    PICKED_UP: 'PICKED_UP',
    DELIVERED: 'DELIVERED',
}

export function placeOrder(storeId, lines) {
  return apiRequest('/api/orders', { method: 'POST', body: { storeId, lines } })
}

export function getCustomerActiveOrders() {
  return apiRequest('/api/customer/orders/active')
}

export function cancelOwnOrder(orderId) {
  return apiRequest(`/api/customer/orders/${orderId}/cancel`, { method: 'POST' })
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