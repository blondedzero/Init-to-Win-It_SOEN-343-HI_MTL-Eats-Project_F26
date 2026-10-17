import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import './App.css'
import RegisterPage from './UI/RegisterPage'

const router = createBrowserRouter([
  {path:"/", element: <LoginPage/>},
  {path:"/register",element: <RegisterPage/>},
])

function App() {
  return <RouterProvider router={router} />
}

export default App
