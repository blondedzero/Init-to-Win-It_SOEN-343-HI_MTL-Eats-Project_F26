import { money } from '../../../../utils/format'

export default function PricingSummary({ pricing, isOperator }) {
    return (
        <div className="pricing-details">
            <strong>Pricing summary</strong>
            <span>Items subtotal: {money(pricing.subtotal)}</span>
            <span>Small order fee: {money(pricing.smallOrderFee)}</span>
            <span>Service fee: {money(pricing.serviceFee)}</span>
            <span>Delivery fee: {money(pricing.deliveryFee)}</span>
            <span>Tax: {money(pricing.taxAmount)}</span>
            {isOperator && <span>PRC rule version: {pricing.ruleVersion ?? 'Unavailable'}</span>}
        </div>
    )
}
