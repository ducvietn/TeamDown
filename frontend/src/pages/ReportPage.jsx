import { useState, useEffect } from 'react'
import { useApp } from '../context/AppContext.jsx'
import { reportApi, groupApi } from '../api/index.js'
import { FileText, Download, BarChart2, RefreshCw, Users, BookOpen } from 'lucide-react'

export default function ReportPage() {
  const { currentUser, currentGroup } = useApp()
  const [groups, setGroups] = useState([])
  const [selectedGroupId, setSelectedGroupId] = useState(null)
  const [reportData, setReportData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(null) // 'pdf' | 'excel' | null
  const [error, setError] = useState('')

  useEffect(() => { loadGroups() }, [])

  const loadGroups = async () => {
    try {
      const data = await groupApi.getAll()
      setGroups(Array.isArray(data) ? data : [])
      if (data?.length > 0 && !selectedGroupId) {
        setSelectedGroupId(data[0].groupId)
      }
    } catch (_) {}
  }

  const loadReport = async () => {
    if (!selectedGroupId) return
    setLoading(true)
    setError('')
    try {
      const data = await reportApi.getReportData(selectedGroupId, currentUser.userId)
      setReportData(data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const handleExport = async (type) => {
    setExporting(type)
    try {
      const blob = type === 'pdf'
        ? await reportApi.downloadPdf(selectedGroupId, currentUser.userId)
        : await reportApi.downloadExcel(selectedGroupId, currentUser.userId)

      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `TeamUp_Baocao_${selectedGroupId}.${type === 'pdf' ? 'pdf' : 'xlsx'}`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } catch (e) {
      alert('Lỗi khi xuất file: ' + e.message)
    } finally {
      setExporting(null)
    }
  }

  return (
    <div className="p-6 max-w-6xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
          <BarChart2 size={24} className="text-primary-500" />
          Xuất báo cáo cho giáo viên
        </h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Báo cáo bao gồm % đóng góp, lịch sử nộp bài, và điểm đánh giá đồng nghiệp ẩn danh
        </p>
      </div>

      {/* Group selector */}
      <div className="card p-4 mb-6 flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <Users size={16} className="text-slate-400" />
          <span className="text-sm font-medium text-slate-700">Chọn nhóm:</span>
        </div>
        <select
          value={selectedGroupId || ''}
          onChange={e => setSelectedGroupId(Number(e.target.value))}
          className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400 bg-white min-w-[200px]"
        >
          {groups.map(g => (
            <option key={g.groupId} value={g.groupId}>
              {g.groupName} — {g.classId}
            </option>
          ))}
        </select>
        <button onClick={loadReport} disabled={loading} className="btn-primary text-sm flex items-center gap-1.5">
          <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
          Tải báo cáo
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4 text-sm text-red-700">{error}</div>
      )}

      {/* Export buttons */}
      {reportData && (
        <div className="flex gap-3 mb-6">
          {[
            { type: 'pdf',   label: 'Xuất PDF',   icon: FileText },
            { type: 'excel', label: 'Xuất Excel', icon: BookOpen },
          ].map(({ type, label, icon: Icon }) => (
            <button
              key={type}
              onClick={() => handleExport(type)}
              disabled={!!exporting}
              className="btn-primary text-sm flex items-center gap-2"
            >
              {exporting === type
                ? <RefreshCw size={13} className="animate-spin" />
                : <Icon size={14} />}
              {exporting === type ? 'Đang xuất...' : label}
            </button>
          ))}
        </div>
      )}

      {/* Report preview */}
      {reportData && (
        <div className="space-y-6">
          {/* Summary */}
          <div className="card p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-slate-800">
                Báo cáo: {reportData.groupName}
              </h2>
              <span className="text-xs text-slate-400">
                Tạo lúc: {reportData.generatedAt && new Date(reportData.generatedAt).toLocaleString('vi-VN')}
              </span>
            </div>

            {/* Members table */}
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50">
                  <tr>
                    {['#', 'Thành viên', 'Công việc', 'Hoàn thành', 'Tiến độ TB', '% Đóng góp', 'Điểm TB peer review', 'Số lượt đánh giá'].map(h => (
                      <th key={h} className="px-4 py-3 text-left font-semibold text-slate-600">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {reportData.members?.map((m, i) => (
                    <tr key={m.userId} className="hover:bg-slate-50">
                      <td className="px-4 py-3 text-slate-400">{i + 1}</td>
                      <td className="px-4 py-3">
                        <p className="font-medium text-slate-800">{m.name}</p>
                        <p className="text-xs text-slate-400">{m.email}</p>
                      </td>
                      <td className="px-4 py-3 text-center">{m.taskCount}</td>
                      <td className="px-4 py-3 text-center text-success font-medium">{m.completedTaskCount || 0}</td>
                      <td className="px-4 py-3 text-center">{m.averageProgress?.toFixed(1)}%</td>
                      <td className="px-4 py-3 text-center font-bold text-primary-600">
                        {m.contributionPercent?.toFixed(1)}%
                      </td>
                      <td className="px-4 py-3 text-center">
                        {m.peerReviewScore != null
                          ? <span className="text-amber-600 font-medium">{m.peerReviewScore.toFixed(1)} /5</span>
                          : <span className="text-slate-300">Chưa có</span>}
                      </td>
                      <td className="px-4 py-3 text-center text-slate-500">{m.peerReviewCount || 0}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Submission history */}
          {reportData.members?.some(m => m.submissions?.length > 0) && (
            <div className="card overflow-hidden">
              <div className="px-6 py-4 border-b border-slate-100">
                <h2 className="text-base font-semibold text-slate-800">Lịch sử nộp bài</h2>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      {['Thành viên', 'Công việc', 'URL file', 'Thời gian nộp', 'Deadline', 'Đúng hạn?'].map(h => (
                        <th key={h} className="px-4 py-3 text-left font-semibold text-slate-600">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {reportData.members?.flatMap(m =>
                      (m.submissions || []).map((s, i) => (
                        <tr key={`${m.userId}-${i}`} className="hover:bg-slate-50">
                          <td className="px-4 py-3 font-medium text-slate-700">{m.name}</td>
                          <td className="px-4 py-3 text-slate-600">{s.taskName}</td>
                          <td className="px-4 py-3">
                            <a href={s.fileUrl} target="_blank" rel="noopener noreferrer"
                              className="text-primary-500 hover:underline text-xs truncate max-w-[200px] block">
                              {s.fileUrl}
                            </a>
                          </td>
                          <td className="px-4 py-3 text-slate-500 text-xs">{s.submittedAt}</td>
                          <td className="px-4 py-3 text-slate-500 text-xs">{s.deadline || '—'}</td>
                          <td className="px-4 py-3">
                            <span className={`badge ${s.onTime ? 'badge-done' : 'bg-red-100 text-red-700'}`}>
                              {s.onTime ? '✓ Đúng hạn' : '✗ Trễ'}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {!reportData && !loading && (
        <div className="text-center py-16 text-slate-400">
          <BarChart2 size={48} className="mx-auto mb-3 opacity-20" />
          <p>Chọn nhóm và nhấn "Tải báo cáo" để xem trước</p>
        </div>
      )}
    </div>
  )
}
