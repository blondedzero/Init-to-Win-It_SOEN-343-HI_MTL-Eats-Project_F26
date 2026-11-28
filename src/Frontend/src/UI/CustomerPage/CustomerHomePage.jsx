import './customer-page.css'
import { useEffect, useState } from 'react'
import { useNavigate, useOutletContext } from 'react-router-dom'
import { getStores, STORESTATUS } from '../../api/Store'
import StoreRating from './StoreRating'

const STORE_STATUS_PRIORITY = {
  [STORESTATUS.OPEN]: 0,
  [STORESTATUS.PAUSED]: 1,
  [STORESTATUS.CLOSED]: 2,
}

function distanceBetween(latitudeA, longitudeA, latitudeB, longitudeB) {
  const radians = (degrees) => degrees * Math.PI / 180
  const latitudeDelta = radians(latitudeB - latitudeA)
  const longitudeDelta = radians(longitudeB - longitudeA)
  const a = Math.sin(latitudeDelta / 2) ** 2
    + Math.cos(radians(latitudeA)) * Math.cos(radians(latitudeB))
    * Math.sin(longitudeDelta / 2) ** 2
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

export default function CustomerHomePage() {
  const { user } = useOutletContext()
  const [stores, setStores] = useState([])
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    getStores().then(setStores).catch((e) => setError(e.message))
  }, [])

  const customerLatitude = Number(user.latitude)
  const customerLongitude = Number(user.longitude)
  const hasCustomerLocation = Number.isFinite(customerLatitude) && Number.isFinite(customerLongitude)
  const sortedStores = [...stores].sort((a, b) => {
    const statusA = a.status?.toUpperCase()
    const statusB = b.status?.toUpperCase()
    const priorityA = STORE_STATUS_PRIORITY[statusA] ?? STORE_STATUS_PRIORITY[STORESTATUS.CLOSED]
    const priorityB = STORE_STATUS_PRIORITY[statusB] ?? STORE_STATUS_PRIORITY[STORESTATUS.CLOSED]

    return priorityA - priorityB
      || (a.storeName ?? '').localeCompare(b.storeName ?? '', undefined, { sensitivity: 'base' })
  })

  return (
    <section className="customer-home">
      <h2 className="customer-home-title">Browse stores</h2>
      {error && <p className="dash-error" role="alert">{error}</p>}
      {!error && stores.length === 0 && <p className="dash-muted">No stores are available yet.</p>}
      <div className="scroll-list">
        {sortedStores.map((store) => {
          const status = store.status?.toUpperCase()
          const distance = hasCustomerLocation
            ? distanceBetween(customerLatitude, customerLongitude, store.latitude, store.longitude)
            : null
          const statusLabel = status === STORESTATUS.OPEN
            ? 'Open'
            : status === STORESTATUS.PAUSED ? 'Paused' : 'Closed'

          return (
            <article key={store.id} className={`store-row store-row-${status?.toLowerCase() ?? 'closed'}`}>
              <div className="store-info">
                <h3 className="store-name">{store.storeName}</h3>
                <StoreRating storeId={store.id} />
                <span className="store-address">{store.address}</span>
                <span className="store-distance">
                  {distance === null ? 'Distance unavailable' : `${distance.toFixed(1)} km away`}
                </span>
              </div>
              <div className="store-actions">
                <span className={`store-status store-status-${status?.toLowerCase() ?? 'closed'}`}>
                  {statusLabel}
                </span>
                <button
                  type="button"
                  className="browse-button"
                  onClick={() => navigate(`/stores/${store.id}`)}
                >
                  Browse
                </button>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}