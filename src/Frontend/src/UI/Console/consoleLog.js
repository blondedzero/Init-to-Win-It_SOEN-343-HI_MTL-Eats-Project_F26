let entries = []
const listeners = new Set()
let nextId = 1

function append(level, text) {
  entries = [...entries, { id: nextId++, level, text, time: new Date().toLocaleTimeString() }].slice(-200)
  listeners.forEach((listener) => listener())
}

export function logCommand(text) {
  append('command', text)
}

export function logSuccess(text) {
  append('success', text)
}

export function logError(text) {
  append('error', text)
}

export function clearConsole() {
  entries = []
  listeners.forEach((listener) => listener())
}

export function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function getEntries() {
  return entries
}
