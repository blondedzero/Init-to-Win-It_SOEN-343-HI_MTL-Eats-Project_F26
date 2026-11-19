import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useOutletContext } from 'react-router-dom'
import { logSuccess } from '../Console/consoleLog'
import { OperatorCourierCard, OperatorOrderCard, OperatorStoreCard } from './Cards'
import { getAllOpenAndPausedStores, getAllOrdersReady, getAllCouriersOnDuty, getAllStores, getAllOrdersInProgress } from '../../api/OperatorRequests'
import { STORESTATUS } from '../../api/Store'
import { ROLE } from '../../api/EnumRoles'
import { ORDERSTATUS } from '../../api/Orders'

export const TABS = {
  OVERVIEW:'OVERVIEW',
  STORES:'STORES',
  ORDERS:'ORDERS'
}

export default function OperatorPage() {
  const { user } = useOutletContext()
  const [ordersInProgress, setOrdersInProgress] = useState([])
  const [stores, setStores] = useState([])
  const [couriersOnDuty, setCouriersOnDuty] = useState([])
  const [error, setError] = useState('')
  const [tab, setTab] = useState(TABS.OVERVIEW)
  const navigate = useNavigate()

  const load = useCallback(async() => {
    const [ordersResult, storesResult, couriersResult] = await Promise.allSettled([
      getAllOrdersInProgress(),
      getAllStores(),
      getAllCouriersOnDuty()
    ])
    const errors = []

    if (ordersResult.status === 'fulfilled') {
      setOrdersInProgress(ordersResult.value)
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
    if (user.role != ROLE.OPERATOR) {
      setError('Access Denied')
      setTimeout(()=>navigate('/'), 1500)
      return
    }
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

      <div className="operator-tabs" role="group" aria-label="Operator sections">
        <button type="button" className="operator-tab" aria-pressed={tab === TABS.OVERVIEW} onClick={() => setTab(TABS.OVERVIEW)}>Overview</button>
        <button type="button" className="operator-tab" aria-pressed={tab === TABS.STORES} onClick={() => setTab(TABS.STORES)}>Stores</button>
        <button type="button" className="operator-tab" aria-pressed={tab === TABS.ORDERS} onClick={() => setTab(TABS.ORDERS)}>Orders & Couriers</button>
      </div>
      <div className='tab-container'>
        {tab == TABS.OVERVIEW && <OverviewTab 
          ordersReady={ordersInProgress.filter(order => order.status == ORDERSTATUS.READY)}
          stores={stores}
          couriersOnDuty={couriersOnDuty}/>}

        {tab == TABS.STORES && <ManageStoresTab stores={stores} handleOperation={handleOperation}/>}

        {tab == TABS.ORDERS && <ManageOrdersTab orders={ordersInProgress} handleOperation={handleOperation}/>}
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
          <h3 className="dash-section-title">Open Stores ({stores.filter(s => s.status !== STORESTATUS.CLOSED).length})</h3>
          <p className="dash-muted">All open and paused stores</p>
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

function ManageOrdersTab({orders, handleOperation}) {
  const ordersReady = orders.filter(order => order.status == ORDERSTATUS.READY)
  const ordersAccepted = orders.filter(order => order.status == ORDERSTATUS.ACCEPTED)
  const ordersPreparing = orders.filter(order => order.status == ORDERSTATUS.PREPARING)

    return(
      <div className="dash-columns">
          <section>
            <h3 className="dash-section-title">Orders Accepted ({ordersAccepted.length})</h3>
            <p className="dash-muted">All current orders in the system</p>
            {ordersAccepted.map((order) => (
              <OperatorOrderCard key={order.orderId} order={order} tab={TABS.ORDERS} handleOperation={handleOperation}/>
            ))}
          </section>

          <section>
            <h3 className="dash-section-title">Orders Preparing ({ordersPreparing.length})</h3>
            <p className="dash-muted">All current orders in the system</p>
            {ordersPreparing.map((order) => (
              <OperatorOrderCard key={order.orderId} order={order} tab={TABS.ORDERS} handleOperation={handleOperation}/>
            ))}
          </section>

            <section>
            <h3 className="dash-section-title">Orders Ready ({ordersReady.length})</h3>
            <p className="dash-muted">All current orders in the system</p>
            {ordersReady.map((order) => (
              <OperatorOrderCard key={order.orderId} order={order} tab={TABS.ORDERS} handleOperation={handleOperation}/>
            ))}
          </section>
      </div>
    )
}
