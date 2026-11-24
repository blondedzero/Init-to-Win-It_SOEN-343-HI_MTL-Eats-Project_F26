import { useCallback, useEffect, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { getMyProducts, getCategories, addProduct, updateProduct, removeProduct, restoreProduct } from '../../api/Store'
import { logSuccess } from '../Console/consoleLog'
import StoreStatusControl from './StoreStatusControl'

const money = (value) => `$${Number(value).toFixed(2)}`

const emptyForm = {
  name: '',
  description: '',
  price: '',
  stockQuantity: '',
  categoryName: '',
}

const STATUS_LABEL = {
  AVAILABLE: 'Available',
  OUT_OF_STOCK: 'Out of stock',
  UNAVAILABLE: 'Removed',
}

const STATUS_BADGE = {
  AVAILABLE: 'available',
  OUT_OF_STOCK: 'sold-out',
  UNAVAILABLE: 'removed',
}

// 5.5.2-6: the store representative's catalog (add / edit / remove items).
export default function StoreCatalogPage() {
  const { user } = useOutletContext()
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)      // null = the form adds a new product
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  // --- loading data ---
  const load = useCallback(() => {
    return Promise.all([getMyProducts(), getCategories()])
      .then(([productList, categoryList]) => {
        setError('')
        setProducts(productList)
        setCategories(categoryList)
      })
      .catch((err) => setError(err.message))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  // --- running a command: call, log, reload ---
  const runCommand = async (command, successMessage) => {
    setBusy(true)
    setError('')
    try {
      const result = await command()
      logSuccess(successMessage(result))
      await load()
      return true
    } catch (err) {
      setError(err.message)
      return false
    } finally {
      setBusy(false)
    }
  }

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setError('')
  }

  const startEditing = (product) => {
    setEditingId(product.productId)
    setForm({
      name: product.name,
      description: product.description ?? '',
      price: String(product.price),
      stockQuantity: String(product.stockQuantity),
      categoryName: product.category,
    })
    setError('')
  }

  const cancelEditing = () => {
    setEditingId(null)
    setForm(emptyForm)
    setError('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const payload = {
      name: form.name.trim(),
      description: form.description.trim(),
      price: Number(form.price),
      stockQuantity: Number(form.stockQuantity),
      categoryName: form.categoryName.trim(),
    }
    if (!payload.name || !payload.categoryName || form.price === '' || form.stockQuantity === '') {
      setError('Name, category, price and stock are required.')
      return
    }

    const ok = editingId === null
      ? await runCommand(() => addProduct(payload), (p) => `Added ${p.name}`)
      : await runCommand(() => updateProduct(editingId, payload), (p) => `Updated ${p.name}`)

    if (ok) cancelEditing()
  }

  return (
    <>
      <div className="dash-title-row">
        <h2 className="dash-title">{user.storeName} — Catalog</h2>
        <StoreStatusControl />
        <button type="button" className="dash-button dash-button-light" onClick={load}>Refresh</button>
      </div>
      {error && <p className="dash-error">{error}</p>}

      <form className="dash-card product-form" onSubmit={handleSubmit}>
        <h3 className="dash-section-title">{editingId === null ? 'Add a product' : `Edit product #${editingId}`}</h3>
        <div className="product-form-grid">
          <label className="product-field">
            <span>Name</span>
            <input className="dash-input" name="name" value={form.name} onChange={handleChange} placeholder="Bananas" />
          </label>
          <label className="product-field">
            <span>Category</span>
            <select
              className="dash-input"
              name="categoryName"
              value={form.categoryName}
              onChange={handleChange}
              required
            >
              <option value="">Select a category</option>
              {categories.map((category) => (
                <option key={category} value={category}>{category}</option>
              ))}
            </select>
          </label>
          <label className="product-field">
            <span>Price ($)</span>
            <input className="dash-input" name="price" type="number" min="0" step="0.01" value={form.price} onChange={handleChange} />
          </label>
          <label className="product-field">
            <span>In stock</span>
            <input className="dash-input" name="stockQuantity" type="number" min="0" step="1" value={form.stockQuantity} onChange={handleChange} />
          </label>
          <label className="product-field product-field-wide">
            <span>Description</span>
            <input className="dash-input" name="description" value={form.description} onChange={handleChange} placeholder="Optional" />
          </label>
        </div>
        <div className="dash-actions">
          <button type="submit" className="dash-button" disabled={busy}>
            {editingId === null ? 'Add product' : 'Save changes'}
          </button>
          {editingId !== null && (
            <button type="button" className="dash-button dash-button-light" onClick={cancelEditing} disabled={busy}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <table className="dash-table">
        <thead>
          <tr>
            <th>Product</th>
            <th>Category</th>
            <th>Price</th>
            <th>In stock</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {products.length === 0 && (
            <tr><td colSpan={6} className="dash-muted">No products yet — add your first one above.</td></tr>
          )}
          {products.map((p) => {
            const removed = p.status === 'UNAVAILABLE'
            return (
              <tr key={p.productId} className={removed ? 'dash-row-disabled' : ''}>
                <td>
                  {p.name}
                  {p.description && <div className="dash-muted product-description">{p.description}</div>}
                </td>
                <td>{p.category}</td>
                <td>{money(p.price)}</td>
                <td>{p.stockQuantity}</td>
                <td>
                  <span className={`dash-badge dash-badge-${STATUS_BADGE[p.status] ?? ''}`}>
                    {STATUS_LABEL[p.status] ?? p.status}
                  </span>
                </td>
                <td>
                  <div className="dash-actions product-actions">
                    <button type="button" className="dash-button dash-button-light" disabled={busy}
                            onClick={() => startEditing(p)}>
                      Edit
                    </button>
                    {removed ? (
                      <button type="button" className="dash-button" disabled={busy}
                              onClick={() => runCommand(() => restoreProduct(p.productId), (r) => `Restored ${r.name}`)}>
                        Restore
                      </button>
                    ) : (
                      <button type="button" className="dash-button dash-button-danger" disabled={busy}
                              onClick={() => runCommand(() => removeProduct(p.productId), (r) => `Removed ${r.name}`)}>
                        Remove
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </>
  )
}
