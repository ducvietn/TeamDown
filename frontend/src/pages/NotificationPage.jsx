import { useState, useEffect } from 'react'
import { useApp } from '../context/AppContext.jsx'
import { notificationApi } from '../api/index.js'
import { Bell, CheckCheck, Clock, AlertTriangle, CheckCircle, Info } from 'lucide-react'

const TYPE_CONFIG = {
  INACTIVITY_WARNING: { icon: AlertTriangle, color: 'text-amber-500', bg: 'bg-amber-50 border-amber-200' },
  TASK_APPROVED:      { icon: CheckCircle,  color: 'text-emerald-500', bg: 'bg-emerald-50 border-emerald-200' },
  TASK_REJECTED:      { icon: Clock,         color: 'text-red-500',   bg: 'bg-red-50 border-red-200' },
}

export default function NotificationPage() {
  const { currentUser } = useApp()
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(false)
  const [unreadOnly, setUnreadOnly] = useState(false)

  useEffect(() => { load() }, [unreadOnly])

  const load = async () => {
    if (!currentUser) return
    setLoading(true)
    try {
      const data = await notificationApi.getAll(currentUser.userId, unreadOnly)
      setNotifications(Array.isArray(data) ? data : [])
    } catch (_) {}
    setLoading(false)
  }

  const markAllRead = async () => {
    try {
      await notificationApi.markAllAsRead(currentUser.userId)
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })))
    } catch (_) {}
  }

  const markAsRead = async (id) => {
    try {
      await notificationApi.markAsRead(id)
      setNotifications(prev => prev.map(n => n.notificationId === id ? { ...n, isRead: true } : n))
    } catch (_) {}
  }

  const unreadCount = notifications.filter(n => !n.isRead).length

  return (
    <div className="p-6 max-w-3xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
            <Bell size={24} className="text-primary-500" />
            Thông báo
          </h1>
          {unreadCount > 0 && (
            <p className="text-sm text-slate-500">{unreadCount} thông báo chưa đọc</p>
          )}
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setUnreadOnly(v => !v)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
              unreadOnly ? 'bg-primary-600 text-white' : 'btn-outline'
            }`}
          >
            {unreadOnly ? 'Đang lọc: Chưa đọc' : 'Lọc chưa đọc'}
          </button>
          {unreadCount > 0 && (
            <button onClick={markAllRead} className="btn-outline text-sm flex items-center gap-1">
              <CheckCheck size={14} />
              Đọc tất cả
            </button>
          )}
        </div>
      </div>

      {/* Notifications list */}
      {loading ? (
        <div className="text-center py-12 text-slate-400">Đang tải thông báo...</div>
      ) : notifications.length === 0 ? (
        <div className="card p-8 text-center">
          <Bell size={40} className="mx-auto text-slate-200 mb-3" />
          <p className="text-slate-400">
            {unreadOnly ? 'Không có thông báo chưa đọc.' : 'Không có thông báo nào.'}
          </p>
        </div>
      ) : (
        <div className="space-y-2">
          {notifications.map(n => {
            const cfg = TYPE_CONFIG[n.type] || TYPE_CONFIG.INACTIVITY_WARNING
            const Icon = cfg.icon
            return (
              <div
                key={n.notificationId}
                onClick={() => !n.isRead && markAsRead(n.notificationId)}
                className={`card p-4 flex items-start gap-3 cursor-pointer transition-all hover:shadow-sm ${
                  n.isRead ? 'opacity-70' : ''
                } ${cfg.bg}`}
              >
                <div className={`w-9 h-9 rounded-full flex items-center justify-center shrink-0 ${cfg.color} bg-white`}>
                  <Icon size={16} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-slate-800 leading-relaxed">{n.message}</p>
                  <p className="text-xs text-slate-400 mt-1 flex items-center gap-1">
                    <Clock size={10} />
                    {new Date(n.createdAt).toLocaleString('vi-VN')}
                    {n.isRead && <span className="ml-2 text-success">✓ Đã đọc</span>}
                  </p>
                </div>
                {!n.isRead && (
                  <div className="w-2 h-2 bg-primary-500 rounded-full mt-2 shrink-0" />
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
