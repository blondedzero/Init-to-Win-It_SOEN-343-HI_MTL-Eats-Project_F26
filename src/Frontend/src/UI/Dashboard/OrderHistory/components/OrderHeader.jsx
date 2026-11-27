export default function OrderHeader({ order, isCustomer }) {
    return (
        <>
            <div className="order-id">Order #{order.orderId}</div>
            <div className="customer">{isCustomer ? order.storeName : order.customerName}</div>
            <div className={`status ${String(order.status).toLowerCase()}`}>{order.status}</div>
        </>
    )
}
