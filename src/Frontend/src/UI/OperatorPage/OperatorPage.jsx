import { useCallback, useEffect, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { logSuccess } from '../Console/consoleLog'
import { OperatorCourierCard, OperatorOrderCard, OperatorStoreCard } from './Cards'
import { getAllOpenAndPausedStores, getAllOrdersReady, getAllCouriersOnDuty, getAllStores } from '../../api/OperatorRequests'
import { STORESTATUS } from '../../api/Store'

export const TABS = {
  OVERVIEW:'OVERVIEW',
  STORES:'STORES',
  ORDERS:'ORDERS'
}

export default function OperatorPage() {
  const { user } = useOutletContext()
  const [ordersReady, setOrdersReady] = useState([])
  const [stores, setStores] = useState([])
  const [couriersOnDuty, setCouriersOnDuty] = useState([])
  const [error, setError] = useState('')
  const [tab, setTab] = useState(TABS.OVERVIEW)

  const load = useCallback(async() => {
    const [ordersResult, storesResult, couriersResult] = await Promise.allSettled([
      getAllOrdersReady(),
      getAllStores(),
      getAllCouriersOnDuty()
    ])
    const errors = []

    if (ordersResult.status === 'fulfilled') {
      setOrdersReady(ordersResult.value)
    } else {
      errors.push(ordersResult.reason.message)
    }

    if (storesResult.status === 'fulfilled') {
      setStores(storesResult.value)
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
  
  async function handleOperation(operation, ...args) {
    try {
      await operation(...args);
      await load();
    }
    catch(err) {
      console.log(err.message)
      setError(err.message)
    }
    
  }

  return (
    <>
      <div className="dash-title-row">
        <h2 className="dash-title">{user.storeName}</h2>
        <button type="button" className="dash-button dash-button-light" onClick={load}>Refresh</button>
      </div>
      {error && <p className="dash-error">{error}</p>}

      <div className='tab-selector'>
        <div className='tab-selector-button' onClick={() => setTab(TABS.OVERVIEW)}>Overview</div>
        <div className='tab-selector-button' onClick={() => setTab(TABS.STORES)}>Stores</div>
        <div className='tab-selector-button' onClick={() => setTab(TABS.ORDERS)}>Orders</div>
      </div>
      <div className='tab-container'>
        {tab == TABS.OVERVIEW && <OverviewTab 
          ordersReady={ordersReady}
          stores={stores}
          couriersOnDuty={couriersOnDuty}/>}

        {tab == TABS.STORES && <ManageStoresTab stores={stores} handleOperation={handleOperation}/>}

        {tab == TABS.ORDERS && <div>Manage stalled orders</div>}
      </div>
    </>
  )
}

function OverviewTab({ordersReady, stores, couriersOnDuty}) {
  return(
      <>
      <div className="dash-columns">
        <section>
          <h3 className="dash-section-title">All Orders Ready ({ordersReady.length})</h3>
          <p className="dash-muted">All current orders in the system</p>
          {ordersReady.map((order) => (
            <OperatorOrderCard key={order.orderId} order={order}/>
          ))}
        </section>
        
        <section>
          <h3 className="dash-section-title">Open Stores ({stores.length})</h3>
          <p className="dash-muted">All open stores</p>
          {stores.filter(s => s.status !== STORESTATUS.CLOSED).map((store) => (
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

function ManageStoresTab({stores, handleOperation}) {
  const openStores = stores.filter(s => s.status == STORESTATUS.OPEN)
  const pausedStores = stores.filter(s => s.status == STORESTATUS.PAUSED)
  const closedStores = stores.filter(s => s.status == STORESTATUS.CLOSED)

  return(
    <>
      <div className="dash-columns">        
        <section>
          <h3 className="dash-section-title">Open Stores ({openStores.length})</h3>
          <p className="dash-muted">All open stores</p>
          {openStores.sort((a, b) => a.ordersInProgress - b.ordersInProgress).map((store) => (
            <OperatorStoreCard key={store.id} store={store} handleOperation={handleOperation} tab={TABS.OVERVIEW}/>
          ))}
        </section>

        <section>
          <h3 className="dash-section-title">Paused Stores ({pausedStores.length})</h3>
          <p className="dash-muted">All paused stores</p>
          {pausedStores.map((store) => (
            <OperatorStoreCard key={store.id} store={store} handleOperation={handleOperation} tab={TABS.OVERVIEW}/>
          ))}
        </section>

        <section>
          <h3 className="dash-section-title">Closed Stores ({closedStores.length})</h3>
          <p className="dash-muted">All closed stores</p>
          {closedStores.map((store) => (
            <OperatorStoreCard key={store.id} store={store} handleOperation={handleOperation} tab={TABS.OVERVIEW}/>
          ))}
        </section>
      </div>
    </>
  )
}
