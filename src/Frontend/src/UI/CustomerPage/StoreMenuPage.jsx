import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getProducts } from '../../api/Store'
import { estimateOrder, placeOrder } from '../../api/Orders'
import { logSuccess } from '../Console/consoleLog'
import StoreRating from './StoreRating'

const money = (value) => `$${Number(value).toFixed(2)}`

export default function StoreMenuPage() {
  const { storeId } = useParams()
  const [menu, setMenu] = useState(null)
  const [quantities, setQuantities] = useState({})
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [estimateResult, setEstimateResult] = useState(null)

  const loadMenu = useCallback(() => {
    return getProducts(storeId)
      .then((data) => {
        setMenu(data)
        logSuccess(`Loaded ${data.store.storeName} (${data.products.length} products)`)
      })
      .catch((err) => setError(err.message))
  }, [storeId])

  useEffect(() => {
    loadMenu()
  }, [loadMenu])

  const selected = menu
    ? menu.products.filter((p) => p.status === 'AVAILABLE' && p.stockQuantity > 0 && quantities[p.productId] > 0)
    : []
  const lines = selected.map((p) => ({ productId: p.productId, quantity: quantities[p.productId] }))
  const linesKey = JSON.stringify(lines)
  const storeOpen = menu?.store.status === 'OPEN'

  useEffect(() => {
    if (!menu || !storeOpen || lines.length === 0) return

    let active = true
    const timer = setTimeout(async () => {
      try {
        const pricing = await estimateOrder(menu.store.id, JSON.parse(linesKey))
        if (active) setEstimateResult({ linesKey, pricing, error: '' })
      } catch (err) {
        if (active) setEstimateResult({ linesKey, pricing: null, error: err.message })
      }
    }, 200)

    return () => {
      active = false
      clearTimeout(timer)
    }
  }, [menu, storeOpen, lines.length, linesKey])

  if (!menu) {
    return error ? <p className="dash-error">{error}</p> : <p className="dash-muted">Loading products…</p>
  }

  const disabledReason = !storeOpen
    ? `${menu.store.storeName} is ${menu.store.status.toLowerCase()} and is not accepting orders.`
    : selected.length === 0 ? 'Select at least one available item.' : ''
  const currentEstimate = estimateResult?.linesKey === linesKey ? estimateResult : null

  const setQuantity = (productId, value) => {
    const quantity = Math.max(0, Math.floor(Number(value) || 0))
    const product = menu.products.find((item) => item.productId === productId)
    const limitedQuantity = Math.min(quantity, product?.stockQuantity ?? 0)
    setQuantities((current) => ({ ...current, [productId]: limitedQuantity }))
    setError('')
    setSuccess('')
  }

  const handlePlaceOrder = async () => {
    setSubmitting(true)
    setError('')
    setSuccess('')
    try {
      const lines = selected.map((p) => ({ productId: p.productId, quantity: quantities[p.productId] }))
      const result = await placeOrder(menu.store.id, lines)
      logSuccess(`${result.message} (event #${result.eventId})`)
      setSuccess(`${result.message}. Total ${money(result.order.totalCost)}.`)
      setQuantities({})
    } catch (err) {
      setError(err.message)
      loadMenu()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <Link to="/customer/home" className="dash-back">← All stores</Link>
      <div className="dash-title-row">
        <h2 className="dash-title">{menu.store.storeName}</h2>
        <span className={`dash-badge dash-badge-${menu.store.status.toLowerCase()}`}>{menu.store.status}</span>
      </div>
      <p className="dash-muted">{menu.store.address}</p>
      <StoreRating storeId={storeId} />

      <table className="dash-table">
        <thead>
          <tr>
            <th>Product</th>
            <th>Category</th>
            <th>Price</th>
            <th>Status</th>
            <th>In stock</th>
            <th>Quantity</th>
          </tr>
        </thead>
        <tbody>
          {menu.products.length === 0 && (
            <tr><td colSpan={6} className="dash-muted">This store has no products yet.</td></tr>
          )}
          {menu.products.map((p) => {
            const available = p.status === 'AVAILABLE' && p.stockQuantity > 0
            return (
              <tr key={p.productId} className={available ? '' : 'dash-row-disabled'}>
                <td>{p.name}</td>
                <td>{p.category}</td>
                <td>{money(p.price)}</td>
                <td>
                  <span className={`dash-badge dash-badge-${available ? 'available' : 'sold-out'}`}>
                    {available ? 'Available' : 'Sold out'}
                  </span>
                </td>
                <td>{p.stockQuantity}</td>
                <td>
                  {available && 
                    <input
                      className="dash-input dash-qty"
                      type="number"
                      min="0"
                      max={p.stockQuantity}
                      value={quantities[p.productId] ?? 0}
                      onChange={(e) => setQuantity(p.productId, e.target.value)}
                    />}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>

      <div className="dash-checkout">
        {selected.length > 0 && storeOpen && <div className="dash-pricing-summary">
          {currentEstimate?.pricing ? <>
            <span>Items subtotal: {money(currentEstimate.pricing.subtotal)}</span>
            <span>Small order fee: {money(currentEstimate.pricing.smallOrderFee)}</span>
            <span>Service fee: {money(currentEstimate.pricing.serviceFee)}</span>
            <span>Delivery fee: {money(currentEstimate.pricing.deliveryFee)}</span>
            <span>Tax: {money(currentEstimate.pricing.taxAmount)}</span>
            <strong>Estimated total: {money(currentEstimate.pricing.totalCost)}</strong>
          </> : currentEstimate?.error
            ? <span className="dash-error">Could not estimate total: {currentEstimate.error}</span>
            : <span className="dash-muted">Calculating order estimate…</span>}
        </div>}
        <button
          type="button"
          className="dash-button"
          disabled={Boolean(disabledReason) || submitting}
          onClick={handlePlaceOrder}
        >
          {submitting ? 'Placing…' : 'Place order'}
        </button>
        {disabledReason && <span className="dash-muted">{disabledReason}</span>}
      </div>

      {error && <p className="dash-error">{error}</p>}
      {success && <p className="dash-success">{success}</p>}
    </>
  )
}
