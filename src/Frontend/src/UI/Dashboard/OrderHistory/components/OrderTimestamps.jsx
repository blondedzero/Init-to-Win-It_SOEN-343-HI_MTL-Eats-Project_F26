import { ORDERSTATUS } from '../../../../api/Orders'
import { formatDateTime } from '../../../../utils/format'

export default function OrderTimestamps({ order }) {
    return (
        <>
            {[ORDERSTATUS.DELIVERED, ORDERSTATUS.REFUNDED].includes(order.status) && order.deliveredAt &&
                <div className="timestamp">Delivered at: {formatDateTime(order.deliveredAt)}</div>}
            {[ORDERSTATUS.CANCELED, ORDERSTATUS.REFUNDED].includes(order.status) && order.canceledAt &&
                <div className="timestamp">Canceled at: {formatDateTime(order.canceledAt)}</div>}
            {order.status == ORDERSTATUS.REJECTED &&
                <div className="timestamp">Rejected at: {formatDateTime(order.canceledAt)}</div>}
        </>
    )
}
