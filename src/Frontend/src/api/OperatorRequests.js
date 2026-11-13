import { apiRequest } from "./Client"

const BASE = '/api/operator'

//STORE OPERATIONS
export function getAllStores() {
  return apiRequest(`${BASE}/stores/all`)
}

export function getAllOpenAndPausedStores() {
  return apiRequest(`${BASE}/stores/open-paused`)
}

//ORDER OPERATIONS
export function getAllOrdersInProgress() {
  return apiRequest(`${BASE}/orders/in-progress`)
}

export function getAllOrdersReady() {
  return apiRequest(`${BASE}/orders/ready`)
}

//COURIER OPERATIONS
export function getAllCouriersOnDuty() {
    return apiRequest(`${BASE}/couriers/on-duty`)
}

export function getAllCouriersIdle() {
    return apiRequest(`${BASE}/couriers/idle`)
}