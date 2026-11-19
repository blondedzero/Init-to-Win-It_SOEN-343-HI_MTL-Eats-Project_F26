import { ORDERSTATUS } from "../../api/Orders";
import { ROLE } from "../../api/EnumRoles";

const money = (value) => `$${Number(value).toFixed(2)}`

export default function OrderHistoryLine({order, role}) {
    const isCustomer = role == ROLE.CUSTOMER
    return(
        <div className="history-line">
            <div className="order-id">Order #{order.orderId}</div>
            {/* Customers see which store the order was from instead of their own name */}
            <div className="customer">{isCustomer ? order.storeName : order.customerName}</div>
            <div className={`status ${String(order.status).toLowerCase()}`}>{order.status}</div>
            {order.status == ORDERSTATUS.DELIVERED && <div className="timestamp">Delivered at: {new Date(order.deliveredAt).toLocaleString()}</div>}
            {order.status == ORDERSTATUS.CANCELED && <div className="timestamp">Canceled at: {new Date(order.canceledAt).toLocaleString()}</div>}
            {/* Order.markRejected() records the rejection time in canceledAt */}
            {order.status == ORDERSTATUS.REJECTED && <div className="timestamp">Rejected at: {new Date(order.canceledAt).toLocaleString()}</div>}
            {isCustomer && (
                <div className="details">
                    <span>{order.lines.map((line) => `${line.quantity} × ${line.name}`).join(', ')}</span>
                    <span className="total">Total {money(order.totalCost)}</span>
                    {order.status == ORDERSTATUS.REJECTED && order.rejectionReason &&
                        <span className="reason">Rejected: {order.rejectionReason}</span>}
                </div>
            )}
        </div>
    )
}
