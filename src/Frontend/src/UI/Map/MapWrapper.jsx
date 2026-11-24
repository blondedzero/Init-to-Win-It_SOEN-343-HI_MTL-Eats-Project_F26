import { useCallback, useEffect, useState } from 'react'
import { useOutletContext } from 'react-router-dom'

import MapCanvas from './MapCanvas'
import { getAllCouriersOnDuty, getAllOpenAndPausedStores } from '../../api/OperatorRequests'

export default function MapWrapper() {
  const [openAndPausedStores, setOpenAndPausedStores] = useState([])
  const [couriersOnDuty, setCouriersOnDuty] = useState([])
  const [error, setError] = useState('')

  const load = useCallback(async() => {
    const [storesResult, couriersResult] = await Promise.allSettled([
      getAllOpenAndPausedStores(),
      getAllCouriersOnDuty()
    ])
    const errors = []

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
       <MapCanvas stores={openAndPausedStores} couriers={couriersOnDuty}/>
      </div>
    </>
  )
}
