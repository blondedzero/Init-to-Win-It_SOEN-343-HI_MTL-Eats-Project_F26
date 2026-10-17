import { ROLE } from './EnumRoles'
import { BACKEND_ROUTE } from './Helpers'

export async function register(role, email, password, firstName, lastName, userDetails = {}) {
  if (!role || !email || !password || !firstName || !lastName) {
    throw new Error('Please fill all required fields.')
  }

  const basePayload = { email, password, firstName, lastName }

  let endpoint = ''
  let payload = basePayload

  switch (role) {
    case ROLE[0]: // CUSTOMER
      if (!userDetails.address) {
        throw new Error('Address is required for customers.')
      }
      endpoint = `${BACKEND_ROUTE}/api/users/register/customer`
      payload = { ...basePayload, address: userDetails.address }
      break

    case ROLE[1]: // STORE
      if (!userDetails.address || !userDetails.storeName) {
        throw new Error('Address and store name are required for store registrations.')
      }
      endpoint = `${BACKEND_ROUTE}/api/users/register/store`
      payload = { ...basePayload, address: userDetails.address, storeName: userDetails.storeName }
      break

    case ROLE[2]: // COURIER
      if (!userDetails.carMake || !userDetails.carModel || !userDetails.carYear || !userDetails.carColor) {
        throw new Error('Vehicle details are required for courier registrations.')
      }
      endpoint = `${BACKEND_ROUTE}/api/users/register/courier`
      payload = {
        ...basePayload,
        carMake: userDetails.carMake,
        carModel: userDetails.carModel,
        carYear: userDetails.carYear,
        carColor: userDetails.carColor,
      }
      break

    case ROLE[3]: // OPERATOR
      endpoint = `${BACKEND_ROUTE}/api/users/register/operator`
      payload = basePayload
      break

    default:
      throw new Error('Invalid role selected.')
  }

  const response = await fetch(endpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Registration failed. Please try again.')
  }

  return response
}