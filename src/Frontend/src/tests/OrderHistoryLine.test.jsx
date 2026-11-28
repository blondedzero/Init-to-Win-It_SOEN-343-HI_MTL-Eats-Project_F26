import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import OrderHistoryLine from '../UI/Dashboard/OrderHistory/OrderHistoryLine'
import { ORDERSTATUS } from '../api/Orders'
import { ROLE } from '../api/EnumRoles'

const deliveredAt = '2025-02-14T12:30:00Z'

function createOrder(overrides = {}) {
    return {
        orderId: 52,
        status: ORDERSTATUS.DELIVERED,
        storeName: 'Fresh Market',
        customerName: 'Customer Example',
        deliveredAt,
        canceledAt: null,
        lines: [{ quantity: 2, name: 'Apples' }],
        totalCost: 19.5,
        rejectionReason: null,
        pricing: {
            subtotal: 16.5,
            smallOrderFee: 1,
            serviceFee: 0.5,
            deliveryFee: 0.75,
            taxAmount: 0.75,
            ruleVersion: 'pricing-v2',
        },
        refund: null,
        ...overrides,
    }
}

function OrderHistoryLineHarness(props) {
    const [activeForm, setActiveForm] = useState(null)
    const onToggleForm = (orderId, component) => {
        setActiveForm((current) =>
            current?.orderId === orderId && current.component === component
                ? null
                : { orderId, component }
        )
    }

    return <OrderHistoryLine {...props} activeForm={activeForm} onToggleForm={onToggleForm} />
}

function renderLine(props) {
    return render(<OrderHistoryLineHarness {...props} />)
}

afterEach(() => cleanup())

describe('OrderHistoryLine', () => {
    it('shows customers the store name, order details, and delivery time', () => {
        renderLine({ order: createOrder(), role: ROLE.CUSTOMER, onRefund: vi.fn() })

        expect(screen.getByText('Fresh Market')).toBeTruthy()
        expect(screen.queryByText('Customer Example')).toBeNull()
        expect(screen.getByText('2 × Apples')).toBeTruthy()
        expect(screen.getByText(`Delivered at: ${new Date(deliveredAt).toLocaleString()}`)).toBeTruthy()
        expect(screen.getByText('Total $19.50')).toBeTruthy()
        expect(screen.queryByRole('button', { name: 'Refund order' })).toBeNull()
    })

    it('shows operators the customer name, pricing rule, and refund details', () => {
        const order = createOrder({
            status: ORDERSTATUS.REFUNDED,
            refund: {
                amount: 5,
                refundedAt: deliveredAt,
                reason: 'Item unavailable',
                operatorId: 7,
            },
        })
        renderLine({ order, role: ROLE.OPERATOR, onRefund: vi.fn() })

        expect(screen.getByText('Customer Example')).toBeTruthy()
        expect(screen.queryByText('Fresh Market')).toBeNull()
        expect(screen.getByText('PRC rule version: pricing-v2')).toBeTruthy()
        expect(screen.getByText('Original charge unchanged: $19.50')).toBeTruthy()
        expect(screen.getByText('Reason: Item unavailable')).toBeTruthy()
        expect(screen.getByText('Operator #7')).toBeTruthy()
        expect(screen.queryByRole('button', { name: 'Refund order' })).toBeNull()
    })

    it('shows a rejected order reason and its rejection time', () => {
        const order = createOrder({
            status: ORDERSTATUS.REJECTED,
            canceledAt: deliveredAt,
            rejectionReason: 'Store could not fulfill the order',
        })
        renderLine({ order, role: ROLE.CUSTOMER, onRefund: vi.fn() })

        expect(screen.getByText(`Rejected at: ${new Date(deliveredAt).toLocaleString()}`)).toBeTruthy()
        expect(screen.getByText('Rejected: Store could not fulfill the order')).toBeTruthy()
    })

    it.each([
        ['delivered', ORDERSTATUS.DELIVERED, null, true],
        ['canceled', ORDERSTATUS.CANCELED, null, true],
        ['ready', ORDERSTATUS.READY, null, false],
        ['already refunded', ORDERSTATUS.DELIVERED, { amount: 5, refundedAt: deliveredAt, reason: 'Prior refund' }, false],
    ])('offers refunds to operators only for eligible %s orders', (_label, status, refund, canRefund) => {
        renderLine({
            order: createOrder({ status, refund }),
            role: ROLE.OPERATOR,
            onRefund: vi.fn(),
        })

        expect(Boolean(screen.queryByRole('button', { name: 'Refund order' }))).toBe(canRefund)
    })

    it('submits the entered refund values', async () => {
        const onRefund = vi.fn().mockResolvedValue(true)
        renderLine({ order: createOrder(), role: ROLE.OPERATOR, onRefund })

        fireEvent.click(screen.getByRole('button', { name: 'Refund order' }))
        const amountInput = screen.getByLabelText(/Refund amount/)
        expect(amountInput.value).toBe('19.5')
        fireEvent.change(amountInput, { target: { value: '5.25' } })
        fireEvent.change(screen.getByLabelText('Reason'), { target: { value: 'Missing item' } })
        fireEvent.click(screen.getByRole('button', { name: 'Confirm refund' }))

        await waitFor(() => expect(onRefund).toHaveBeenCalledWith(52, 5.25, 'Missing item'))
        expect(screen.getByRole('button', { name: 'Confirm refund' })).toBeTruthy()
    })

    it('keeps the refund form open when the refund callback reports failure', async () => {
        const onRefund = vi.fn().mockResolvedValue(false)
        renderLine({ order: createOrder(), role: ROLE.OPERATOR, onRefund })

        fireEvent.click(screen.getByRole('button', { name: 'Refund order' }))
        fireEvent.change(screen.getByLabelText('Reason'), { target: { value: 'Missing item' } })
        fireEvent.click(screen.getByRole('button', { name: 'Confirm refund' }))

        await waitFor(() => expect(onRefund).toHaveBeenCalledWith(52, 19.5, 'Missing item'))
        expect(screen.getByRole('button', { name: 'Confirm refund' })).toBeTruthy()
    })
})
