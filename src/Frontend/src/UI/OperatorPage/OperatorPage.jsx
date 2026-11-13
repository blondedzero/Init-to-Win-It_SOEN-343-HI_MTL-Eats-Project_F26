import { useCallback, useEffect, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { getAllOrdersInProgress } from '../../api/Orders'
import { logSuccess } from '../Console/consoleLog'
import { OperatorOrderCard, OperatorStoreCard } from './Cards'
import { getAllOpenAndPausedStores } from '../../api/Store'

export default function OperatorPage() {
  const { user } = useOutletContext()
  const [ordersInProgress, setOrdersInProgress] = useState([])
  const [openAndPausedStores, setOpenAndPausedStores] = useState([])
  const [error, setError] = useState('')

  const load = useCallback(async() => {
    return Promise.all([getAllOrdersInProgress(), getAllOpenAndPausedStores()])
      .then(([ordersInProgress, openAndPausedStores]) => {
        setError('')
        setOrdersInProgress(ordersInProgress)
        setOpenAndPausedStores(openAndPausedStores)
      })
      .catch((err) => setError(err.message))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  return (
    <>
      <div className="dash-title-row">
        <h2 className="dash-title">{user.storeName}</h2>
        <button type="button" className="dash-button dash-button-light" onClick={load}>Refresh</button>
      </div>
      {error && <p className="dash-error">{error}</p>}

      <div className="dash-columns">

        <section>
          <h3 className="dash-section-title">All Orders in Progress ({ordersInProgress.length})</h3>
          <p className="dash-muted">All current orders in the system</p>
          {ordersInProgress.map((order) => (
            <OperatorOrderCard key={order.orderId} order={order}/>
          ))}
        </section>
        
        <section>
          <h3 className="dash-section-title">Open Stores ({openAndPausedStores.length})</h3>
          <p className="dash-muted">All open stores</p>
          {openAndPausedStores.map((store) => (
            <OperatorStoreCard key={store.storeId} store={store}/>
          ))}
        </section>

      </div>
    </>
  )
}
