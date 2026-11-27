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

export default function OrderHistoryLine({ order, role, onRefund, onRate, ratingScale }) {
    const isCustomer = role == ROLE.CUSTOMER
    const isOperator = role == ROLE.OPERATOR

    return (
        <div className="history-line">
            <OrderHeader order={order} isCustomer={isCustomer} />
            <OrderTimestamps order={order} />
            <OrderDetails order={order} />
            {order.pricing && <PricingSummary pricing={order.pricing} isOperator={isOperator} />}
            {order.refund && <RefundDetails refund={order.refund} totalCost={order.totalCost} isOperator={isOperator} />}
            {canRefund(order, role) && <RefundSection order={order} onRefund={onRefund} />}
            {order.rating && <RatingDetails rating={order.rating} maxScore={ratingScale?.maxScore} />}
            {canRate(order, role) && ratingScale && <RatingSection order={order} scale={ratingScale} onRate={onRate} />}
        </div>
    )
}
