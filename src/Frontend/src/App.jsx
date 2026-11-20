import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import './App.css'
import RegisterPage from './UI/RegisterPage/RegisterPage'
import LoginPage from './UI/RegisterPage/LoginPage'
import TempAccountPage from './UI/TempAccountPage'
import CustomerHomePage from './UI/CustomerPage/CustomerHomePage'
import StoreMenuPage from './UI/CustomerPage/StoreMenuPage'
import StoreOrdersPage from './UI/StorePage/StoreOrdersPage'
// import StoreCatalogPage from './UI/StorePage/StoreCatalogPage'
import DashboardLayout from './UI/Dashboard/DashboardLayout'
import OperatorPage from './UI/OperatorPage/OperatorPage'
import MapWrapper from './UI/MapWrapper'
import OrderHistoryPage from './UI/Dashboard/OrderHistoryPage'
import CustomerOrdersPage from './UI/CustomerPage/CustomerOrdersPage'
import CourierPage from './UI/CourierPage/CourierPage'

const router = createBrowserRouter([
  {path:"/", element: <LoginPage/>},
  {path:"/register",element: <RegisterPage/>},
  {path:"/account", element: <TempAccountPage/>},
  {
    element: <DashboardLayout/>,
    children: [
      {path:"/customer/home",element:<CustomerHomePage/>},
      {path:"/stores/:storeId", element: <StoreMenuPage/>},
      {path:"/customer/orders", element: <CustomerOrdersPage/>},
      {path:"/store/orders", element: <StoreOrdersPage/>},
      // {path:"/store/catalog", element: <StoreCatalogPage/>},
      {path:"/operator/home", element: <OperatorPage/>},
      {path:"/courier/home", element: <CourierPage/>},
      {path:"/map", element: <MapWrapper/>},
      {path:'/history', element: <OrderHistoryPage/>}
    ]
  }
])

function App() {
  return <RouterProvider router={router} />
}

export default App
