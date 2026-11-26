import { apiRequest } from "./Client"

const BASE = '/api/operator'
function opRequest(route,  { method, body } = {}) {
  return apiRequest( `${BASE}${route}`, { method, body })
}

//STORE OPERATIONS
export function getAllStores() {
  return opRequest('/stores/all')
}

export function getAllStoresForMap() {
  return opRequest('/stores/map')
}

export function openStore(storeId) {
  return opRequest(`/stores/${storeId}/open`, {method: 'POST'})
}

export function pauseStore(storeId) {
  return opRequest(`/stores/${storeId}/pause`, {method: 'POST'})
}

//ORDER OPERATIONS
export function getAllOrdersInProgress() {
  return opRequest('/orders/in-progress')
}

export function getAllOrdersReady() {
  return opRequest('/orders/ready')
}

export function cancelOrder(orderId) {
  return opRequest(`/orders/${orderId}/cancel`, { method: 'POST' })
}

export function assignOrderToCourier(courierId, orderId) {
  return opRequest(`/courier/${courierId}/assign/${orderId}`, { method: 'POST' })
}

export function resetSimulation() {
  return opRequest('/simulation/reset', { method: 'POST' })
}

//COURIER OPERATIONS
export function getAllCouriersOnDuty() {
    return opRequest('/couriers/on-duty')
}

export function getAllCouriersIdle() {
    return opRequest('/couriers/idle')
}