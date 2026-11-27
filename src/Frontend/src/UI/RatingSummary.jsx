import StarRating from './StarRating'

export default function RatingSummary({ rating }) {
  if (!rating) return null
  if (rating.ratingCount === 0) return <p className="dash-muted">No ratings yet</p>
  return (
    <p className="dash-muted rating-summary">
      <StarRating value={rating.averageScore} max={rating.maxScore} />
      {` ${rating.averageScore.toFixed(1)}/${rating.maxScore} (${rating.ratingCount} ${rating.ratingCount === 1 ? 'rating' : 'ratings'})`}
    </p>
  )
}
