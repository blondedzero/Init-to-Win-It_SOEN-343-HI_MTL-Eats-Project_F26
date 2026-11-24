import { useEffect, useState } from 'react'
import { changeStoreStatus, getMyStore, STORESTATUS } from '../../api/Store'
import { logSuccess } from '../Console/consoleLog'

export default function StoreStatusControl() {
  const [store, setStore] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    getMyStore()
      .then(setStore)
      .catch((err) => setError(err.message))
  }, [])

  const setStatus = async (status) => {
    setBusy(true)
    setError('')
    try {
      const updatedStore = await changeStoreStatus(status)
      setStore(updatedStore)
      logSuccess(`Store is now ${status.toLowerCase()}.`)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (!store) {
    return error
      ? <span className="dash-error" role="alert">{error}</span>
      : <span className="dash-muted">Loading store status…</span>
  }

  return (
    <div className="dash-actions">
      {error && <span className="dash-error" role="alert">{error}</span>}
      <span className={`dash-badge dash-badge-${store.status.toLowerCase()}`}>
        {store.status}
      </span>
      <button
        type="button"
        className="dash-button"
        disabled={busy || store.status === STORESTATUS.OPEN}
        onClick={() => setStatus(STORESTATUS.OPEN)}
      >
        Open Store
      </button>
      <button
        type="button"
        className="dash-button dash-button-light"
        disabled={busy || store.status === STORESTATUS.PAUSED}
        onClick={() => setStatus(STORESTATUS.PAUSED)}
      >
        Pause Store
      </button>
      <button
        type="button"
        className="dash-button dash-button-danger"
        disabled={busy || store.status === STORESTATUS.CLOSED}
        onClick={() => setStatus(STORESTATUS.CLOSED)}
      >
        Close Store
      </button>
    </div>
  )
}