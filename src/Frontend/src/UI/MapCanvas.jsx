import { useEffect, useRef, useState } from 'react'
import { loadGoogleMaps, getStoreColor, parseCoordinate } from './mapUtils'
import { logCommand, logSuccess, logError } from '../Console/consoleLog'

export default function MapCanvas({
    stores = [],
    couriers = [],
    user = null,
    onSelectEntity = null,
}) {
    const mapContainerRef = useRef(null)
    const mapInstanceRef = useRef(null)
    const markersRef = useRef([])           // Holds references to all active markers (can be used for cleanup and re-rendering)
    const infoWindowRef = useRef(null)

    const [mapLoaded, setMapLoaded] = useState(false)
    const [errorMsg, setErrorMsg] = useState('')

    // Initialize Google Map on mount
    useEffect(() => {
        let isMounted = true
        const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY

        // Output console logs for every command and error -> R-DASH-06 and Issue 5.7.1-5
        logCommand('DASH: Initializing map canvas...')

        loadGoogleMaps(apiKey)
        .then((maps) => {
            if (!isMounted || !mapContainerRef.current) return

            // Default center on Montreal before bounds scale to dataset
            mapInstanceRef.current = new maps.Map(mapContainerRef.current, {
                center: { lat: 45.5017, lng: -73.5673 },
                zoom: 12,
                mapTypeControl: false,
                streetViewControl: false,
                fullscreenControl: true,
            })

            infoWindowRef.current = new maps.InfoWindow()
            setMapLoaded(true)
            logSuccess('DASH: Map canvas initialized successfully.')
        })
        .catch((err) => {
            if (!isMounted) return
            setErrorMsg(err.message)
            logError(`DASH: ${err.message}`)
        })

        return () => {
            isMounted = false
            // Clears the map container DOM element so that the map can be re-initialized if the component is remounted 
            if (mapContainerRef.current) {
                mapContainerRef.current.innerHTML = ''
            }
        }
    }, [])

    // Render markers and adjust viewport bounds when data updates
    useEffect(() => {
        if (!mapLoaded || !mapInstanceRef.current || !window.google) return

        const maps = window.google.maps
        const map = mapInstanceRef.current

        // Clear existing markers before redrawing
        markersRef.current.forEach((marker) => marker.setMap(null))
        markersRef.current = []

        const validStores = (stores || [])
        .map((s) => ({
            ...s,
            latitude: parseCoordinate(s.latitude),
            longitude: parseCoordinate(s.longitude),
        }))
        .filter((s) => s.latitude !== null && s.longitude !== null)

        const validCouriers = (couriers || [])
        .map((c) => ({
            ...c,
            latitude: parseCoordinate(c.latitude),
            longitude: parseCoordinate(c.longitude),
        }))
        .filter((c) => c.latitude !== null && c.longitude !== null)

        if (validStores.length === 0 && validCouriers.length === 0) {
            return
        }

        // Scale the city map viewport to dataset bounds -> R-DASH-02 & Issue 5.7.1-2
        const bounds = new maps.LatLngBounds()

        const handleMarkerClick = (type, entity, marker, contentHtml) => {
            const role = (user?.role || '').toUpperCase()
            const entityId = entity.accountId || entity.id || 'Unknown'

            logCommand(`DASH: Selected ${type} #${entityId} as role: ${role || 'GUEST'}`)

            // Enforce role permission: Customers cannot inspect courier telemetry -> R-DASH-01 & Issue 5.7.1-4
            if (type === 'COURIER' && role === 'CUSTOMER') {
                logError('DASH: Permission denied - Customers cannot view courier details.')
                return
            }

            infoWindowRef.current.setContent(contentHtml)
            infoWindowRef.current.open(map, marker)

            if (typeof onSelectEntity === 'function') {
                onSelectEntity(type, entity)
            }
        }

        // Render grocery store markers
        validStores.forEach((store) => {
            const position = { lat: store.latitude, lng: store.longitude }
            bounds.extend(position)

            const colour = getStoreColor(store)
            const queueLength = Number(store.size ?? store.queue?.length ?? 0)
            const name = store.storeName || 'Grocery Store'

            const marker = new maps.Marker({
                position,
                map,
                title: `${name} (Queue: ${queueLength})`,
                icon: {
                    path: maps.SymbolPath.CIRCLE,
                    scale: 10,
                    fillColor: colour,
                    fillOpacity: 0.9,
                    strokeColor: '#ffffff',
                    strokeWeight: 2,
                },
            })

            const contentHtml = `
            <div style="font-family: system-ui, sans-serif; font-size: 13px; color: #1f2937; padding: 2px;">
            <strong>${name}</strong><br/>
            Status: ${store.status || 'OPEN'}<br/>
            Queue: <strong>${queueLength}</strong> orders
            </div>
            `

            marker.addListener('click', () => {
                handleMarkerClick('STORE', store, marker, contentHtml)
            })

            markersRef.current.push(marker)
        })

        // Render courier markers using directional arrow icons
        validCouriers.forEach((courier) => {
            const position = { lat: courier.latitude, lng: courier.longitude }
            bounds.extend(position)

            const name = `${courier.firstName || ''} ${courier.lastName || ''}`.trim() || 'Courier'
            const status = (courier.status || 'IDLE').toUpperCase()

            const marker = new maps.Marker({
                position,
                map,
                title: `${name} (${status})`,
                icon: {
                    path: maps.SymbolPath.FORWARD_CLOSED_ARROW,
                    scale: 6,
                    fillColor: '#2563eb',
                    fillOpacity: 1,
                    strokeColor: '#ffffff',
                    strokeWeight: 1.5,
                },
            })

            const contentHtml = `
            <div style="font-family: system-ui, sans-serif; font-size: 13px; color: #1f2937; padding: 2px;">
            <strong>${name}</strong><br/>
            State: ${status}
            </div>
            `

            marker.addListener('click', () => {
                handleMarkerClick('COURIER', courier, marker, contentHtml)
            })

            markersRef.current.push(marker)
        })

        // Fit map viewport bounds to dataset
        if (validStores.length + validCouriers.length === 1) {
            map.setCenter(bounds.getCenter())
            map.setZoom(14)
        } else {
            map.fitBounds(bounds, 50)
        }
    }, [mapLoaded, stores, couriers, user, onSelectEntity])

    // Gracefully handles empty datasets or missing coordinates with a warning message overlay -> R-DASH-06 & Issue 5.7.1-6
    const hasData =
    (stores || []).some((s) => parseCoordinate(s.latitude) !== null) ||
    (couriers || []).some((c) => parseCoordinate(c.latitude) !== null)

    return (
        <div
            style={{
                position: 'relative',
                width: '100%',
                height: '480px',
                borderRadius: '8px',
                overflow: 'hidden',
                border: '1px solid #e5e7eb',
            }}
        >
        {errorMsg && (
            <div
                style={{
                    padding: '16px',
                    backgroundColor: '#fef2f2',
                    color: '#b91c1c',
                    height: '100%',
                    fontSize: '14px',
                }}
            >
                <strong>Map Error:</strong> {errorMsg}
            </div>
        )}

        {!errorMsg && mapLoaded && !hasData && (
            <div
                style={{
                    position: 'absolute',
                    top: '12px',
                    left: '50%',
                    transform: 'translateX(-50%)',
                    zIndex: 5,
                    backgroundColor: '#1f2937',
                    color: '#f3f4f6',
                    padding: '6px 14px',
                    borderRadius: '6px',
                    fontSize: '13px',
                    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                }}
            >
                No active locations with valid coordinates found in dataset.
            </div>
        )}

        {!errorMsg && mapLoaded && (
            <div
                style={{
                    position: 'absolute',
                    bottom: '12px',
                    left: '12px',
                    zIndex: 5,
                    backgroundColor: 'rgba(255, 255, 255, 0.95)',
                    color: '#374151',
                    padding: '8px 12px',
                    borderRadius: '6px',
                    fontSize: '12px',
                    boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
                    display: 'flex',
                    gap: '12px',
                    alignItems: 'center',
                }}
            >
                <span>
                    <span style={{ color: '#16a34a', fontWeight: 'bold' }}>●</span> Short Queue
                </span>
                <span>
                    <span style={{ color: '#d97706', fontWeight: 'bold' }}>●</span> Building
                </span>
                <span>
                    <span style={{ color: '#dc2626', fontWeight: 'bold' }}>●</span> Long / Paused
                </span>
                <span>
                    <span style={{ color: '#2563eb', fontWeight: 'bold' }}>▲</span> Courier
                </span>
            </div>
        )}

            <div
                ref={mapContainerRef}
                style={{ width: '100%', height: '100%', display: errorMsg ? 'none' : 'block' }}
            />
            </div>
    )
}