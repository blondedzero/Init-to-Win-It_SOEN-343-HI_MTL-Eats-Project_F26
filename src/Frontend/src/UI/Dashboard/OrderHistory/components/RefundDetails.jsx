import { formatDateTime, money } from '../../../../utils/format'

export default function RefundDetails({ refund, totalCost, isOperator }) {
    return (
        <div className="refund-details">
            <strong>Refund: {money(refund.amount)}</strong>
            {isOperator && <span>Original charge unchanged: {money(totalCost)}</span>}
            <span>Refunded at: {formatDateTime(refund.refundedAt)}</span>
            <span>Reason: {refund.reason}</span>
            {isOperator && <span>Operator #{refund.operatorId}</span>}
        </div>
    )
}
