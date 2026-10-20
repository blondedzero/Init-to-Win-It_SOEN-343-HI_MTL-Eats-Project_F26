import { useEffect, useState } from 'react'
import { login } from '../../api/REG/Login.js'
import './register-page.css'
import { Link } from 'react-router-dom'
import { validateToken } from '../../api/REG/Auth.js'

const initialForm = {
  email: '',
  password: '',
}

export default function LoginPage() {
  const [formData, setFormData] = useState(initialForm)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) return
    async function validateTokenOnLauch(token) {
        try {
          setIsSubmitting(true)
          setError('')
          const res = await validateToken(token)
          const resBody = await res.json()
          console.log(resBody)
          setSuccess(resBody.message)

          //navigate to ...

        } catch (invalidTokenError) {
            setError(invalidTokenError.message || 'Authentication failed. Please login again.')
            localStorage.removeItem('token')
        } finally {
            setIsSubmitting(false)
        }  
    } 

    validateTokenOnLauch(token)
  }, [])

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((current) => ({ ...current, [name]: value }))
    setError('')
    setSuccess('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (!formData.email.trim() || !formData.password) {
      setError('Please fill all required fields.')
      return
    }

    try {
      setIsSubmitting(true)
      setError('')

      const res = await login(formData.email.trim(), formData.password)
      const resBody = await res.json()
      console.log(resBody)
      setSuccess(resBody.message)
      localStorage.setItem('token', resBody.token)
      setFormData(initialForm)

    } catch (submitError) {
        setError(submitError.message || 'Login failed. Please try again.')
    } finally {
        setIsSubmitting(false)
    }
  }

  return (
    <main className="register-page">
      <div className="register-card">
        <div className="register-header">
          <p className="eyebrow">Welcome to ShopEasy</p>
          <h1>Login</h1>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
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

            <label className="input-label">
                <span>Password</span>
                <input
                className="register-input"
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Enter your password"
                />
            </label>

          {error && <p className="register-message register-error">{error}</p>}
          {success && <p className="register-message register-success">{success}</p>}

          <button className="register-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Logging in...' : 'Login'}
          </button>

          <span>Don't have an account? <Link to="/register">Register</Link></span>
        </form>
      </div>
    </main>
  )
}