import { useEffect, useState } from 'react'
import { useOutletContext } from 'react-router-dom'

import MapCanvas from './MapCanvas'
import { getAllCouriersOnDuty, getAllStoresForMap } from '../../api/OperatorRequests'
import { ROLE } from '../../api/EnumRoles'

export default function MapWrapper() {
  const { user } = useOutletContext()
  const [stores, setStores] = useState([])
  const [couriersOnDuty, setCouriersOnDuty] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    let isMounted = true
    const couriersRequest = user?.role === ROLE.OPERATOR
      ? getAllCouriersOnDuty()
      : Promise.resolve([])
    Promise.allSettled([getAllStoresForMap(), couriersRequest])
      .then(([storesResult, couriersResult]) => {
        if (!isMounted) return
        const errors = []

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
      })

    return () => {
      isMounted = false
    }
  }, [user?.role])

  return (
    <>
      {error && <p className="dash-error" role="alert">{error}</p>}
      <div className="dash-title-row">
       <MapCanvas stores={stores} couriers={couriersOnDuty} user={user}/>
      </div>
    </>
  )
}
