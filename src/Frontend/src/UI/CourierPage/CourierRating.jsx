import { useEffect, useState } from 'react'
import { getMyCourierRating } from '../../api/Reviews'
import RatingSummary from '../Rating/RatingSummary'

export default function CourierRating() {
  const [rating, setRating] = useState(null)

  useEffect(() => {
    getMyCourierRating().then(setRating).catch(() => setRating(null))
  }, [])

  return <RatingSummary rating={rating} />
}
