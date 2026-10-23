import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import './App.css'
import RegisterPage from './UI/RegisterPage/RegisterPage'
import LoginPage from './UI/RegisterPage/LoginPage'
import TempAccountPage from './UI/TempAccountPage'
import CustomerHomePage from './UI/CustomerPage/CustomerHomePage'

const router = createBrowserRouter([
  {path:"/", element: <LoginPage/>},
  {path:"/register",element: <RegisterPage/>},
  {path:"/account", element: <TempAccountPage/>},
  {path:"/homeCustomer",element:<CustomerHomePage/>}
])

function App() {
  return <RouterProvider router={router} />
}

export default App
