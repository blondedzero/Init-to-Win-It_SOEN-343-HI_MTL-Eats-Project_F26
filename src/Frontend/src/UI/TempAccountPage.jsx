import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { getAuthenticatedUserData } from "../api/REG/Auth"
import { ROLE } from "../api/EnumRoles"
import "./temp-account-page.css"

//this is a temporary page where the user is redirected after login
export default function TempAccountPage() {
    
    const authToken = localStorage.getItem('authToken') || sessionStorage.getItem('authToken')
    const [user, setUser] = useState({})
    const [error, setError] = useState('')
    const navigate = useNavigate()
    
    useEffect(() => {
        if (!authToken) {
            navigate('/')
            return
        }
        async function getUserData(token) {
            try {
            const res = await getAuthenticatedUserData(token)
            const resBody = await res.json()
            console.log(resBody)
            setUser({
                firstName: resBody.firstName,
                lastName: resBody.lastName,
                role: resBody.role,
                email: resBody.email,
                storeName: resBody.storeName
            })
    
            } catch (error) {
                if (error.status === 401) {
                    localStorage.removeItem('authToken')
                    sessionStorage.removeItem('authToken')
                    navigate('/', { replace: true })
                    return
                }
                setError(error.message || 'Failed to get user data')
            }
        }

        getUserData(authToken)
    }, [])

    const logout = () => {
        localStorage.removeItem("authToken")
        sessionStorage.removeItem("authToken")
        setTimeout(() => {
            navigate("/")
        }, 1500)
    }

    return(
        <div className="user-info">
            <span>This is a temporary account page</span>
            {user && 
                <>
                    <span>Name: {`${user.firstName}, ${user.lastName}`}</span>
                    <span>Email: {user.email}</span>
                    <span>Role: {user.role}</span>
                    {user.role == ROLE[1] && <span>Store name: {user.storeName}</span>}
                </>}
            {error && <p className="register-message register-error">{error}</p>}
            <button className="logout-button" onClick={logout}>
                Logout
            </button>
        </div>
    )
}