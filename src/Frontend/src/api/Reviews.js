import { apiRequest } from './Client'

export function rateOrder(orderId, storeScore, courierScore) {
  return apiRequest(`/api/orders/${orderId}/rating`, { method: 'POST', body: { storeScore, courierScore } })
}

export function getStoreRating(storeId) {
  return apiRequest(`/api/stores/${storeId}/rating`)
}

export function getMyCourierRating() {
  return apiRequest('/api/courier/me/rating')
}

export function getRatingScale() {
  return apiRequest('/api/ratings/scale')
}
