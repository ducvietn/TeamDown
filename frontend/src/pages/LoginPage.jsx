import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApp } from '../context/AppContext.jsx'
import { userApi, groupApi } from '../api/index.js'

export default function LoginPage() {
  const { login, selectGroup } = useApp()
  const navigate = useNavigate()

  const [mode, setMode] = useState('select-user') // select-user | select-group | login
  const [users, setUsers] = useState([])
  const [groups, setGroups] = useState([])
  const [selectedUser, setSelectedUser] = useState(null)
  const [selectedGroup, setSelectedGroup] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // Step 1 — Load users
  const loadUsers = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await userApi.getAll()
      setUsers(data || [])
      setMode('select-user')
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  // Step 2 — Load groups for selected user
  const selectUser = async (user) => {
    setSelectedUser(user)
    setLoading(true)
    setError('')
    try {
      const data = await groupApi.getAll()
      setGroups(data || [])
      setMode('select-group')
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  // Step 3 — Confirm login
  const confirmLogin = () => {
    login(selectedUser)
    selectGroup(selectedGroup)
    navigate('/')
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-600 to-primary-800 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-white rounded-2xl shadow-lg mb-4">
            <span className="text-primary-600 font-black text-2xl">TU</span>
          </div>
          <h1 className="text-3xl font-bold text-white">TeamUp</h1>
          <p className="text-primary-100 mt-1">Quản lý dự án nhóm sinh viên</p>
        </div>

        <div className="card p-8">
          {mode === 'select-user' && (
            <>
              <h2 className="text-lg font-semibold text-slate-800 mb-1">Chọn tài khoản</h2>
              <p className="text-sm text-slate-500 mb-4">Chọn tài khoản để đăng nhập</p>

              {loading ? (
                <div className="text-center py-8 text-slate-400">Đang tải...</div>
              ) : error ? (
                <div className="text-center py-8">
                  <p className="text-danger text-sm mb-3">{error}</p>
                  <button onClick={loadUsers} className="btn-outline text-sm">Thử lại</button>
                </div>
              ) : (
                <div className="space-y-2">
                  {users.map((u) => (
                    <button
                      key={u.userId}
                      onClick={() => selectUser(u)}
                      className="w-full flex items-center gap-3 p-3 rounded-lg border border-slate-200 hover:border-primary-400 hover:bg-primary-50 transition-all text-left"
                    >
                      <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center text-primary-700 font-semibold text-sm">
                        {u.name?.[0]?.toUpperCase() || '?'}
                      </div>
                      <div>
                        <p className="text-sm font-medium text-slate-800">{u.name}</p>
                        <p className="text-xs text-slate-500">{u.email}</p>
                      </div>
                      <span className="ml-auto badge bg-slate-100 text-slate-600 text-[10px]">
                        {u.role}
                      </span>
                    </button>
                  ))}
                  {users.length === 0 && (
                    <p className="text-center text-slate-400 py-6 text-sm">
                      Không có tài khoản. Khởi động backend trước.
                    </p>
                  )}
                </div>
              )}
            </>
          )}

          {mode === 'select-group' && (
            <>
              <div className="flex items-center gap-2 mb-1">
                <button onClick={() => setMode('select-user')} className="text-sm text-slate-400 hover:text-slate-600">
                  ← Quay lại
                </button>
              </div>
              <h2 className="text-lg font-semibold text-slate-800 mb-1">Chọn nhóm</h2>
              <p className="text-sm text-slate-500 mb-4">
                Xin chào <strong>{selectedUser?.name}</strong> — chọn nhóm để tiếp tục
              </p>

              {loading ? (
                <div className="text-center py-8 text-slate-400">Đang tải...</div>
              ) : error ? (
                <p className="text-danger text-sm">{error}</p>
              ) : (
                <>
                  <div className="space-y-2 mb-6">
                    {groups.map((g) => (
                      <button
                        key={g.groupId}
                        onClick={() => setSelectedGroup(g)}
                        className={`w-full flex items-center gap-3 p-3 rounded-lg border transition-all text-left ${
                          selectedGroup?.groupId === g.groupId
                            ? 'border-primary-500 bg-primary-50'
                            : 'border-slate-200 hover:border-primary-300'
                        }`}
                      >
                        <div className="w-10 h-10 bg-slate-100 rounded-full flex items-center justify-center text-slate-600 font-semibold text-sm">
                          {g.groupName?.[0]?.toUpperCase()}
                        </div>
                        <div>
                          <p className="text-sm font-medium text-slate-800">{g.groupName}</p>
                          <p className="text-xs text-slate-500">{g.classId}</p>
                        </div>
                        {g.leader?.userId === selectedUser?.userId && (
                          <span className="ml-auto badge bg-primary-100 text-primary-700 text-[10px]">
                            Leader
                          </span>
                        )}
                      </button>
                    ))}
                    {groups.length === 0 && (
                      <p className="text-center text-slate-400 py-6 text-sm">
                        Không có nhóm nào. Tạo nhóm trước.
                      </p>
                    )}
                  </div>

                  <button
                    onClick={confirmLogin}
                    disabled={!selectedGroup}
                    className="btn-primary w-full disabled:opacity-40"
                  >
                    Tiếp tục với "{selectedGroup?.groupName || '...'}"
                  </button>
                </>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
