const money = (value) => `$${Number(value).toFixed(2)}`

export default function OrderCard({ order, position, children }) {
  return (
    <article className="dash-card">
      <div className="dash-title-row">
        <h4 className="dash-card-title">
          {position != null && <span className="dash-position">#{position}</span>}
          Order {order.orderId}
        </h4>
        <span className={`dash-badge dash-badge-${order.status.toLowerCase()}`}>{order.status}</span>
      </div>
      <p className="dash-muted">
        {order.customerName} · {new Date(order.placedAt).toLocaleTimeString()}
      </p>
      <ul className="dash-lines">
        {order.lines.map((line) => (
          <li key={line.productId}>{line.quantity} × {line.name}</li>
        ))}
      </ul>
      <p className="dash-total">Total {money(order.totalCost)}</p>
      {children}
    </article>
  )
}

export function OrderReadyCard({order}) {
  return (
    <article className="dash-card">
      <div className="dash-title-row">
        <h4 className="dash-card-title">
          Order {order.orderId}
        </h4>
        <span className={`dash-badge dash-badge-${order.status.toLowerCase()}`}>{order.status}</span>
      </div>
      <p className="dash-muted">
        {order.customerName} · {new Date(order.placedAt).toLocaleTimeString()}
        <br/> Ready at: {new Date(order.readyAt).toLocaleTimeString()}
      </p>
    </article>
  )
}
