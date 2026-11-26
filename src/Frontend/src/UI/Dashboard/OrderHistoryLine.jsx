import { useState } from "react";
import { ORDERSTATUS } from "../../api/Orders";
import { ROLE } from "../../api/EnumRoles";

const money = (value) => `$${Number(value).toFixed(2)}`

export default function OrderHistoryLine({order, role, onRefund}) {
    const isCustomer = role == ROLE.CUSTOMER
    const isOperator = role == ROLE.OPERATOR
    const canRefund = isOperator
        && [ORDERSTATUS.DELIVERED, ORDERSTATUS.CANCELED].includes(order.status)
        && !order.refund
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

    return(
        <div className="history-line">
            <div className="order-id">Order #{order.orderId}</div>
            {/* Customers see which store the order was from instead of their own name */}
            <div className="customer">{isCustomer ? order.storeName : order.customerName}</div>
            <div className={`status ${String(order.status).toLowerCase()}`}>{order.status}</div>
            {[ORDERSTATUS.DELIVERED, ORDERSTATUS.REFUNDED].includes(order.status) && order.deliveredAt &&
                <div className="timestamp">Delivered at: {new Date(order.deliveredAt).toLocaleString()}</div>}
            {[ORDERSTATUS.CANCELED, ORDERSTATUS.REFUNDED].includes(order.status) && order.canceledAt &&
                <div className="timestamp">Canceled at: {new Date(order.canceledAt).toLocaleString()}</div>}
            {/* Order.markRejected() records the rejection time in canceledAt */}
            {order.status == ORDERSTATUS.REJECTED && <div className="timestamp">Rejected at: {new Date(order.canceledAt).toLocaleString()}</div>}
            <div className="details">
                <span>{order.lines.map((line) => `${line.quantity} × ${line.name}`).join(', ')}</span>
                <span className="total">Total {money(order.totalCost)}</span>
                {order.status == ORDERSTATUS.REJECTED && order.rejectionReason &&
                    <span className="reason">Rejected: {order.rejectionReason}</span>}
            </div>
            {order.pricing && <div className="pricing-details">
                <strong>Pricing summary</strong>
                <span>Items subtotal: {money(order.pricing.subtotal)}</span>
                <span>Small order fee: {money(order.pricing.smallOrderFee)}</span>
                <span>Service fee: {money(order.pricing.serviceFee)}</span>
                <span>Delivery fee: {money(order.pricing.deliveryFee)}</span>
                <span>Tax: {money(order.pricing.taxAmount)}</span>
                {isOperator && <span>PRC rule version: {order.pricing.ruleVersion ?? 'Unavailable'}</span>}
            </div>}
            {order.refund && <div className="refund-details">
                <strong>Refund: {money(order.refund.amount)}</strong>
                {isOperator && <span>Original charge unchanged: {money(order.totalCost)}</span>}
                <span>Refunded at: {new Date(order.refund.refundedAt).toLocaleString()}</span>
                <span>Reason: {order.refund.reason}</span>
                {isOperator && <span>Operator #{order.refund.operatorId}</span>}
            </div>}
            {canRefund && <>
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
            </>}
        </div>
    )
}
