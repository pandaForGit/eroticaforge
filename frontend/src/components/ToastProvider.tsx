import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { ToastContext } from './toastContext'

type ToastKind = 'error' | 'info'

export function ToastProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)
  const [message, setMessage] = useState('')
  const [kind, setKind] = useState<ToastKind>('info')

  const push = useCallback((next: string, nextKind: ToastKind) => {
    setMessage(next)
    setKind(nextKind)
    setOpen(true)
    window.setTimeout(() => setOpen(false), 6500)
  }, [])

  const showError = useCallback((msg: string) => push(msg, 'error'), [push])
  const showInfo = useCallback((msg: string) => push(msg, 'info'), [push])

  const value = useMemo(() => ({ showError, showInfo }), [showError, showInfo])

  return (
    <ToastContext.Provider value={value}>
      {children}
      {open ? (
        <div
          role="alert"
          className={
            kind === 'error' ? 'ef-toast ef-toast--error' : 'ef-toast ef-toast--info'
          }
        >
          {message}
        </div>
      ) : null}
    </ToastContext.Provider>
  )
}
