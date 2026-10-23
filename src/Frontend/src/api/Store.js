import { BACKEND_ROUTE } from './Helpers'

export async function getOpenStores() {
  const res = await fetch(`${BACKEND_ROUTE}/api/stores`)
  if (!res.ok) throw new Error('Could not load stores')
  return res.json()