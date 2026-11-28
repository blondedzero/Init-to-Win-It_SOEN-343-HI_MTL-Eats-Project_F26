import { ROLE } from '../../api/EnumRoles'
import StoreRating from '../CustomerPage/StoreRating'

export default function StorePopupContent({ store, queueLength, user }) {
    const name = store.storeName || 'Grocery Store'

    return (
        <div style={{ fontFamily: 'system-ui, sans-serif', fontSize: '13px', color: '#1f2937', padding: '2px' }}>
            <strong>{name} - #{store.id}</strong><br />
            Status: {store.status || 'OPEN'}<br />
            Queue: <strong>{queueLength}</strong> orders
            <StoreRating storeId={store.id} />
            {user?.role === ROLE.CUSTOMER &&
                <a
                    href={`/stores/${encodeURIComponent(store.id)}`}
                    style={{ display: 'inline-block', marginTop: '8px', color: '#2563eb' }}
                >
                    Browse menu
                </a>}
        </div>
    )
}
