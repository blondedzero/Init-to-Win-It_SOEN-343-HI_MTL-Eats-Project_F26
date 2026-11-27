import { useEffect, useState } from 'react'
import { getStoreRating } from '../../api/Reviews'
import RatingSummary from '../RatingSummary'

export default function StoreRating({ storeId }) {
  const [rating, setRating] = useState(null)

  useEffect(() => {
    getStoreRating(storeId).then(setRating).catch(() => setRating(null))
  }, [storeId])

  return <RatingSummary rating={rating} />
}
