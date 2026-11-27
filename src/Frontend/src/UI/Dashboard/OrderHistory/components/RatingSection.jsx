import { useState } from 'react'
import StarRating from '../../../StarRating'

export default function RatingSection({ order, scale, onRate }) {
    const [showRatingForm, setShowRatingForm] = useState(false)
    const [storeScore, setStoreScore] = useState(0)
    const [courierScore, setCourierScore] = useState(0)
    const [submitting, setSubmitting] = useState(false)
    const complete = storeScore >= scale.minScore && courierScore >= scale.minScore

    async function submitRating(event) {
        event.preventDefault()
        setSubmitting(true)
        try {
            const rated = await onRate(order.orderId, storeScore, courierScore)
            if (rated) setShowRatingForm(false)
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <>
            <button type="button" className="dash-button dash-button-square rating-toggle"
                    onClick={() => setShowRatingForm((shown) => !shown)}>
                {showRatingForm ? 'Hide rating form' : 'Rate order'}
            </button>
            {showRatingForm && <form className="rating-form" onSubmit={submitRating}>
                <label>
                    Store ({order.storeName})
                    <StarRating value={storeScore} max={scale.maxScore} onChange={setStoreScore}
                                label="Store score" />
                </label>
                <label>
                    Courier{order.courierName ? ` (${order.courierName})` : ''}
                    <StarRating value={courierScore} max={scale.maxScore} onChange={setCourierScore}
                                label="Courier score" />
                </label>
                <button type="submit" className="dash-button dash-button-square" disabled={submitting || !complete}>
                    {submitting ? 'Submitting...' : 'Submit rating'}
                </button>
            </form>}
        </>
    )
}
