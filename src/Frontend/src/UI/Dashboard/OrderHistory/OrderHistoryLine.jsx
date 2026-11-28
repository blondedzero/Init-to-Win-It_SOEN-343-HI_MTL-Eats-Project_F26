import { ROLE } from '../../../api/EnumRoles'
import OrderDetails from './components/OrderDetails'
import OrderHeader from './components/OrderHeader'
import OrderTimestamps from './components/OrderTimestamps'
import PricingSummary from './components/PricingSummary'
import RatingDetails from './components/RatingDetails'
import RatingSection from './components/RatingSection'
import RefundDetails from './components/RefundDetails'
import RefundSection from './components/RefundSection'
import { canRate, canRefund } from './orderRules'
import { COMPONENT } from '../msgFor'

export default function OrderHistoryLine({ order, role, onRefund, onRate, ratingScale, activeForm, onToggleForm, feedback }) {
    const isCustomer = role == ROLE.CUSTOMER
    const isOperator = role == ROLE.OPERATOR
    const canRefundOrder = canRefund(order, role)
    const canRateOrder = canRate(order, role)
    const refundFormOpen = activeForm?.orderId === order.orderId && activeForm?.component === COMPONENT.refund
    const ratingFormOpen = activeForm?.orderId === order.orderId && activeForm?.component === COMPONENT.review
    const refundFeedback = feedback?.orderId === order.orderId && feedback.component === COMPONENT.refund
        ? feedback
        : null
    const ratingFeedback = feedback?.orderId === order.orderId && feedback.component === COMPONENT.review
        ? feedback
        : null

    return (
        <div className="history-line">
            <OrderHeader order={order} isCustomer={isCustomer} />
            <OrderTimestamps order={order} />
            <OrderDetails order={order} />
            {order.pricing && <PricingSummary pricing={order.pricing} isOperator={isOperator} />}
            {order.refund && <RefundDetails refund={order.refund} totalCost={order.totalCost} isOperator={isOperator} />}
            {canRefundOrder && <RefundSection
                order={order}
                onRefund={onRefund}
                isOpen={refundFormOpen}
                onToggle={() => onToggleForm(order.orderId, COMPONENT.refund)}
                error={refundFeedback?.error}
                success={refundFeedback?.success}
            />}
            {refundFeedback && (!canRefundOrder || !refundFormOpen) && (refundFeedback.error || refundFeedback.success) && (
                <p role={refundFeedback.error ? 'alert' : 'status'} className={`dash-${refundFeedback.error ? 'error' : 'success'} history-action-feedback`}>
                    {refundFeedback.error || refundFeedback.success}
                </p>
            )}
            {order.rating && <RatingDetails rating={order.rating} maxScore={ratingScale?.maxScore} />}
            {canRateOrder && ratingScale && <RatingSection
                order={order}
                scale={ratingScale}
                onRate={onRate}
                isOpen={ratingFormOpen}
                onToggle={() => onToggleForm(order.orderId, COMPONENT.review)}
                error={ratingFeedback?.error}
                success={ratingFeedback?.success}
            />}
            {ratingFeedback && (!canRateOrder || !ratingFormOpen) && (ratingFeedback.error || ratingFeedback.success) && (
                <p role={ratingFeedback.error ? 'alert' : 'status'} className={`dash-${ratingFeedback.error ? 'error' : 'success'} history-action-feedback`}>
                    {ratingFeedback.error || ratingFeedback.success}
                </p>
            )}
        </div>
    )
}
