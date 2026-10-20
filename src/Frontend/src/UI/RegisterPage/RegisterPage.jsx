import { useEffect, useState } from 'react'
import { register } from '../../api/REG/Register.js'
import { ROLE } from '../../api/EnumRoles.js'
import './register-page.css'
import { Link, useNavigate } from 'react-router-dom'
import CustomerDetails from './CustomerDetails.jsx'
import StoreDetails from './StoreDetails.jsx'
import CourierDetails from './CourierDetails.jsx'

const initialForm = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  confirmPassword: '',
  role: ROLE[0],
  address: '',
  cardNumber:'',
  cardExpMonth:'',
  cardExpYear:'',
  cardCVV:'',
  storeName: '',
  carMake: '',
  carModel: '',
  carYear: '',
  carColor: '',
}

export default function RegisterPage() {
  const navigate = useNavigate()
  const [formData, setFormData] = useState(initialForm)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isRedirecting, setIsRedirecting] = useState(false)

  const roleOptions = Object.values(ROLE)
  const selectedRole = formData.role

  useEffect(() => {
    //if user opens register page, redirect to login page and authenticate if user has a token
    const token = localStorage.getItem('authToken') || sessionStorage.getItem('authToken')
    if (!token) return
    navigate("/")
  }, [])

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((current) => ({ ...current, [name]: value }))
    setError('')
    setSuccess('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (isRedirecting) return

    if (!formData.firstName.trim() || !formData.lastName.trim() || !formData.email.trim() || !formData.password) {
      setError('Please complete all required fields.')
      return
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    try {
      setIsSubmitting(true)
      setError('')

      const res = await register(selectedRole, formData.email.trim(), formData.password, formData.firstName.trim(), formData.lastName.trim(), {
        address: formData.address.trim(),
        storeName: formData.storeName.trim(),
        carMake: formData.carMake.trim(),
        carModel: formData.carModel.trim(),
        carYear: formData.carYear.trim(),
        carColor: formData.carColor.trim(),
        cardNumber: formData.cardNumber.trim(),
        cardExpMonth: formData.cardExpMonth.trim(),
        cardExpYear: formData.cardExpYear.trim(),
        cardCVV: formData.cardCVV.trim()
      })

      console.log(res)
      const resBody = await res.json()
      setSuccess(resBody.message)

      setFormData(initialForm)
      setIsRedirecting(true)
      setTimeout(() => {
        navigate('/')
      }, 2500)

    } catch (submitError) {
      setError(submitError.message || 'Registration failed. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="register-page">
      <div className="register-card">
        <div className="register-header">
          <p className="eyebrow">Welcome to ShopEasy</p>
          <h1>Create an account</h1>
        </div>

        <form onSubmit={handleSubmit} className="register-form">
          <div className="form-grid">
            <label className="input-label">
              <span>First name</span>
              <input
                className="register-input"
                type="text"
                name="firstName"
                value={formData.firstName}
                onChange={handleChange}
                placeholder="Enter your first name"
              />
            </label>

            <label className="input-label">
              <span>Last name</span>
              <input
                className="register-input"
                type="text"
                name="lastName"
                value={formData.lastName}
                onChange={handleChange}
                placeholder="Enter your last name"
              />
            </label>
          </div>

          <label className="input-label">
            <span>Email</span>
            <input
              className="register-input"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="name@example.com"
            />
          </label>

          <div className="form-grid">
            <label className="input-label">
              <span>Password</span>
              <input
                className="register-input"
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Create a password"
              />
            </label>

            <label className="input-label">
              <span>Confirm password</span>
              <input
                className="register-input"
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="Confirm your password"
              />
            </label>
          </div>

          <label className="input-label">
            <span>Sign up as</span>
            <select className="register-select" name="role" value={selectedRole} onChange={handleChange}>
              {roleOptions.map((role) => (
                <option key={role} value={role}>
                  {role}
                </option>
              ))}
            </select>
          </label>


          <div className='user-details'>
            
            {selectedRole === ROLE[0] && <CustomerDetails formData={formData} handleChange={handleChange}/>}

            {selectedRole === ROLE[1] && <StoreDetails formData={formData} handleChange={handleChange} />}

            {selectedRole === ROLE[2] && <CourierDetails formData={formData} handleChange={handleChange} />}
          </div>
          
          {error && <p className="register-message register-error">{error}</p>}
          {success && <p className="register-message register-success">{success}</p>}

          <button className="register-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Creating account...' : 'Create account'}
          </button>

          <span>Already have an account? <Link to="/">Login</Link></span>

        </form>
      </div>
    </main>
  )
}

