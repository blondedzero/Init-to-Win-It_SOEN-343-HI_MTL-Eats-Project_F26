import { useOutletContext } from "react-router-dom";
import { getOrderHistory, refundOrder } from "../../api/PRC/PRCRequests";
import { getRatingScale, rateOrder } from "../../api/Reviews";
import { ROLE } from "../../api/EnumRoles";
import { useCallback, useEffect, useState } from "react";
import OrderHistoryLine from "./OrderHistory/OrderHistoryLine";
import './order-history.css'
import { COMPONENT } from "./msgFor";

function getLastActionTimestamp(order) {
    return [order.placedAt, order.deliveredAt, order.canceledAt, order.refund?.refundedAt].reduce((latest, timestamp) => {
        const parsed = Date.parse(timestamp)
        return Number.isNaN(parsed) ? latest : Math.max(latest, parsed)
    }, 0)
}

export default function OrderHistoryPage() {
    const { user } = useOutletContext()
    const [orderHistory, setOrderHistory] = useState([])
    const [error, setError] = useState('')
    const [feedback, setFeedback] = useState(null)
    const [activeForm, setActiveForm] = useState(null)
    const [isLoaded, setIsLoaded] = useState(false)
    const [ratingScale, setRatingScale] = useState(null)

    const load = useCallback(async() => {
        const [orderHistoryResult] = await Promise.allSettled([
          getOrderHistory()
        ])
        const errors = []
    
        if (orderHistoryResult.status === 'fulfilled') {
          setOrderHistory([...orderHistoryResult.value].sort(
            (a, b) => getLastActionTimestamp(b) - getLastActionTimestamp(a)
          ))
          setIsLoaded(true)
          console.log(orderHistoryResult.value)
        } else {
          errors.push(orderHistoryResult.reason.message)
        }
    
        setError(errors.join('; '))
      }, [])

    function toggleForm(orderId, component) {
        setFeedback(null)
        setActiveForm((current) =>
            current?.orderId === orderId && current.component === component
                ? null
                : { orderId, component }
        )
    }

    async function handleRefund(orderId, amount, reason) {
        const actionFeedback = { orderId, component: COMPONENT.refund, error: '', success: '' }
        setFeedback(actionFeedback)
        try {
            await refundOrder(orderId, amount, reason)
            await load()
            setFeedback({ ...actionFeedback, success: `Order #${orderId} refunded successfully.` })
            return true
        } catch (err) {
            setFeedback({ ...actionFeedback, error: err.message })
            return false
        }
    }

    async function handleRate(orderId, storeScore, courierScore) {
        const actionFeedback = { orderId, component: COMPONENT.review, error: '', success: '' }
        setFeedback(actionFeedback)
        try {
            await rateOrder(orderId, storeScore, courierScore)
            await load()
            setFeedback({ ...actionFeedback, success: `Order #${orderId} rated successfully.` })
            return true
        } catch (err) {
            setFeedback({ ...actionFeedback, error: err.message })
            return false
        }
    }

      useEffect(() => {
        load()
      }, [load])

      useEffect(() => {
        if (user.role !== ROLE.CUSTOMER) return
        getRatingScale().then(setRatingScale).catch((err) => setError(err.message))
      }, [user.role])

    return(
        <>
        <div className="history-container">
            <h1>Order history</h1>
            {error && <p className="dash-error">{error}</p>}
            {isLoaded ?
              orderHistory.length > 0? orderHistory.map(order => <OrderHistoryLine key={order.orderId} order={order} role={user.role} onRefund={handleRefund} onRate={handleRate} ratingScale={ratingScale} activeForm={activeForm} onToggleForm={toggleForm} feedback={feedback}/>)
              :<>Order history is empty.</>
            :<i>Loading order history...</i>}
        </div>
        </>
    )
    
}