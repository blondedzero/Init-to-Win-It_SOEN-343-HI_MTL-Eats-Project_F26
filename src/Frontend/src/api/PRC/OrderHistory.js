import { apiRequest } from "../Client"

export async function getOrderHistory(userId) {
    return apiRequest(`/api/prc/history/${userId}`)
}