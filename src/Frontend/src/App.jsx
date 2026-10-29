import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import './App.css'
import RegisterPage from './UI/RegisterPage/RegisterPage'
import LoginPage from './UI/RegisterPage/LoginPage'
import TempAccountPage from './UI/TempAccountPage'
import CustomerHomePage from './UI/CustomerPage/CustomerHomePage'
import StoreMenuPage from './UI/CustomerPage/StoreMenuPage'
import StoreOrdersPage from './UI/StorePage/StoreOrdersPage'
import DashboardLayout from './UI/Dashboard/DashboardLayout'

const router = createBrowserRouter([
  {path:"/", element: <LoginPage/>},
  {path:"/register",element: <RegisterPage/>},
  {path:"/account", element: <TempAccountPage/>},
  {
    element: <DashboardLayout/>,
    children: [
      {path:"/homeCustomer",element:<CustomerHomePage/>},
      {path:"/stores/:storeId", element: <StoreMenuPage/>},
      {path:"/store/orders", element: <StoreOrdersPage/>},
    ]
  }
])

function App() {
  return <RouterProvider router={router} />
}

export default App
