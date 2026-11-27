import { useOutletContext } from "react-router-dom";
import { getOrderHistory, refundOrder } from "../../api/PRC/PRCRequests";
import { getRatingScale, rateOrder } from "../../api/Reviews";
import { ROLE } from "../../api/EnumRoles";
import { useCallback, useEffect, useState } from "react";
import OrderHistoryLine from "./OrderHistory/OrderHistoryLine";
import './order-history.css'

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
    const [success, setSuccess] = useState('')
    const [isHistoryLoaded, setIsHistoryLoaded] = useState(false)
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
          setIsHistoryLoaded(true)
          console.log(orderHistoryResult.value)
        } else {
          errors.push(orderHistoryResult.reason.message)
        }
    
        setError(errors.join('; '))
      }, [])

    async function handleRefund(orderId, amount, reason) {
        setError('')
        setSuccess('')
        try {
            await refundOrder(orderId, amount, reason)
            await load()
            setSuccess(`Order #${orderId} refunded successfully.`)
            return true
        } catch (err) {
            setError(err.message)
            return false
        }
    }

    async function handleRate(orderId, storeScore, courierScore) {
        setError('')
        setSuccess('')
        try {
            await rateOrder(orderId, storeScore, courierScore)
            await load()
            setSuccess(`Order #${orderId} rated successfully.`)
            return true
        } catch (err) {
            setError(err.message)
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
            {success && <p className="dash-success">{success}</p>}
            {isHistoryLoaded ? 
              orderHistory.length > 0? orderHistory.map(order => <OrderHistoryLine key={order.orderId} order={order} role={user.role} onRefund={handleRefund} onRate={handleRate} ratingScale={ratingScale}/>)
              :<>Order history is empty.</>
            :<>Loading order hisory...</>}
        </div>
        </>
    )
    
}