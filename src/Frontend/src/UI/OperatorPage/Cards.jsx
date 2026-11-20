import { useState } from "react"
import { COURIERSTATUS } from "../../api/Courier"
import { assignOrderToCourier, cancelOrder, closeStore, openStore, pauseStore } from "../../api/OperatorRequests"
import { ORDERSTATUS } from "../../api/Orders"
import { STORESTATUS } from "../../api/Store"
import { TABS } from "./OperatorPage"

export function OperatorOrderCard({order, tab, handleOperation, stores = [], idleCouriers = []}) {
    const [showCouriers, setShowCouriers] = useState(false)
    const [assigningCourierId, setAssigningCourierId] = useState(null)
    const store = stores.find((candidate) => candidate.id === order.storeId)
    const couriersByDistance = idleCouriers
        .map((courier) => ({ courier, distance: getDistanceKm(store, courier) }))
        .sort((a, b) => {
            if (a.distance !== null && b.distance !== null && a.distance !== b.distance) {
                return a.distance - b.distance
            }
            if (a.distance === null && b.distance !== null) return 1
            if (a.distance !== null && b.distance === null) return -1
            return a.courier.courierId - b.courier.courierId
        })

    async function assignCourier(courierId) {
        setAssigningCourierId(courierId)
        try {
            const assigned = await handleOperation(assignOrderToCourier, courierId, order.orderId)
            if (assigned) setShowCouriers(false)
        } finally {
            setAssigningCourierId(null)
        }
    }

    return(
        <article className="dash-card">
        <div className="dash-title-row">
            <h4 className="dash-card-title">
            Order {order.orderId}
            </h4>
            <span className={`dash-badge dash-badge-${order.status.toLowerCase()}`}>{order.status}</span>
        </div>
        <p className="dash-muted">
            Customer: {order.customerName} - {order.deliveryAddress}
            <br/> Store: {order.storeName}
            {order.status == ORDERSTATUS.READY && <><br/> Courier: {order.courierName || (order.courierId != null ? `#${order.courierId}` : "Unassigned")}</>}
        </p>
        {tab == TABS.ORDERS && 
        <div className="op-order-buttons">
            {order.status == ORDERSTATUS.READY && order.courierId == null && <>
                <button type="button" onClick={() => setShowCouriers((visible) => !visible)} aria-expanded={showCouriers} aria-controls={`courier-options-${order.orderId}`}>
                    {showCouriers ? "Hide couriers" : "Assign courier"}
                </button>
                {showCouriers && <div className="op-courier-options" id={`courier-options-${order.orderId}`}>
                    <p className="dash-muted">Available couriers, closest to the store first</p>
                    {couriersByDistance.length === 0
                        ? <p className="dash-muted">No idle couriers are currently available.</p>
                        : couriersByDistance.map(({courier, distance}) => (
                            <div className="op-courier-option" key={courier.courierId}>
                                <span>
                                    {courier.name} - #{courier.courierId}
                                    <small>{distance === null ? "Distance unavailable" : `${distance.toFixed(1)} km away`}</small>
                                </span>
                                <button type="button" disabled={assigningCourierId !== null} onClick={() => assignCourier(courier.courierId)}>
                                    {assigningCourierId === courier.courierId ? "Assigning..." : "Assign"}
                                </button>
                            </div>
                        ))}
                </div>}
            </>}
            <button onClick={() => {
                if (window.confirm(`Confirm cancel order #${order.orderId}?`)) {
                    handleOperation(cancelOrder, order.orderId)
                }
            }}>Cancel order</button>
        </div>}
        </article>
    )
}

function getDistanceKm(store, courier) {
    const storeLatitude = parseCoordinate(store?.latitude)
    const storeLongitude = parseCoordinate(store?.longitude)
    const courierLatitude = parseCoordinate(courier.latitude)
    const courierLongitude = parseCoordinate(courier.longitude)
    if ([storeLatitude, storeLongitude, courierLatitude, courierLongitude].some((coordinate) => coordinate === null)) {
        return null
    }

    const radians = (degrees) => degrees * Math.PI / 180
    const latitudeDelta = radians(courierLatitude - storeLatitude)
    const longitudeDelta = radians(courierLongitude - storeLongitude)
    const a = Math.sin(latitudeDelta / 2) ** 2
        + Math.cos(radians(storeLatitude)) * Math.cos(radians(courierLatitude))
        * Math.sin(longitudeDelta / 2) ** 2
    const boundedA = Math.min(1, Math.max(0, a))
    return 6371 * 2 * Math.atan2(Math.sqrt(boundedA), Math.sqrt(1 - boundedA))
}

function parseCoordinate(value) {
    if (value === null || value === undefined || value === '') return null
    const coordinate = Number(value)
    return Number.isFinite(coordinate) ? coordinate : null
}

export function OperatorStoreCard({store, handleOperation, tab}) {
    return(
        <article className="dash-card">
        <div className="dash-title-row">
            <h4 className="dash-card-title">
            {store.storeName} - #{store.id}
            </h4>
            <span className={`dash-badge dash-badge-${store.status.toLowerCase()}`}>{store.status}</span>
        </div>
        
        {store.status != STORESTATUS.CLOSED && <p className="dash-muted">
            Orders in progress: {store.ordersInProgress}
            <br/>Orders ready: {store.ordersReady}
        </p>}

        {tab == TABS.OVERVIEW && 
            <>
            {(store.status == STORESTATUS.OPEN) && <button 
                className={`op-button-pause`}
                onClick={() => handleOperation(pauseStore, store.id)}
                >
                    Pause Store
            </button>}

            {store.status != STORESTATUS.OPEN && <button 
                className={`op-button-open`}
                onClick={() => handleOperation(openStore, store.id)}
                >
                    Open Store
            </button>}

            {store.status != STORESTATUS.CLOSED && <button 
                className={`op-button-close`}
                onClick={() => handleOperation(closeStore, store.id)}
                >
                    Close Store
            </button>}
            </>
        }
        </article>
    )
}

export function OperatorCourierCard({courier}) {
    return(
        <article className="dash-card">
        <div className="dash-title-row">
            <h4 className="dash-card-title">
            {courier.name} - #{courier.courierId}
            </h4>
            <span className={`dash-badge dash-badge-${courier.status.toLowerCase()}`}>{courier.status}</span>
        </div>
        { courier.status != COURIERSTATUS.OFFLINE && <p className="dash-muted">
            TBD
        </p>}
        </article>
    )
}