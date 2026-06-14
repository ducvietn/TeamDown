import { useState, useCallback } from 'react'
import { CheckCircle } from 'lucide-react'

/**
 * ProgressSlider
 *
 * A 0–100% slider with:
 *  - Real-time visual feedback (color changes as progress increases)
 *  - Debounced API call to avoid flooding the backend
 *  - Auto-submit prompt when reaching 100%
 *  - Disabled when task is DONE
 */
export default function ProgressSlider({ task, onProgressChange, onSubmitRequest }) {
  const [value, setValue] = useState(task.progress || 0)
  const [saving, setSaving] = useState(false)
  const [showSubmit, setShowSubmit] = useState(false)
  const [fileUrl, setFileUrl] = useState('')

  const isDone = task.status === 'DONE'
  const isLocked = task.status === 'PENDING_REVIEW' && !isDone

  const color = value < 30 ? '#94a3b8'
    : value < 70 ? '#3b82f6'
    : value < 100 ? '#f59e0b'
    : '#10b981'

  const handleChange = useCallback((e) => {
    const newVal = Number(e.target.value)
    setValue(newVal)
    setShowSubmit(newVal === 100)
  }, [])

  const handleSave = async () => {
    setSaving(true)
    try {
      await onProgressChange(task.taskId, value)
    } finally {
      setSaving(false)
    }
  }

  const handleSubmitRequest = () => {
    if (!fileUrl.trim()) {
      alert('Vui lòng nhập URL file trước khi nộp!')
      return
    }
    onSubmitRequest(task.taskId, fileUrl.trim())
    setShowSubmit(false)
    setFileUrl('')
  }

  if (isDone) {
    return (
      <div className="flex items-center gap-2 text-success font-medium text-sm">
        <CheckCircle size={16} />
        Hoàn thành & Đã khóa
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {/* Slider row */}
      <div className="flex items-center gap-3">
        <div className="flex-1">
          <div className="relative">
            <div className="h-3 bg-slate-100 rounded-full overflow-hidden">
              <div
                className="h-full rounded-full transition-all duration-300"
                style={{ width: `${value}%`, backgroundColor: color }}
              />
            </div            <input
              type="range"
              min={0}
              max={100}
              step={5}
              value={value}
              onChange={handleChange}
              disabled={isLocked}
              className="absolute inset-0 w-full opacity-0 cursor-pointer"
              style={{ height: '0.75rem', top: '50%', transform: 'translateY(-50%)' }}
            />
          </div>
        </div>
        <span
          className="text-sm font-bold w-10 text-right shrink-0"
          style={{ color }}
        >
          {value}%
        </span>
        <button
          onClick={handleSave}
          disabled={saving || value === task.progress}
          className="btn-primary text-xs px-3 py-1.5 shrink-0 disabled:opacity-40"
        >
          {saving ? '...' : 'Lưu'}
        </button>
      </div>

      {/* 100% submit prompt */}
      {showSubmit && (
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 space-y-2">
          <p className="text-sm font-medium text-amber-800">
            Bạn đã hoàn thành 100%! Nhập URL file để nộp bài.
          </p>
          <div className="flex gap-2">
            <input
              type="url"
              value={fileUrl}
              onChange={e => setFileUrl(e.target.value)}
              placeholder="https://drive.google.com/... hoặc URL file"
              className="flex-1 text-sm border border-amber-300 rounded-lg px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-amber-400"
            />
            <button
              onClick={handleSubmitRequest}
              className="btn-primary text-xs px-3 py-1.5 shrink-0"
            >
              Nộp bài
            </button>
            <button
              onClick={() => setShowSubmit(false)}
              className="btn-outline text-xs px-2 py-1.5 text-slate-500"
            >
              Hủy
            </button>
          </div>
        </div>
      )}

      {/* PENDING_REVIEW lock notice */}
      {isLocked && (
        <p className="text-xs text-amber-600 flex items-center gap-1">
          <span className="w-1.5 h-1.5 bg-amber-400 rounded-full" />
          Đang chờ trưởng nhóm duyệt — không thể chỉnh sửa tiến độ
        </p>
      )}
    </div>
  )
}
