import { useCallback, useEffect, useState } from 'react'
import {
  COURIERSTATUS,
  acceptDelivery,
  cancelDeliveryAssignment,
  changeCourierStatus,
  confirmDelivery,
  confirmPickup,
  declineDelivery,
  getCourierDashboard,
} from '../../api/Courier'
import { logSuccess } from '../Console/consoleLog'
import CourierRating from './CourierRating'

const money = (value) => `$${Number(value).toFixed(2)}`

export default function CourierPage() {
  const [dashboard, setDashboard] = useState(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [busyOrderId, setBusyOrderId] = useState(null)
  const [statusBusy, setStatusBusy] = useState(false)

  const load = useCallback(() => {
    return getCourierDashboard()
      .then((data) => {
        setError('')
        setDashboard(data)
      })
      .catch((err) => setError(err.message))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const respondToOffer = async (orderId, respond) => {
    setBusyOrderId(orderId)
    setError('')
    setSuccess('')
    try {
      const result = await respond(orderId)
      logSuccess(`${result.message} (event #${result.eventId})`)
      setSuccess(result.message)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyOrderId(null)
    }
  }

  const toggleDuty = async () => {
    const nextStatus = dashboard.status === COURIERSTATUS.OFFLINE
      ? COURIERSTATUS.IDLE
      : COURIERSTATUS.OFFLINE
    setStatusBusy(true)
    setError('')
    setSuccess('')
    try {
      const updatedDashboard = await changeCourierStatus(nextStatus)
      setDashboard(updatedDashboard)
      const message = nextStatus === COURIERSTATUS.IDLE ? 'You are now online.' : 'You are now offline.'
      setSuccess(message)
      logSuccess(message)
    } catch (err) {
      setError(err.message)
    } finally {
      setStatusBusy(false)
    }
  }

  const hasActiveDelivery = dashboard?.status === COURIERSTATUS.CARRYING
    || (dashboard?.status === COURIERSTATUS.ASSIGNED
      && dashboard.orders.some((order) => order.courierAccepted))
  const hasPendingOffer = dashboard?.status === COURIERSTATUS.IDLE
    && dashboard.orders.some((order) => !order.courierAccepted)

  return (
    <>
      <div className="dash-title-row">
        <h2 className="dash-title">Courier orders</h2>
        {dashboard && <span className="dash-badge">{dashboard.status}</span>}
        {dashboard && (
          <button
            type="button"
            className={`dash-button ${dashboard.status === COURIERSTATUS.OFFLINE ? '' : 'dash-button-light'}`}
            onClick={toggleDuty}
            disabled={statusBusy || hasActiveDelivery}
            title={hasActiveDelivery
              ? 'Complete your accepted delivery before going offline.'
              : hasPendingOffer ? 'Going offline will withdraw your pending offer.' : undefined}
          >
            {statusBusy ? 'Updating…' : dashboard.status === COURIERSTATUS.OFFLINE ? 'Go online' : 'Go offline'}
          </button>
        )}
        <button type="button" className="dash-button dash-button-light" onClick={load}>
          Refresh
        </button>
      </div>
      <p className="dash-muted">Manage your assigned delivery orders.</p>
      <CourierRating />
      {hasActiveDelivery && (
        <p className="dash-muted">Complete your accepted delivery before going offline.</p>
      )}
      {error && <p className="dash-error" role="alert">{error}</p>}
      {success && <p className="dash-success" role="status">{success}</p>}

      {dashboard && dashboard.orders.length === 0 && !error && (
        <p className="dash-muted">You have no active delivery orders right now.</p>
      )}
      {dashboard?.orders.map((order) => {
        const busy = busyOrderId === order.orderId
        return (
          <article key={order.orderId} className="dash-card">
            <div className="dash-title-row">
              <h3 className="dash-card-title">Order {order.orderId}</h3>
              <span className={`dash-badge dash-badge-${order.status.toLowerCase()}`}>{order.status}</span>
            </div>
            <p className="dash-muted">
              {order.storeName} · placed {new Date(order.placedAt).toLocaleString()}
            </p>
            <p><strong>Deliver to:</strong> {order.deliveryAddress}</p>
            <p><strong>Customer:</strong> {order.customerName}</p>
            <ul className="dash-lines">
              {order.lines.map((line) => (
                <li key={line.productId}>{line.quantity} × {line.name}</li>
              ))}
            </ul>
            <p className="dash-total">Order total {money(order.totalCost)}</p>
            <div className="dash-actions">
              {dashboard.status === COURIERSTATUS.IDLE && !order.courierAccepted && (
                <>
                  <button
                    type="button"
                    className="dash-button"
                    disabled={busy}
                    onClick={() => respondToOffer(order.orderId, acceptDelivery)}
                  >
                    {busy ? 'Updating…' : 'Accept'}
                  </button>
                  <button
                    type="button"
                    className="dash-button dash-button-danger"
                    disabled={busy}
                    onClick={() => respondToOffer(order.orderId, declineDelivery)}
                  >
                    Decline
                  </button>
                </>
              )}
              {dashboard.status === COURIERSTATUS.ASSIGNED && order.courierAccepted && (
                <>
                  <button
                    type="button"
                    className="dash-button"
                    disabled={busy}
                    onClick={() => respondToOffer(order.orderId, confirmPickup)}
                  >
                    {busy ? 'Updating…' : 'Mark picked up'}
                  </button>
                  <button
                    type="button"
                    className="dash-button dash-button-danger"
                    disabled={busy}
                    onClick={() => respondToOffer(order.orderId, cancelDeliveryAssignment)}
                  >
                    Cancel pickup
                  </button>
                </>
              )}
              {dashboard.status === COURIERSTATUS.CARRYING && (
                <button
                  type="button"
                  className="dash-button"
                  disabled={busy}
                  onClick={() => respondToOffer(order.orderId, confirmDelivery)}
                >
                  {busy ? 'Updating…' : 'Mark delivered'}
                </button>
              )}
            </div>
          </article>
        )
      })}
    </>
  )
}