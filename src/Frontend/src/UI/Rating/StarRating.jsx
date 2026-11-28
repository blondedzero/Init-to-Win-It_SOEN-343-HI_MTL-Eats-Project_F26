import { useState } from 'react'

export default function StarRating({ value, max, onChange, label }) {
  const [hovered, setHovered] = useState(0)
  const stars = Array.from({ length: max }, (_, i) => i + 1)

  if (!onChange) {
    const filledPercent = Math.max(0, Math.min(100, (value / max) * 100))
    return (
      <span className="star-rating star-display" role="img" aria-label={`${value} out of ${max} stars`}>
        <span className="star-row">
          {stars.map((star) => <span key={star} className="star">★</span>)}
        </span>
        <span className="star-row star-row-filled" style={{ width: `${filledPercent}%` }}>
          {stars.map((star) => <span key={star} className="star star-filled">★</span>)}
        </span>
      </span>
    )
  }

  const shown = hovered || value
  return (
    <span className="star-rating" role="radiogroup" aria-label={label}>
      {stars.map((star) => (
        <button
          key={star}
          type="button"
          role="radio"
          aria-checked={star === value}
          aria-label={`${star} ${star === 1 ? 'star' : 'stars'}`}
          className={`star star-button ${star <= shown ? 'star-filled' : ''}`}
          onClick={() => onChange(star)}
          onMouseEnter={() => setHovered(star)}
          onMouseLeave={() => setHovered(0)}
        >★</button>
      ))}
    </span>
  )
}
