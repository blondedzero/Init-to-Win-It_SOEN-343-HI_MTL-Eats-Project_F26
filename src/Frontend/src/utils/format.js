export const money = (value) => `$${Number(value).toFixed(2)}`

export const formatDateTime = (value) => new Date(value).toLocaleString()
