export function OperatorOrderCard({order}) {
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
            <br/> Courier: {order.courier || "UNNASSIGNED"}
        </p>
        </article>
    )
}

export function OperatorStoreCard({store}) {
    return(
        <article className="dash-card">
        <div className="dash-title-row">
            <h4 className="dash-card-title">
            {store.storeName} - #{store.orderId}
            </h4>
            <span className={`dash-badge dash-badge-${store.status.toLowerCase()}`}>{store.status}</span>
        </div>
        <p className="dash-muted">
            Orders in progress: {store.ordersInProgress.length}
            Orders ready: {store.ordersReady.length}
        </p>
        </article>
    )
}