import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useApp } from '../../context/AppContext.jsx'
import { LayoutDashboard, ListTodo, UserCheck, Bell, BarChart2, LogOut, Users } from 'lucide-react'
import { useEffect, useState } from 'react'
import { notificationApi } from '../../api/index.js'

export default function Layout() {
  const { currentUser, currentGroup, logout, isLeader, isTeacher } = useApp()
  const navigate = useNavigate()
  const [unreadCount, setUnreadCount] = useState(0)

  useEffect(() => {
    if (!currentUser) return
    const poll = setInterval(async () => {
      try {
        const count = await notificationApi.getUnreadCount(currentUser.userId)
        setUnreadCount(Number(count) || 0)
      } catch (_) {}
    }, 30_000)
    return () => clearInterval(poll)
  }, [currentUser])

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const navItem = (to, icon: Icon, label) => (
    <NavLink
      key={to}
      to={to}
      className={({ isActive }) =>
        `flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
          isActive
            ? 'bg-primary-600 text-white'
            : 'text-slate-600 hover:bg-slate-100'
        }`
      }
    >
      {icon}
      {label}
    </NavLink>
  )

  return (
    <div className="min-h-screen flex">
      {/* ── Sidebar ─────────────────────────────────────── */}
      <aside className="w-64 bg-white border-r border-slate-200 flex flex-col shrink-0">
        {/* Logo */}
        <div className="px-6 py-5 border-b border-slate-100">
          <h1 className="text-xl font-bold text-primary-600 flex items-center gap-2">
            <span className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center text-white text-sm">
              TU
            </span>
            TeamUp
          </h1>
        </div>

        {/* User & Group info */}
        <div className="px-6 py-4 border-b border-slate-100">
          <p className="text-sm font-semibold text-slate-800">{currentUser?.name}</p>
          <p className="text-xs text-slate-500">{currentUser?.email}</p>
          {currentGroup && (
            <div className="mt-2 flex items-center gap-1.5 bg-slate-50 rounded-md px-2 py-1">
              <Users size={12} className="text-slate-400" />
              <span className="text-xs text-slate-600">{currentGroup.groupName}</span>
              {isLeader() && (
                <span className="ml-auto text-[10px] bg-primary-100 text-primary-700 px-1.5 py-0.5 rounded font-medium">
                  Leader
                </span>
              )}
            </div>
          )}
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-4 py-4 space-y-1">
          {navItem('/', <ListTodo size={18} />, 'Công việc')}
          {navItem('/dashboard', <LayoutDashboard size={18} />, 'Dashboard')}
          {navItem('/my-reviews', <UserCheck size={18} />, 'Đánh giá đồng nghiệp')}

          {isLeader() &&
            navItem('/leader-review', <UserCheck size={18} />, 'Duyệt bài (Leader)')}

          {isTeacher() &&
            navItem('/reports', <BarChart2 size={18} />, 'Xuất báo cáo')}

          {navItem('/notifications', (
            <span className="relative">
              <Bell size={18} />
              {unreadCount > 0 && (
                <span className="absolute -top-1.5 -right-1.5 w-4 h-4 bg-danger rounded-full text-white text-[10px] flex items-center justify-center font-bold">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </span>
          ), 'Thông báo')}
        </nav>

        {/* Logout */}
        <div className="px-4 pb-4">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium text-slate-500 hover:bg-red-50 hover:text-danger w-full transition-colors"
          >
            <LogOut size={18} />
            Đăng xuất
          </button>
        </div>
      </aside>

      {/* ── Main Content ─────────────────────────────────── */}
      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  )
}
