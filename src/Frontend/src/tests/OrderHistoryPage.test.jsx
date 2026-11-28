import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { useOutletContext } from 'react-router-dom'
import { getOrderHistory, refundOrder } from '../api/PRC/PRCRequests'
import { getRatingScale, rateOrder } from '../api/Reviews'
import { ORDERSTATUS } from '../api/Orders'
import { ROLE } from '../api/EnumRoles'
import OrderHistoryPage from '../UI/Dashboard/OrderHistoryPage'

vi.mock('react-router-dom', () => ({
    useOutletContext: vi.fn(),
}))

vi.mock('../api/PRC/PRCRequests', () => ({
    getOrderHistory: vi.fn(),
    refundOrder: vi.fn(),
}))

vi.mock('../api/Reviews', () => ({
    getRatingScale: vi.fn(),
    rateOrder: vi.fn(),
}))

function createOrder(orderId, overrides = {}) {
    return {
        orderId,
        status: ORDERSTATUS.DELIVERED,
        storeName: 'Fresh Market',
        customerName: 'Customer Example',
        courierName: 'Courier Example',
        placedAt: '2025-02-14T10:00:00Z',
        deliveredAt: '2025-02-14T12:30:00Z',
        canceledAt: null,
        lines: [{ quantity: 1, name: 'Apples' }],
        totalCost: 10,
        rejectionReason: null,
        pricing: null,
        refund: null,
        rating: null,
        ...overrides,
    }
}

function renderOrderHistory(role) {
    useOutletContext.mockReturnValue({ user: { role } })
    return render(<OrderHistoryPage />)
}

function submitRating(form) {
    const oneStarButtons = within(form).getAllByRole('radio', { name: '1 star' })
    fireEvent.click(oneStarButtons[0])
    fireEvent.click(oneStarButtons[1])
    fireEvent.click(within(form).getByRole('button', { name: 'Submit rating' }))
}

afterEach(() => cleanup())

beforeEach(() => {
    vi.clearAllMocks()
    getOrderHistory.mockResolvedValue([createOrder(51), createOrder(52)])
    refundOrder.mockResolvedValue({})
    getRatingScale.mockResolvedValue({ minScore: 1, maxScore: 5 })
    rateOrder.mockResolvedValue({})
})

describe('OrderHistoryPage action forms', () => {
    it('keeps only the most recently opened refund form open', async () => {
        const { container } = renderOrderHistory(ROLE.OPERATOR)

        await screen.findByText('Order #51')
        const lines = container.querySelectorAll('.history-line')
        await within(lines[0]).findByRole('button', { name: 'Refund order' })
        fireEvent.click(within(lines[0]).getByRole('button', { name: 'Refund order' }))
        expect(lines[0].querySelector('.refund-form')).not.toBeNull()

        fireEvent.click(within(lines[1]).getByRole('button', { name: 'Refund order' }))

        expect(lines[0].querySelector('.refund-form')).toBeNull()
        expect(lines[1].querySelector('.refund-form')).not.toBeNull()
    })

    it('shows refund errors only beside the order whose refund failed', async () => {
        refundOrder.mockRejectedValueOnce(new Error('Refund declined'))
        const { container } = renderOrderHistory(ROLE.OPERATOR)

        await screen.findByText('Order #51')
        const lines = container.querySelectorAll('.history-line')
        await within(lines[0]).findByRole('button', { name: 'Refund order' })
        fireEvent.click(within(lines[0]).getByRole('button', { name: 'Refund order' }))
        const form = lines[0].querySelector('.refund-form')
        fireEvent.change(within(form).getByLabelText('Reason'), { target: { value: 'Missing item' } })
        fireEvent.click(within(form).getByRole('button', { name: 'Confirm refund' }))

        const alert = await within(form).findByRole('alert')
        expect(alert.textContent).toContain('Refund declined')
        expect(container.querySelectorAll('.refund-form .dash-error')).toHaveLength(1)
        expect(lines[1].textContent).not.toContain('Refund declined')
    })

    it('shows refund success beside the refunded order', async () => {
        getOrderHistory
            .mockReset()
            .mockResolvedValueOnce([createOrder(51), createOrder(52)])
            .mockResolvedValueOnce([
                createOrder(51, {
                    status: ORDERSTATUS.REFUNDED,
                    refund: {
                        amount: 10,
                        refundedAt: '2025-02-14T13:00:00Z',
                        reason: 'Missing item',
                        operatorId: 7,
                    },
                }),
                createOrder(52),
            ])
        const { container } = renderOrderHistory(ROLE.OPERATOR)

            await screen.findByText('Order #51')
            const line = container.querySelector('.history-line')
        await within(line).findByRole('button', { name: 'Refund order' })
        fireEvent.click(within(line).getByRole('button', { name: 'Refund order' }))
        fireEvent.change(within(line).getByLabelText('Reason'), { target: { value: 'Missing item' } })
        fireEvent.click(within(line).getByRole('button', { name: 'Confirm refund' }))

        const status = await within(line).findByRole('status')
        expect(status.textContent).toContain('Order #51 refunded successfully.')
        expect(container.querySelectorAll('.history-action-feedback')).toHaveLength(1)
    })

    it('shows rating errors only beside the order whose rating failed', async () => {
        rateOrder.mockRejectedValueOnce(new Error('Rating declined'))
        const { container } = renderOrderHistory(ROLE.CUSTOMER)

        await screen.findByText('Order #51')
        const lines = container.querySelectorAll('.history-line')
        await within(lines[0]).findByRole('button', { name: 'Rate order' })
        fireEvent.click(within(lines[0]).getByRole('button', { name: 'Rate order' }))
        const form = lines[0].querySelector('.rating-form')
        submitRating(form)

        const alert = await within(form).findByRole('alert')
        expect(alert.textContent).toContain('Rating declined')
        expect(container.querySelectorAll('.rating-form .dash-error')).toHaveLength(1)
        expect(lines[1].textContent).not.toContain('Rating declined')
    })

    it('shows rating success beside the rated order', async () => {
        getOrderHistory
            .mockReset()
            .mockResolvedValueOnce([createOrder(51), createOrder(52)])
            .mockResolvedValueOnce([
                createOrder(51, {
                    rating: {
                        storeScore: 5,
                        courierScore: 4,
                        ratedAt: '2025-02-14T13:00:00Z',
                    },
                }),
                createOrder(52),
            ])
        const { container } = renderOrderHistory(ROLE.CUSTOMER)

            await screen.findByText('Order #51')
            const line = container.querySelector('.history-line')
        await within(line).findByRole('button', { name: 'Rate order' })
        fireEvent.click(within(line).getByRole('button', { name: 'Rate order' }))
        submitRating(line.querySelector('.rating-form'))

        const status = await within(line).findByRole('status')
        expect(status.textContent).toContain('Order #51 rated successfully.')
        expect(container.querySelectorAll('.history-action-feedback')).toHaveLength(1)
    })
})
