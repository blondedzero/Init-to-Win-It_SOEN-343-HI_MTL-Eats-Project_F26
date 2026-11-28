import { formatDateTime } from '../../../../utils/format'
import StarRating from '../../../Rating/StarRating'

export default function RatingDetails({ rating, maxScore }) {
    const max = maxScore ?? 5
    return (
        <div className="rating-details">
            <strong>Rated</strong>
            <span>Store <StarRating value={rating.storeScore} max={max} /></span>
            <span>Courier <StarRating value={rating.courierScore} max={max} /></span>
            <span>Rated at: {formatDateTime(rating.ratedAt)}</span>
        </div>
    )
}
