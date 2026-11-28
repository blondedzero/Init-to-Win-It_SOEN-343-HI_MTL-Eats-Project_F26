import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { getCustomerActiveOrders } from '../api/Orders'
import { getCourierRating } from '../api/Reviews'
import CustomerOrdersPage from '../UI/CustomerPage/CustomerOrdersPage'

vi.mock('../api/Orders', () => ({
    cancelOwnOrder: vi.fn(),
    getCustomerActiveOrders: vi.fn(),
    ORDERSTATUS: { PLACED: 'PLACED' },
}))

vi.mock('../api/Reviews', () => ({
    getCourierRating: vi.fn(),
}))

function createOrder(overrides = {}) {
    return {
        orderId: 1,
        status: 'PREPARING',
        storeName: 'Fresh Market',
        placedAt: '2025-02-14T10:00:00Z',
        courierId: 12,
        courierName: 'Alex Courier',
        lines: [],
        totalCost: 10,
        ...overrides,
    }
}

afterEach(() => cleanup())

beforeEach(() => {
    vi.clearAllMocks()
    getCustomerActiveOrders.mockResolvedValue([
        createOrder(),
        createOrder({ orderId: 2, status: 'PLACED', courierId: null, courierName: null }),
    ])
    getCourierRating.mockResolvedValue({
        accountId: 12,
        averageScore: 4.7,
        ratingCount: 3,
        maxScore: 5,
    })
})

describe('CustomerOrdersPage', () => {
    it('shows assigned couriers with their average rating and unassigned orders clearly', async () => {
        render(<CustomerOrdersPage />)

        expect(await screen.findByText('Courier: Alex Courier')).toBeTruthy()
        expect(await screen.findByText('Average rating: 4.7 / 5 (3 ratings)')).toBeTruthy()
        expect(screen.getByText('Courier: Not assigned yet')).toBeTruthy()
        expect(getCourierRating).toHaveBeenCalledTimes(1)
        expect(getCourierRating).toHaveBeenCalledWith(12)
    })

    it('shows when an assigned courier has not received any ratings', async () => {
        getCourierRating.mockResolvedValueOnce({
            accountId: 12,
            averageScore: null,
            ratingCount: 0,
            maxScore: 5,
        })

        render(<CustomerOrdersPage />)

        expect(await screen.findByText('Average rating: Not rated yet')).toBeTruthy()
    })
})
