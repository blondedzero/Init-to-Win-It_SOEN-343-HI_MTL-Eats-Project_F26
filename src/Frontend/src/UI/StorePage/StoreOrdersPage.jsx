import { useCallback, useEffect, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import {
  acceptOrder,
  getIncomingOrders,
  getOrdersInPreparation,
  getReadyOrders,
  getQueue,
  markReady,
  rejectOrder,
  startPreparing,
} from '../../api/Orders'
import { logSuccess } from '../Console/consoleLog'
import OrderCard, { OrderReadyCard } from './OrderCard'
import StoreStatusControl from './StoreStatusControl'
import StoreNav from './StoreNav'

export default function StoreOrdersPage() {
  const { user } = useOutletContext()
  const [incoming, setIncoming] = useState([])
  const [queue, setQueue] = useState({ size: 0, orders: [] })
  const [preparing, setPreparing] = useState([])
  const [ready, setReady] = useState([])
  const [error, setError] = useState('')
  const [busyOrderId, setBusyOrderId] = useState(null)
  const [rejecting, setRejecting] = useState({})

  const load = useCallback(() => {
    return Promise.all([getIncomingOrders(), getQueue(), getOrdersInPreparation(), getReadyOrders()])
      .then(([incomingOrders, preparationQueue, inPreparation, readyOrders]) => {
        setError('')
        setIncoming(incomingOrders)
        setQueue(preparationQueue)
        setPreparing(inPreparation)
        setReady(readyOrders)
      })
      .catch((err) => setError(err.message))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const stopRejecting = (orderId) => {
    setRejecting((current) => {
      const next = { ...current }
      delete next[orderId]
      return next
    })
  }

  const runCommand = async (orderId, command) => {
    setBusyOrderId(orderId)
    setError('')
    try {
      const result = await command()
      logSuccess(`${result.message} (event #${result.eventId})`)
      stopRejecting(orderId)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyOrderId(null)
    }
  }

  return (
    <>
      <StoreNav />
      <div className="dash-title-row">
        <h2 className="dash-title">{user.storeName}</h2>
        <StoreStatusControl />
        <button type="button" className="dash-button dash-button-light" onClick={load}>Refresh</button>
      </div>
      {error && <p className="dash-error">{error}</p>}

      <div className="dash-columns">
        <section>
          <h3 className="dash-section-title">Incoming ({incoming.length})</h3>
          <p className="dash-muted">Waiting for you to accept or reject.</p>
          {incoming.map((order) => {
            const reason = rejecting[order.orderId]
            const busy = busyOrderId === order.orderId
            return (
              <OrderCard key={order.orderId} order={order}>
                {reason === undefined ? (
                  <div className="dash-actions">
                    <button type="button" className="dash-button" disabled={busy}
                            onClick={() => runCommand(order.orderId, () => acceptOrder(order.orderId))}>
                      Accept
                    </button>
                    <button type="button" className="dash-button dash-button-danger" disabled={busy}
                            onClick={() => setRejecting((current) => ({ ...current, [order.orderId]: '' }))}>
                      Reject…
                    </button>
                  </div>
                ) : (
                  <div className="dash-actions">
                    <input
                      className="dash-input dash-reason"
                      placeholder="Reason for rejecting"
                      value={reason}
                      onChange={(e) => setRejecting((current) => ({ ...current, [order.orderId]: e.target.value }))}
                    />
                    <button type="button" className="dash-button dash-button-danger" disabled={busy || !reason.trim()}
                            onClick={() => runCommand(order.orderId, () => rejectOrder(order.orderId, reason))}>
                      Confirm reject
                    </button>
                    <button type="button" className="dash-button dash-button-light"
                            onClick={() => stopRejecting(order.orderId)}>
                      Cancel
                    </button>
                  </div>
                )}
              </OrderCard>
            )
          })}
        </section>

        <section>
          <h3 className="dash-section-title">Preparation queue ({queue.size})</h3>
          <p className="dash-muted">Accepted orders, first in, first out.</p>
          {queue.orders.map((order, index) => (
            <OrderCard key={order.orderId} order={order} position={index + 1}>

              <div className="dash-actions">
                <button type="button" className={`dash-button ${index === 0 ? '' : 'dash-button-light'}`}
                        disabled={busyOrderId === order.orderId}
                        onClick={() => runCommand(order.orderId, () => startPreparing(order.orderId))}>
                  Start preparing
                </button>
              </div>
            </OrderCard>
          ))}
        </section>

        <section>
          <h3 className="dash-section-title">Preparing ({preparing.length})</h3>
          <p className="dash-muted">Mark each one ready when it is packed.</p>
          {preparing.map((order) => (
            <OrderCard key={order.orderId} order={order}>
              <div className="dash-actions">
                <button type="button" className="dash-button" disabled={busyOrderId === order.orderId}
                        onClick={() => runCommand(order.orderId, () => markReady(order.orderId))}>
                  Mark ready
                </button>
              </div>
            </OrderCard>
          ))}
        </section>

        <section>
          <h3 className="dash-section-title">Ready ({ready.length})</h3>
          <p className="dash-muted">Orders ready for pickup.</p>
          {ready.map((order) => (
            <OrderReadyCard key={order.orderId} order={order}/>
          ))}
        </section>
      </div>
    </>
  )
}
