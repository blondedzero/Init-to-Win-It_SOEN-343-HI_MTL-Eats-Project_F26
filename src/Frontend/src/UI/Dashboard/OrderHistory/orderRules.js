import { ORDERSTATUS } from '../../../api/Orders'
import { ROLE } from '../../../api/EnumRoles'

export const canRefund = (order, role) =>
    role === ROLE.OPERATOR &&
    [ORDERSTATUS.DELIVERED, ORDERSTATUS.CANCELED].includes(order.status) &&
    !order.refund

export const canRate = (order, role) =>
    role === ROLE.CUSTOMER &&
    order.status === ORDERSTATUS.DELIVERED &&
    !order.rating
