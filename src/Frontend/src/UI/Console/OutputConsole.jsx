import { useEffect, useRef, useSyncExternalStore } from 'react'
import { clearConsole, getEntries, subscribe } from './consoleLog'

export default function OutputConsole() {
  const entries = useSyncExternalStore(subscribe, getEntries)
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: 'nearest' })
  }, [entries])

  return (
    <section className="output-console">
      <div className="output-console-header">
        <span>Output console</span>
        <button type="button" onClick={clearConsole}>Clear</button>
      </div>
      <div className="output-console-body">
        {entries.length === 0 && <div className="console-line console-muted">No commands yet.</div>}
        {entries.map((entry) => (
          <div key={entry.id} className={`console-line console-${entry.level}`}>
            <span className="console-time">{entry.time}</span> {entry.text}
          </div>
        ))}
        <div ref={bottomRef} />
      </div>
    </section>
  )
}
