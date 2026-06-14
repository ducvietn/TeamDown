import { useState } from 'react'
import { useApp } from '../context/AppContext.jsx'
import { taskApi } from '../api/index.js'
import ProgressSlider from '../components/shared/ProgressSlider.jsx'
import EffortEstimator from '../components/shared/EffortEstimator.jsx'
import TaskModal from '../components/tasks/TaskModal.jsx'
import TaskCard from '../components/tasks/TaskCard.jsx'
import { Plus, RefreshCw } from 'lucide-react'

export default function TaskWorkspace() {
  const { currentUser, currentGroup } = useApp()
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [activeTask, setActiveTask] = useState(null)
  const [filter, setFilter] = useState('ALL')

  const loadTasks = async () => {
    if (!currentGroup?.groupId) return
    setLoading(true)
    setError('')
    try {
      const data = await taskApi.getByGroup(currentGroup.groupId)
      setTasks(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  // Load my assigned tasks
  const loadMyTasks = async () => {
    if (!currentUser?.userId) return
    setLoading(true)
    setError('')
    try {
      const data = await taskApi.getByAssignee(currentUser.userId)
      setTasks(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const handleProgressChange = async (taskId, newProgress) => {
    try {
      const updated = await taskApi.updateProgress(taskId, newProgress)
      setTasks(prev => prev.map(t => t.taskId === taskId ? updated : t))
    } catch (e) {
      alert(e.message)
    }
  }

  const handleSubmitTask = async (taskId, fileUrl) => {
    try {
      const updated = await taskApi.submit(taskId, fileUrl)
      setTasks(prev => prev.map(t => t.taskId === taskId ? updated : t))
      alert('Đã nộp bài! Trưởng nhóm sẽ duyệt.')
    } catch (e) {
      alert(e.message)
    }
  }

  const filtered = tasks.filter(t => {
    if (filter === 'ALL') return true
    return t.status === filter
  })

  const statusCounts = {
    ALL: tasks.length,
    TODO: tasks.filter(t => t.status === 'TODO').length,
    IN_PROGRESS: tasks.filter(t => t.status === 'IN_PROGRESS').length,
    PENDING_REVIEW: tasks.filter(t => t.status === 'PENDING_REVIEW').length,
    DONE: tasks.filter(t => t.status === 'DONE').length,
  }

  return (
    <div className="p-6 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Công việc</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            {currentGroup?.groupName || 'Chưa chọn nhóm'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={loadTasks} className="btn-outline text-sm flex items-center gap-1.5">
            <RefreshCw size={14} /> Tải lại
          </button>
          <button onClick={() => setShowCreate(true)} className="btn-primary text-sm flex items-center gap-1.5">
            <Plus size={14} /> Tạo công việc
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="flex gap-2 mb-6 flex-wrap">
        {['ALL', 'TODO', 'IN_PROGRESS', 'PENDING_REVIEW', 'DONE'].map(s => (
          <button
            key={s}
            onClick={() => setFilter(s)}
            className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
              filter === s
                ? 'bg-primary-600 text-white'
                : 'bg-white border border-slate-200 text-slate-600 hover:border-primary-300'
            }`}
          >
            {s === 'ALL' ? 'Tất cả' :
             s === 'IN_PROGRESS' ? 'Đang làm' :
             s === 'PENDING_REVIEW' ? 'Chờ duyệt' :
             s === 'TODO' ? 'Việc mới' : 'Hoàn thành'}
            <span className={`ml-1.5 px-1.5 py-0.5 rounded-full text-[11px] ${
              filter === s ? 'bg-primary-700' : 'bg-slate-100'
            }`}>
              {statusCounts[s]}
            </span>
          </button>
        ))}
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4 text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Task grid */}
      {loading ? (
        <div className="text-center py-16 text-slate-400">Đang tải công việc...</div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 text-slate-400">
          <p className="text-lg">Không có công việc nào</p>
          <button onClick={loadMyTasks} className="mt-3 btn-outline text-sm">
            Tải công việc của tôi
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map(task => (
            <TaskCard
              key={task.taskId}
              task={task}
              isLeader={currentGroup?.leader?.userId === currentUser?.userId}
              onProgressChange={handleProgressChange}
              onSubmit={handleSubmitTask}
              onViewDetails={() => setActiveTask(task)}
            />
          ))}
        </div>
      )}

      {/* Modals */}
      {showCreate && (
        <TaskModal
          groupId={currentGroup?.groupId}
          members={currentGroup?.members || []}
          onClose={() => setShowCreate(false)}
          onCreated={(newTask) => {
            setTasks(prev => [newTask, ...prev])
            setShowCreate(false)
          }}
        />
      )}

      {activeTask && (
        <TaskModal
          task={activeTask}
          groupId={currentGroup?.groupId}
          members={currentGroup?.members || []}
          onClose={() => setActiveTask(null)}
          onUpdated={(updated) => {
            setTasks(prev => prev.map(t => t.taskId === updated.taskId ? updated : t))
            setActiveTask(null)
          }}
        />
      )}
    </div>
  )
}
