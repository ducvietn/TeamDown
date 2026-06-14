import { useState, useEffect } from 'react'
import { useApp } from '../../context/AppContext.jsx'
import { taskApi } from '../../api/index.js'
import { CheckCircle, XCircle, Eye, RefreshCw, Clock, AlertTriangle, FileText } from 'lucide-react'

export default function LeaderReviewPage() {
  const { currentGroup, currentUser } = useApp()
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [processingId, setProcessingId] = useState(null)
  const [selectedTask, setSelectedTask] = useState(null)
  const [submissions, setSubmissions] = useState([])
  const [submitLoading, setSubmitLoading] = useState(false)

  const loadPendingTasks = async () => {
    if (!currentGroup?.groupId) return
    setLoading(true)
    setError('')
    try {
      const all = await taskApi.getByGroup(currentGroup.groupId)
      setTasks(Array.isArray(all) ? all : [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadPendingTasks() }, [currentGroup])

  const loadSubmissions = async (taskId) => {
    setSubmitLoading(true)
    try {
      const data = await taskApi.getSubmissions(taskId)
      setSubmissions(Array.isArray(data) ? data : [])
    } catch (e) {
      setSubmissions([])
    } finally {
      setSubmitLoading(false)
    }
  }

  const handleApprove = async (task) => {
    if (!confirm(`Phê duyệt công việc "${task.taskName}"?\nTrạng thái sẽ chuyển thành HOÀN THÀNH và bị khóa.`)) return
    setProcessingId(task.taskId)
    try {
      const updated = await taskApi.approve(task.taskId, currentUser.userId)
      setTasks(prev => prev.map(t => t.taskId === task.taskId ? updated : t))
    } catch (e) {
      alert('Lỗi: ' + e.message)
    } finally {
      setProcessingId(null)
    }
  }

  const handleReject = async (task) => {
    if (!confirm(`Từ chối công việc "${task.taskName}"?\nTrạng thái sẽ quay về ĐANG LÀM.`)) return
    setProcessingId(task.taskId)
    try {
      const updated = await taskApi.reject(task.taskId, currentUser.userId)
      setTasks(prev => prev.map(t => t.taskId === task.taskId ? updated : t))
    } catch (e) {
      alert('Lỗi: ' + e.message)
    } finally {
      setProcessingId(null)
    }
  }

  const pendingTasks = tasks.filter(t => t.status === 'PENDING_REVIEW')
  const inProgressTasks = tasks.filter(t => t.status === 'IN_PROGRESS')
  const doneTasks = tasks.filter(t => t.status === 'DONE')

  const openTaskModal = async (task) => {
    setSelectedTask(task)
    await loadSubmissions(task.taskId)
  }

  const statusBadge = (status) => {
    const map = {
      PENDING_REVIEW: 'badge-pending',
      IN_PROGRESS:    'badge-progress',
      DONE:           'badge-done',
      TODO:           'badge-todo',
    }
    const labels = {
      PENDING_REVIEW: 'Chờ duyệt',
      IN_PROGRESS:    'Đang làm',
      DONE:           'Hoàn thành',
      TODO:           'Việc mới',
    }
    return <span className={`badge ${map[status] || 'badge-todo'}`}>{labels[status] || status}</span>
  }

  const TaskRow = ({ task }) => {
    const isProcessing = processingId === task.taskId
    return (
      <div className="flex items-center gap-3 p-4 bg-white rounded-xl border border-slate-200 hover:border-primary-200 transition-all">
        {/* Progress ring */}
        <div className="relative w-12 h-12 shrink-0">
          <svg className="w-12 h-12 -rotate-90">
            <circle cx="24" cy="24" r="18" fill="none" stroke="#e2e8f0" strokeWidth="4" />
            <circle
              cx="24" cy="24" r="18" fill="none"
              stroke={task.status === 'DONE' ? '#10b981' : '#3b82f6'}
              strokeWidth="4"
              strokeLinecap="round"
              strokeDasharray={`${2 * Math.PI * 18}`}
              strokeDashoffset={`${2 * Math.PI * 18 * (1 - (task.progress || 0) / 100)}`}
            />
          </svg>
          <span className="absolute inset-0 flex items-center justify-center text-[10px] font-bold text-slate-700">
            {task.progress}%
          </span>
        </div>

        {/* Task info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <p className="font-medium text-slate-800 truncate">{task.taskName}</p>
            {statusBadge(task.status)}
          </div>
          {task.assignedTo && (
            <p className="text-xs text-slate-500 mt-0.5">
              Giao cho: <strong>{task.assignedTo.name}</strong>
            </p>
          )}
          {task.lastProgressUpdate && (
            <p className="text-xs text-slate-400 mt-0.5 flex items-center gap-1">
              <Clock size={10} />
              Cập nhật: {new Date(task.lastProgressUpdate).toLocaleString('vi-VN')}
            </p>
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={() => openTaskModal(task)}
            className="btn-outline text-xs px-3 py-1.5 flex items-center gap-1"
          >
            <Eye size={13} /> Xem file
          </button>

          {task.status === 'PENDING_REVIEW' && (
            <>
              <button
                onClick={() => handleApprove(task)}
                disabled={isProcessing}
                className="btn-success text-xs px-3 py-1.5 flex items-center gap-1"
              >
                {isProcessing ? <RefreshCw size={12} className="animate-spin" /> : <CheckCircle size={13} />}
                Duyệt
              </button>
              <button
                onClick={() => handleReject(task)}
                disabled={isProcessing}
                className="btn-danger text-xs px-3 py-1.5 flex items-center gap-1"
              >
                {isProcessing ? <RefreshCw size={12} className="animate-spin" /> : <XCircle size={13} />}
                Từ chối
              </button>
            </>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="p-6 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Duyệt bài nộp</h1>
          <p className="text-sm text-slate-500">
            Xem file, phê duyệt hoặc từ chối công việc của thành viên
          </p>
        </div>
        <button onClick={loadPendingTasks} className="btn-outline text-sm flex items-center gap-1.5">
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          Làm mới
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4 text-sm text-red-700">{error}</div>
      )}

      {/* Summary cards */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Chờ duyệt', count: pendingTasks.length, color: 'text-amber-600 bg-amber-50', icon: AlertTriangle },
          { label: 'Đang làm', count: inProgressTasks.length, color: 'text-blue-600 bg-blue-50', icon: Clock },
          { label: 'Hoàn thành', count: doneTasks.length, color: 'text-emerald-600 bg-emerald-50', icon: CheckCircle },
        ].map(({ label, count, color, icon: Icon }) => (
          <div key={label} className="card p-4 flex items-center gap-3">
            <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${color}`}>
              <Icon size={20} />
            </div>
            <div>
              <p className="text-2xl font-bold text-slate-800">{count}</p>
              <p className="text-xs text-slate-500">{label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Pending review tasks */}
      {pendingTasks.length > 0 && (
        <div className="mb-8">
          <h2 className="text-sm font-semibold text-amber-600 uppercase tracking-wider mb-3">
            Cần duyệt ({pendingTasks.length})
          </h2>
          <div className="space-y-3">
            {pendingTasks.map(task => <TaskRow key={task.taskId} task={task} />)}
          </div>
        </div>
      )}

      {/* In-progress tasks */}
      {inProgressTasks.length > 0 && (
        <div className="mb-8">
          <h2 className="text-sm font-semibold text-blue-600 uppercase tracking-wider mb-3">
            Đang làm ({inProgressTasks.length})
          </h2>
          <div className="space-y-3">
            {inProgressTasks.map(task => <TaskRow key={task.taskId} task={task} />)}
          </div>
        </div>
      )}

      {/* Done tasks */}
      {doneTasks.length > 0 && (
        <div className="mb-8">
          <h2 className="text-sm font-semibold text-emerald-600 uppercase tracking-wider mb-3">
            Hoàn thành ({doneTasks.length})
          </h2>
          <div className="space-y-3">
            {doneTasks.map(task => <TaskRow key={task.taskId} task={task} />)}
          </div>
        </div>
      )}

      {tasks.length === 0 && !loading && (
        <div className="text-center py-16 text-slate-400">
          <FileText size={40} className="mx-auto mb-3 opacity-30" />
          <p>Chưa có công việc nào</p>
        </div>
      )}

      {/* File preview modal */}
      {selectedTask && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[80vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100">
              <h2 className="text-lg font-bold text-slate-800">{selectedTask.taskName}</h2>
              {selectedTask.assignedTo && (
                <p className="text-sm text-slate-500 mt-1">
                  của <strong>{selectedTask.assignedTo.name}</strong>
                </p>
              )}
            </div>
            <div className="p-6">
              {submitLoading ? (
                <p className="text-slate-400 text-sm">Đang tải file...</p>
              ) : submissions.length === 0 ? (
                <p className="text-slate-400 text-sm">Không có file được nộp.</p>
              ) : (
                <div className="space-y-3">
                  {submissions.map(sub => (
                    <div key={sub.submissionId} className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
                      <FileText size={20} className="text-primary-500 shrink-0" />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-slate-700 truncate">{sub.fileUrl}</p>
                        <p className="text-xs text-slate-400">
                          {new Date(sub.submittedAt).toLocaleString('vi-VN')}
                        </p>
                      </div>
                      <a
                        href={sub.fileUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="btn-outline text-xs px-2.5 py-1 shrink-0"
                      >
                        Mở
                      </a>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="p-4 border-t border-slate-100 flex justify-end">
              <button onClick={() => setSelectedTask(null)} className="btn-outline text-sm">
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
