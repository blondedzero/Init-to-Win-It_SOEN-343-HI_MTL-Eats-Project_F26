// Singleton promise to ensure the Google Maps script is only loaded once globally -> R-DASH-07 & Issue 5.7.1-7
let scriptLoadPromise = null

// Injects and loads the Google Maps JavaScript API script once -> Issue 5.7.1-1
export function loadGoogleMaps(apiKey) {
    if (window.google && window.google.maps) {
        return Promise.resolve(window.google.maps)
    }
    if (scriptLoadPromise) {
        return scriptLoadPromise
    }

    scriptLoadPromise = new Promise((resolve, reject) => {
        if (!apiKey) {
            reject(new Error('VITE_GOOGLE_MAPS_API_KEY is missing from Frontend/.env.local'))
            return
        }

        const script = document.createElement('script')
        script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}`
        script.async = true
        script.defer = true

        script.onload = () => {
            if (window.google && window.google.maps) {
                resolve(window.google.maps)
            } else {
                reject(new Error('Google Maps script loaded, but window.google.maps is undefined.'))
            }
        }

        script.onerror = () => {
            scriptLoadPromise = null
            reject(new Error('Failed to load Google Maps script. Check your network or API key.'))
        }

        document.head.appendChild(script)
    })

  return scriptLoadPromise
}

// Determines store marker color based on operational status and queue length -> R-DASH-03 & Issue 5.7.1-3
export function getStoreColor(store) {
    const status = (store.status || 'OPEN').toUpperCase()
    const queueLength = Number(store.size ?? store.queue?.length ?? 0)

    if (status === 'PAUSED' || status === 'CLOSED' || queueLength >= 6) {
        return '#dc2626' // Red
    }
    if (queueLength >= 3) {
        return '#d97706' // Amber
    }
    return '#16a34a' // Green
}

// Safely parses numeric coordinates -> R-DASH-06 & Issue 5.7.1-6 
export function parseCoordinate(val) {
    // returns null if empty or invalid, otherwise returns the number (protects against mapping errors)
    if (val === null || val === undefined || val === '') return null
    const parsed = Number(val)
    return !isNaN(parsed) ? parsed : null
}