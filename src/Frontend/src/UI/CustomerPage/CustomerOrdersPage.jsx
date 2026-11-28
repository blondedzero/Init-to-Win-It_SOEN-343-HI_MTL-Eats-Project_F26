import { useCallback, useEffect, useState } from 'react'
import { cancelOwnOrder, getCustomerActiveOrders, ORDERSTATUS } from '../../api/Orders'
import { getCourierRating } from '../../api/Reviews'
import { logSuccess } from '../Console/consoleLog'

const money = (value) => `$${Number(value).toFixed(2)}`

function courierRatingLabel(courierRating) {
  if (!courierRating || courierRating.loading) return 'Loading courier rating...'
  if (courierRating.error) return `Average rating unavailable: ${courierRating.error}`

  const { averageScore, maxScore, ratingCount } = courierRating.summary
  if (averageScore == null) return 'Courier rating: Not rated yet'
  return `Courier rating: ${averageScore.toFixed(1)} / ${maxScore} (${ratingCount} ratings)`
}

export default function CustomerOrdersPage() {
  const [orders, setOrders] = useState([])
  const [courierRatings, setCourierRatings] = useState({})
  const [error, setError] = useState('')
  const [busyOrderId, setBusyOrderId] = useState(null)

  const load = useCallback(async () => {
    try {
      const activeOrders = await getCustomerActiveOrders()
      setError('')
      setOrders(activeOrders)

      const courierIds = [...new Set(
        activeOrders
          .map((order) => order.courierId)
          .filter((courierId) => courierId != null)
      )]
      setCourierRatings(Object.fromEntries(courierIds.map((courierId) => [courierId, { loading: true }])))

      const ratings = await Promise.all(courierIds.map(async (courierId) => {
        try {
          return [courierId, { summary: await getCourierRating(courierId) }]
        } catch (err) {
          return [courierId, { error: err.message || 'Could not load courier rating' }]
        }
      }))
      setCourierRatings(Object.fromEntries(ratings))
    } catch (err) {
      setError(err.message)
    }
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
          {order.courierId == null ? (
            <p className="dash-muted">Courier: Not assigned yet</p>
          ) : (
            <>
              <p className="dash-muted">
                Courier: {order.courierName || `Courier #${order.courierId}`}
              </p>
              <p className="dash-muted">{courierRatingLabel(courierRatings[order.courierId])}</p>
            </>
          )}
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
