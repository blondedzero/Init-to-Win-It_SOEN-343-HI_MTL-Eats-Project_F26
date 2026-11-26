import { useOutletContext } from "react-router-dom";
import { getOrderHistory, refundOrder } from "../../api/PRC/PRCRequests";
import { useCallback, useEffect, useState } from "react";
import OrderHistoryLine from "./OrderHistoryLine";
import './order-history.css'
import { ROLE } from "../../api/EnumRoles";

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
    
      useEffect(() => {
        load()
      }, [load])

    return(
        <>
        <div className="history-container">
            <h1>Order history</h1>
            {error && <p className="dash-error">{error}</p>}
            {success && <p className="dash-success">{success}</p>}
            {isHistoryLoaded ? 
              orderHistory.length > 0? orderHistory.map(order => <OrderHistoryLine key={order.orderId} order={order} role={user.role} onRefund={handleRefund}/>)
              :<>Order history is empty.</>
            :<>Loading order hisory...</>}
        </div>
        </>
    )
    
}