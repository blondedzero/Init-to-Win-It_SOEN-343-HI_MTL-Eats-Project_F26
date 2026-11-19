import { apiRequest } from './Client'

export const COURIERSTATUS = {
  OFFLINE: 'OFFLINE',
  IDLE: 'IDLE',
  ASSIGNED: 'ASSIGNED',
  CARRYING: 'CARRYING',
}

export function getCourierDashboard() {
  return apiRequest('/api/courier/me')
}

export function changeCourierStatus(status) {
  return apiRequest('/api/courier/me/status', { method: 'POST', body: { status } })
}

export function acceptDelivery(orderId) {
  return apiRequest(`/api/courier/orders/${orderId}/accept`, { method: 'POST' })
}

export function confirmPickup(orderId) {
  return apiRequest(`/api/courier/orders/${orderId}/pickup`, { method: 'POST' })
}

export function confirmDelivery(orderId) {
  return apiRequest(`/api/courier/orders/${orderId}/deliver`, { method: 'POST' })
}
