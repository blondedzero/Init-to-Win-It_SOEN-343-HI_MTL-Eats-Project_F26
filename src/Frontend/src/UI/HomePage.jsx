import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { ROLE } from "../api/EnumRoles"

//this is the page a user gets redirected to after a successful login
export default function HomePage() {
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

    return(
        <div className="home-page">
            {user.role == ROLE.CUSTOMER }
            
        </div>
    )
}
