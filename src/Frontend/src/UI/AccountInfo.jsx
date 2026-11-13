import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { getAuthenticatedUserData } from "../api/REG/Auth"
import { ROLE } from "../api/EnumRoles"
import "./account-info.css"

export default function AccountInfo(user) {
    
    const navigate = useNavigate()
    
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
                    {user.role === ROLE.STORE && <span>Store name: {user.storeName}</span>}
                </>}
            {error && <p className="register-message register-error">{error}</p>}
            <button className="logout-button" onClick={logout}>
                Logout
            </button>
        </div>
    )
}