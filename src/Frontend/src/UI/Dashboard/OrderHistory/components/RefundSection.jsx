import { useState } from 'react'
import { money } from '../../../../utils/format'

export default function RefundSection({ order, onRefund }) {
    const [showRefundForm, setShowRefundForm] = useState(false)
    const [refundAmount, setRefundAmount] = useState(String(order.totalCost))
    const [refundReason, setRefundReason] = useState('')
    const [submitting, setSubmitting] = useState(false)

    async function submitRefund(event) {
        event.preventDefault()
        setSubmitting(true)
        try {
            const refunded = await onRefund(order.orderId, Number(refundAmount), refundReason)
            if (refunded) setShowRefundForm(false)
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <>
            <button type="button" className="refund-toggle" onClick={() => setShowRefundForm((shown) => !shown)}>
                {showRefundForm ? 'Hide refund form' : 'Refund order'}
            </button>
            {showRefundForm && <form className="refund-form" onSubmit={submitRefund}>
                <label>
                    Refund amount (max {money(order.totalCost)})
                    <input
                        type="number"
                        min="0.01"
                        max={order.totalCost}
                        step="0.01"
                        required
                        value={refundAmount}
                        onChange={(event) => setRefundAmount(event.target.value)}
                    />
                </label>
                <label>
                    Reason
                    <textarea
                        maxLength="500"
                        required
                        value={refundReason}
                        onChange={(event) => setRefundReason(event.target.value)}
                    />
                </label>
                <button type="submit" disabled={submitting}>
                    {submitting ? 'Processing...' : 'Confirm refund'}
                </button>
            </form>}
        </>
    )
}
