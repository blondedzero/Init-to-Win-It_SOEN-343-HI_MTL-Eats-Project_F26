import { useState } from 'react'
import { money } from '../../../../utils/format'

export default function RefundSection({ order, onRefund, isOpen, onToggle, error, success }) {
    const [refundAmount, setRefundAmount] = useState(String(order.totalCost))
    const [refundReason, setRefundReason] = useState('')
    const [submitting, setSubmitting] = useState(false)

    async function submitRefund(event) {
        event.preventDefault()
        setSubmitting(true)
        try {
            await onRefund(order.orderId, Number(refundAmount), refundReason)
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <>
            <button type="button" className="refund-toggle" onClick={onToggle}>
                {isOpen ? 'Hide refund form' : 'Refund order'}
            </button>
            {isOpen &&
            <form className="refund-form" onSubmit={submitRefund}>
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
                {error && <p role="alert" className="dash-error">{error}</p>}
                {success && <p role="status" className="dash-success">{success}</p>}
            </form>}
        </>
    )
}
