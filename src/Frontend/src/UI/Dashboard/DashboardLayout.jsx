import { useEffect, useState } from 'react'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { getAuthenticatedUserData } from '../../api/REG/Auth'
import { logout } from '../../api/REG/Login'
import { getAuthToken } from '../../api/Client'
import { homePathFor } from '../../api/REG/HomePath'
import OutputConsole from '../Console/OutputConsole'
import './dashboard.css'
import { ROLE } from '../../api/EnumRoles'

export default function DashboardLayout() {
  const [user, setUser] = useState(null)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    const token = getAuthToken()
    if (!token) {
      navigate('/', { replace: true })
      return
    }
    getAuthenticatedUserData(token)
      .then((res) => res.json())
      .then(setUser)
      .catch((err) => {
        if (err.status === 401) {
          logout()
          navigate('/', { replace: true })
          return
        }
        setError(err.message || 'Failed to load your account')
      })
  }, [navigate])

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  if (error) return <p className="dash-error">{error}</p>
  if (!user) return <p className="dash-muted">Loading…</p>

  return (
    <div className="dash-shell">
      <header className="dash-header">
        <Link to={homePathFor(user.role)} className="dash-brand">ShopEasy</Link>
        <div className='header-nav'>
        {user.role != ROLE.STORE && <button onClick={() => navigate('/map')}>MAP</button>}

        {user.role == ROLE.OPERATOR && <button onClick={() => navigate('/operator/home')}>Manage System</button>}
        
        {user.role == ROLE.CUSTOMER && <button onClick={() => navigate('/customer/home')}>Browse Stores</button>}
        {user.role == ROLE.CUSTOMER && <button onClick={() => navigate('/customer/orders')}>My Orders</button>}
        
        {user.role == ROLE.COURIER &&  <button onClick={() => navigate("/courier/home")}>Pending orders</button>}
        
        {user.role == ROLE.STORE && <button onClick={() => navigate('/store/orders')}>Manage Orders</button>}
        {user.role == ROLE.STORE && <button onClick={() => navigate('/store/catalog')}>Manage Store Catalog</button>}
        <button onClick={() => navigate('/history')}>Order History</button>
        </div>
        
        <div className="dash-user">
          <span>{user.storeName || `${user.firstName} ${user.lastName}`}</span>
          <span className="dash-role">{user.role}</span>
          <button type="button" className="dash-button dash-button-light" onClick={handleLogout}>Logout</button>
        </div>
      </header>
      <main className="dash-page">
        <Outlet context={{ user }} />
      </main>
      <OutputConsole />
    </div>
  )
}
