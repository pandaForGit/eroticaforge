import { useContext } from 'react'
import { ToastContext, type ToastContextValue } from './toastContext'

export function useToast(): ToastContextValue {
  const v = useContext(ToastContext)
  if (!v) {
    throw new Error('useToast 必须在 ToastProvider 内使用')
  }
  return v
}
