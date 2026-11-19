import { ORDERSTATUS } from "../../api/Orders";

export default function OrderHistoryLine({order}) {
    return(
        <div className="history-line">
            <div className="order-id">Order #{order.orderId}</div>
            <div className="customer">{order.customerName}</div>
            <div className={`status ${String(order.status).toLowerCase()}`}>{order.status}</div>
            {order.status == ORDERSTATUS.DELIVERED && <div className="timestamp">Delivered at: {order.deliveredAt}</div>}
            {order.status == ORDERSTATUS.CANCELED && <div className="timestamp">Canceled at: {order.canceledAt}</div>}
        </div>
    )
}