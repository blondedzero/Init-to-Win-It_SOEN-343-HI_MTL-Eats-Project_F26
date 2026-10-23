import './customer-page.css'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getOpenStores } from '../../api/Store'

export default function CustomerHomePage() {
  const [stores, setStores] = useState([])
  const [error, setError] = useState('')
  const navigate = useNavigate()


  useEffect(() => {
      console.log('fetching stores…')
    getOpenStores().then(setStores).catch(e => setError(e.message))
  }, [])

 return (

     <div className="scroll-list">
       {stores.map((store) => (
         <div key={store.id} className="store-row">
                <div className="store-info">
                       <span className="store-name">{store.storeName}</span>
                       <span className="store-distance">{store.address}</span>
                     </div>
           <button className="browse-button" onClick={() => navigate(`/stores/${store.id}`)}>Browse</button>
         </div>
       ))}
     </div>
   )
}