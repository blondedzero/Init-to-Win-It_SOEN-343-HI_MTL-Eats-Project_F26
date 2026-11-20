import { apiRequest } from "../Client"

export async function getOrderHistory() {
    return apiRequest('/api/prc/history')
}

export function refundOrder(orderId, amount, reason) {
    return apiRequest(`/api/prc/orders/${orderId}/refund`, {
        method: 'POST',
        body: { amount, reason },
    })
}