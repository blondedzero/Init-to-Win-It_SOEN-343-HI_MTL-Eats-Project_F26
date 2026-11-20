import { apiRequest } from "../Client"

export async function getOrderHistory() {
    return apiRequest('/api/prc/history')
}