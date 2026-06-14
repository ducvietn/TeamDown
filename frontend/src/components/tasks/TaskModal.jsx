import { useState } from 'react'
import { taskApi } from '../../api/index.js'
import { X, Calendar, User, AlignLeft, Clock } from 'lucide-react'

export default function TaskModal({ task, groupId, members, onClose, onCreated, onUpdated }) {
  const isEdit = !!task
  const [form, setForm] = useState({
    taskName:     task?.taskName || '',
    description:  task?.description || '',
    deadline:     task?.deadline ? task.deadline.slice(0, 16) : '',
    assignedToId: task?.assignedTo?.userId || '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const set = (key) => (e) => setForm(f => ({ ...f, [key]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.taskName.trim()) {
      setError('Tên công việc là bắt buộc.')
      return
    }
    setLoading(true)
    setError('')
    try {
      if (isEdit) {
        const updated = await taskApi.update(task.taskId, form)
        if (onUpdated) onUpdated(updated)
      } else {
        const payload = {
          ...form,
          groupId: Number(groupId),
          assignedToId: form.assignedToId ? Number(form.assignedToId) : null,
        }
        const created = await taskApi.create(payload)
        if (onCreated) onCreated(created)
      }
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <h2 className="text-lg font-bold text-slate-800">
            {isEdit ? 'Chỉnh sửa công việc' : 'Tạo công việc mới'}
          </h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition-colors">
            <X size={20} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Task name */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Tên công việc <span className="text-danger">*</span>
            </label>
            <input
              type="text"
              value={form.taskName}
              onChange={set('taskName')}
              placeholder="VD: Thiết kế giao diện trang chủ"
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
              maxLength={200}
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1 flex items-center gap-1">
              <AlignLeft size={13} /> Mô tả
            </label>
            <textarea
              value={form.description}
              onChange={set('description')}
              placeholder="Mô tả chi tiết công việc..."
              rows={3}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400 resize-none"
            />
          </div>

          {/* Assigned to */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1 flex items-center gap-1">
              <User size={13} /> Giao cho
            </label>
            <select
              value={form.assignedToId}
              onChange={set('assignedToId')}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400 bg-white"
            >
              <option value="">— Chưa giao —</option>
              {members.map(m => (
                <option key={m.userId} value={m.userId}>{m.name}</option>
              ))}
            </select>
          </div>

          {/* Deadline */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1 flex items-center gap-1">
              <Calendar size={13} /> Hạn nộp
            </label>
            <input
              type="datetime-local"
              value={form.deadline}
              onChange={set('deadline')}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
            />
          </div>

          {/* Deadline info */}
          {task?.deadline && (
            <div className="flex items-center gap-1.5 text-xs text-slate-500 bg-slate-50 rounded-lg px-3 py-2">
              <Clock size={12} />
              Deadline: {new Date(task.deadline).toLocaleString('vi-VN')}
            </div>
          )}

          {/* Error */}
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">
              {error}
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-2 pt-2">
            <button type="button" onClick={onClose} className="btn-outline flex-1">
              Hủy
            </button>
            <button type="submit" disabled={loading} className="btn-primary flex-1">
              {loading ? 'Đang lưu...' : isEdit ? 'Lưu thay đổi' : 'Tạo công việc'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
