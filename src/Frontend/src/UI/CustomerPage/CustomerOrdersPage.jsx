import { useCallback, useEffect, useState } from 'react'
import { cancelOwnOrder, getCustomerActiveOrders, ORDERSTATUS } from '../../api/Orders'
import { logSuccess } from '../Console/consoleLog'

const money = (value) => `$${Number(value).toFixed(2)}`

export default function CustomerOrdersPage() {
  const [orders, setOrders] = useState([])
  const [error, setError] = useState('')
  const [busyOrderId, setBusyOrderId] = useState(null)

  const load = useCallback(() => {
    return getCustomerActiveOrders()
      .then((activeOrders) => {
        setError('')
        setOrders(activeOrders)
      })
      .catch((err) => setError(err.message))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const cancel = async (orderId) => {
    setBusyOrderId(orderId)
    setError('')
    try {
      const result = await cancelOwnOrder(orderId)
      logSuccess(`${result.message} (event #${result.eventId})`)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyOrderId(null)
    }
  }

  return (
    <>
      <div className="dash-title-row">
        <h2 className="dash-title">My orders</h2>
        <button type="button" className="dash-button dash-button-light" onClick={load}>Refresh</button>
      </div>
      {error && <p className="dash-error">{error}</p>}
      <p className="dash-muted">Orders in progress. Finished orders move to Order History.</p>

      {orders.length === 0 && <p className="dash-muted">You have no orders in progress.</p>}
      {orders.map((order) => (
        <article key={order.orderId} className="dash-card">
          <div className="dash-title-row">
            <h4 className="dash-card-title">Order {order.orderId}</h4>
            <span className={`dash-badge dash-badge-${order.status.toLowerCase()}`}>{order.status}</span>
          </div>
          <p className="dash-muted">
            {order.storeName} · placed {new Date(order.placedAt).toLocaleString()}
          </p>
          <ul className="dash-lines">
            {order.lines.map((line) => (
              <li key={line.productId}>{line.quantity} × {line.name}</li>
            ))}
          </ul>
          <p className="dash-total">Total {money(order.totalCost)}</p>
          {/* Customers may cancel only while the order is still placed */}
          {order.status === ORDERSTATUS.PLACED && (
            <div className="dash-actions">
              <button type="button" className="dash-button dash-button-danger"
                      disabled={busyOrderId === order.orderId}
                      onClick={() => cancel(order.orderId)}>
                Cancel order
              </button>
            </div>
          )}
        </article>
      ))}
    </>
  )
}
