import { useOutletContext } from "react-router-dom";
import { getOrderHistory } from "../../api/PRC/OrderHistory";
import { useCallback, useEffect, useState } from "react";
import OrderHistoryLine from "./OrderHistoryLine";
import './order-history.css'
import { ROLE } from "../../api/EnumRoles";

function getLastActionTimestamp(order) {
    return [order.placedAt, order.deliveredAt, order.canceledAt].reduce((latest, timestamp) => {
        const parsed = Date.parse(timestamp)
        return Number.isNaN(parsed) ? latest : Math.max(latest, parsed)
    }, 0)
}

export default function OrderHistoryPage() {
    const { user } = useOutletContext()
    const [orderHistory, setOrderHistory] = useState([])
    const [error, setError] = useState('')

    const load = useCallback(async() => {
        const [orderHistoryResult] = await Promise.allSettled([
          getOrderHistory(user.userId)
        ])
        const errors = []
    
        if (orderHistoryResult.status === 'fulfilled') {
          setOrderHistory([...orderHistoryResult.value].sort(
            (a, b) => getLastActionTimestamp(b) - getLastActionTimestamp(a)
          ))
          console.log(orderHistoryResult.value)
        } else {
          errors.push(orderHistoryResult.reason.message)
        }
    
        setError(errors.join('; '))
      }, [])
    
      useEffect(() => {
        load()
      }, [load])

    return(
        <>
        <div className="history-container">
            <h1>Order history</h1>
            {orderHistory.length > 0? orderHistory.map(order => <OrderHistoryLine key={order.orderId} order={order}/>)
            :<>Order history is empty.</>}
        </div>
        </>
    )
    
}