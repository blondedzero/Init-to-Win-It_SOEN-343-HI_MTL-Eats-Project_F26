import { useState } from 'react'
import { register } from '../api/Register.js'
import { ROLE } from '../api/EnumRoles.js'
import './register-page.css'

const initialForm = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  confirmPassword: '',
  role: ROLE[0],
  address: '',
  storeName: '',
  carMake: '',
  carModel: '',
  carYear: '',
  carColor: '',
}

export default function RegisterPage() {
  const [formData, setFormData] = useState(initialForm)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const roleOptions = Object.values(ROLE)
  const selectedRole = formData.role

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((current) => ({ ...current, [name]: value }))
    setError('')
    setSuccess('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

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

      await register(selectedRole, formData.email.trim(), formData.password, formData.firstName.trim(), formData.lastName.trim(), {
        address: formData.address.trim(),
        storeName: formData.storeName.trim(),
        carMake: formData.carMake.trim(),
        carModel: formData.carModel.trim(),
        carYear: formData.carYear.trim(),
        carColor: formData.carColor.trim(),
      })

      setSuccess('Registration successful! Welcome aboard.')
      setFormData(initialForm)
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

          {selectedRole === ROLE[0] && (
            <label className="input-label">
              <span>Address</span>
              <input
                className="register-input"
                type="text"
                name="address"
                value={formData.address}
                onChange={handleChange}
                placeholder="123 Street, City, Province, Zip"
              />
            </label>
          )}

          {selectedRole === ROLE[1] && (
            <>
              <label className="input-label">
                <span>Store address</span>
                <input
                  className="register-input"
                  type="text"
                  name="address"
                  value={formData.address}
                  onChange={handleChange}
                  placeholder="Enter the store address"
                />
              </label>

              <label className="input-label">
                <span>Store name</span>
                <input
                  className="register-input"
                  type="text"
                  name="storeName"
                  value={formData.storeName}
                  onChange={handleChange}
                  placeholder="Enter the store name"
                />
              </label>
            </>
          )}

          {selectedRole === ROLE[2] && (
            <div className="form-grid">
              <label className="input-label">
                <span>Car make</span>
                <input
                  className="register-input"
                  type="text"
                  name="carMake"
                  value={formData.carMake}
                  onChange={handleChange}
                  placeholder="Enter car make"
                />
              </label>

              <label className="input-label">
                <span>Car model</span>
                <input
                  className="register-input"
                  type="text"
                  name="carModel"
                  value={formData.carModel}
                  onChange={handleChange}
                  placeholder="Enter car model"
                />
              </label>

              <label className="input-label">
                <span>Car year</span>
                <input
                  className="register-input"
                  type="text"
                  name="carYear"
                  value={formData.carYear}
                  onChange={handleChange}
                  placeholder="Enter car year"
                />
              </label>

              <label className="input-label">
                <span>Car color</span>
                <input
                  className="register-input"
                  type="text"
                  name="carColor"
                  value={formData.carColor}
                  onChange={handleChange}
                  placeholder="Enter car color"
                />
              </label>
            </div>
          )}

          {error && <p className="register-message register-error">{error}</p>}
          {success && <p className="register-message register-success">{success}</p>}

          <button className="register-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Creating account...' : 'Create account'}
          </button>
        </form>
      </div>
    </main>
  )
}
