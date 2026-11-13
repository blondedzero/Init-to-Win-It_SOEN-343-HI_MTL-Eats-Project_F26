import { useCallback, useEffect, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { logSuccess } from '../Console/consoleLog'
import { OperatorCourierCard, OperatorOrderCard, OperatorStoreCard } from './Cards'
import { getAllOpenAndPausedStores, getAllOrdersReady, getAllCouriersOnDuty} from '../../api/OperatorRequests'

export default function OperatorPage() {
  const { user } = useOutletContext()
  const [ordersReady, setOrdersReady] = useState([])
  const [openAndPausedStores, setOpenAndPausedStores] = useState([])
  const [couriersOnDuty, setCouriersOnDuty] = useState([])
  const [error, setError] = useState('')

  const load = useCallback(async() => {
    const [ordersResult, storesResult, couriersResult] = await Promise.allSettled([
      getAllOrdersReady(),
      getAllOpenAndPausedStores(),
      getAllCouriersOnDuty()
    ])
    const errors = []

    if (ordersResult.status === 'fulfilled') {
      setOrdersReady(ordersResult.value)
    } else {
      errors.push(ordersResult.reason.message)
    }

    if (storesResult.status === 'fulfilled') {
      setOpenAndPausedStores(storesResult.value)
    } else {
      errors.push(storesResult.reason.message)
    }

    if (couriersResult.status === 'fulfilled') {
      setCouriersOnDuty(couriersResult.value)
    } else {
      errors.push(couriersResult.reason.message)
    }

    setError(errors.join('; '))
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
          <h3 className="dash-section-title">All Orders Ready ({ordersReady.length})</h3>
          <p className="dash-muted">All current orders in the system</p>
          {ordersReady.map((order) => (
            <OperatorOrderCard key={order.orderId} order={order}/>
          ))}
        </section>
        
        <section>
          <h3 className="dash-section-title">Open Stores ({openAndPausedStores.length})</h3>
          <p className="dash-muted">All open stores</p>
          {openAndPausedStores.map((store) => (
            <OperatorStoreCard key={store.id} store={store}/>
          ))}
        </section>

        <section>
          <h3 className="dash-section-title">Couriers on Duty ({couriersOnDuty.length})</h3>
          <p className="dash-muted">All couriers on duty</p>
          {couriersOnDuty.map((courier) => (
            <OperatorCourierCard key={courier.id} courier={courier}/>
          ))}
        </section>

      </div>
    </>
  )
}
