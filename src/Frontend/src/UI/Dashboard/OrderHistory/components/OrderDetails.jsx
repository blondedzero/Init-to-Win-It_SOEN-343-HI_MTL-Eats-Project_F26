import { ORDERSTATUS } from '../../../../api/Orders'
import { money } from '../../../../utils/format'

export default function OrderDetails({ order }) {
    return (
        <div className="details">
            <span>{order.lines.map((line) => `${line.quantity} × ${line.name}`).join(', ')}</span>
            <span className="total">Total {money(order.totalCost)}</span>
            {order.status == ORDERSTATUS.REJECTED && order.rejectionReason &&
                <span className="reason">Rejected: {order.rejectionReason}</span>}
        </div>
    )
}
