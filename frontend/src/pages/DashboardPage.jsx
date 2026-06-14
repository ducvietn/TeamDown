import { useState } from 'react'
import { useApp } from '../../context/AppContext.jsx'
import { dashboardApi } from '../../api/index.js'
import {
  PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid
} from 'recharts'
import { RefreshCw, Clock, CheckCircle, AlertCircle, TrendingUp } from 'lucide-react'

const CHART_COLORS = [
  '#3b82f6', '#10b981', '#f59e0b', '#ef4444',
  '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16',
]

export default function DashboardPage() {
  const { currentGroup } = useApp()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = async () => {
    if (!currentGroup?.groupId) return
    setLoading(true)
    setError('')
    try {
      const result = await dashboardApi.getGroupDashboard(currentGroup.groupId)
      setData(result)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const pieData = data?.members?.map((m, i) => ({
    name:  m.name,
    value: Math.round((m.contributionPercent || 0) * 100) / 100,
    fill:  CHART_COLORS[i % CHART_COLORS.length],
  })) || []

  const barData = data?.members?.map(m => ({
    name:   m.name.split(' ').slice(-1)[0],  // last word = surname
    Tiến:  Math.round(m.averageProgress * 10) / 10,
    Hoàn:  Math.round((m.contributionPercent || 0) * 10) / 10,
  })) || []

  const stats = data?.stats

  return (
    <div className="p-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Dashboard</h1>
          <p className="text-sm text-slate-500">
            {data?.groupName || currentGroup?.groupName || ''} — cập nhật thời gian thực
          </p>
        </div>
        <button onClick={load} disabled={loading} className="btn-outline text-sm flex items-center gap-1.5">
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          Làm mới
        </button>
      </div>

      {/* Last updated */}
      {data?.generatedAt && (
        <p className="text-xs text-slate-400 mb-4">
          Dữ liệu cập nhật lúc: {new Date(data.generatedAt).toLocaleString('vi-VN')}
        </p>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4 text-sm text-red-700">{error}</div>
      )}

      {/* Quick stats */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          {[
            { label: 'Tổng công việc', value: stats.totalTasks, icon: TrendingUp, color: 'text-primary-600' },
            { label: 'Đã hoàn thành', value: stats.completedTasks, icon: CheckCircle, color: 'text-success' },
            { label: 'Đang xử lý', value: stats.pendingTasks, icon: Clock, color: 'text-warning' },
            { label: 'Trễ deadline', value: stats.overdueTasks, icon: AlertCircle, color: 'text-danger' },
          ].map(({ label, value, icon: Icon, color }) => (
            <div key={label} className="card p-4 flex items-center gap-3">
              <div className={`w-10 h-10 rounded-lg flex items-center justify-center bg-slate-50 ${color}`}>
                <Icon size={20} />
              </div>
              <div>
                <p className="text-2xl font-bold text-slate-800">{value}</p>
                <p className="text-xs text-slate-500">{label}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Charts row */}
      {data && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
          {/* Pie Chart — Contribution % */}
          <div className="card p-6">
            <h2 className="text-base font-semibold text-slate-800 mb-4">% Đóng góp của từng thành viên</h2>
            {pieData.length === 0 ? (
              <p className="text-center text-slate-400 py-12">Chưa có dữ liệu</p>
            ) : (
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    outerRadius={110}
                    innerRadius={60}
                    paddingAngle={3}
                    dataKey="value"
                    label={({ name, value }) => `${name}: ${value}%`}
                    labelLine={{ stroke: '#94a3b8', strokeWidth: 1 }}
                  >
                    {pieData.map((entry, i) => (
                      <Cell key={i} fill={entry.fill} />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(val) => `${val}%`}
                    contentStyle={{ borderRadius: 8, border: '1px solid #e2e8f0', fontSize: 13 }}
                  />
                  <Legend
                    formatter={(val) => <span className="text-slate-600 text-sm">{val}</span>}
                  />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>

          {/* Bar Chart — Progress vs Contribution */}
          <div className="card p-6">
            <h2 className="text-base font-semibold text-slate-800 mb-4">Tiến độ trung bình & Đóng góp</h2>
            {barData.length === 0 ? (
              <p className="text-center text-slate-400 py-12">Chưa có dữ liệu</p>
            ) : (
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={barData} barGap={4}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                  <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} unit="%" />
                  <Tooltip
                    unit="%"
                    contentStyle={{ borderRadius: 8, border: '1px solid #e2e8f0', fontSize: 13 }}
                  />
                  <Bar dataKey="Tiến" fill="#3b82f6" radius={[4, 4, 0, 0]} name="Tiến độ TB" />
                  <Bar dataKey="Hoàn" fill="#10b981" radius={[4, 4, 0, 0]} name="Đóng góp %" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      )}

      {/* Member table */}
      {data?.members && (
        <div className="card overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-100">
            <h2 className="text-base font-semibold text-slate-800">Chi tiết từng thành viên</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50">
                <tr>
                  {['Thành viên', 'Vai trò', 'Công việc', 'Hoàn thành', 'Đang làm', 'Tiến độ TB', '% Đóng góp'].map(h => (
                    <th key={h} className="px-4 py-3 text-left font-semibold text-slate-600">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.members.map((m, i) => (
                  <tr key={m.userId} className="hover:bg-slate-50">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div
                          className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold shrink-0"
                          style={{ backgroundColor: CHART_COLORS[i % CHART_COLORS.length] }}
                        >
                          {m.name?.[0]?.toUpperCase()}
                        </div>
                        <div>
                          <p className="font-medium text-slate-800">{m.name}</p>
                          <p className="text-xs text-slate-400">{m.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`badge ${m.role === 'TEACHER' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'}`}>
                        {m.role === 'TEACHER' ? 'Giáo viên' : 'Sinh viên'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">{m.taskCount}</td>
                    <td className="px-4 py-3 text-center">
                      <span className="text-success font-medium">{m.completedTaskCount || 0}</span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className="text-warning font-medium">{m.pendingTaskCount || 0}</span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                          <div
                            className="h-full bg-primary-500 rounded-full transition-all"
                            style={{ width: `${m.averageProgress || 0}%` }}
                          />
                        </div>
                        <span className="text-xs font-medium text-slate-600 w-10">{Math.round(m.averageProgress || 0)}%</span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="w-20 h-2 bg-slate-100 rounded-full overflow-hidden">
                          <div
                            className="h-full rounded-full transition-all"
                            style={{
                              width: `${m.contributionPercent || 0}%`,
                              backgroundColor: CHART_COLORS[i % CHART_COLORS.length],
                            }}
                          />
                        </div>
                        <span className="text-xs font-bold text-slate-700 w-10">
                          {(m.contributionPercent || 0).toFixed(1)}%
                        </span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!data && !loading && (
        <div className="text-center py-16">
          <button onClick={load} className="btn-primary">
            Tải Dashboard
          </button>
        </div>
      )}
    </div>
  )
}
