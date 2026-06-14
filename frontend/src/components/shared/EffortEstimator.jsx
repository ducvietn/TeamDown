import { useState } from 'react'
import { Clock, Zap, Edit2, Check } from 'lucide-react'

/**
 * EffortEstimator
 *
 * Allows members to estimate effort before starting a task:
 *  - Estimated hours (1–40h)
 *  - Difficulty level (1–5)
 *  - Rationale / notes
 *
 * Shows the estimated effort breakdown and a "Start" button.
 */
export default function EffortEstimator({ task, onEstimateChange }) {
  const [editing, setEditing] = useState(false)
  const [hours, setHours] = useState(task.estimatedHours || 8)
  const [difficulty, setDifficulty] = useState(task.difficulty || 3)
  const [notes, setNotes] = useState(task.estimateNotes || '')
  const [saved, setSaved] = useState(!!task.estimatedHours)

  const difficultyLabels = ['', 'Dễ', 'Trung bình', 'Khó', 'Rất khó', 'Cực kỳ khó']
  const difficultyColors = ['', 'text-emerald-600', 'text-blue-600', 'text-amber-600', 'text-orange-500', 'text-red-600']

  const handleSave = () => {
    const estimate = { estimatedHours: hours, difficulty, estimateNotes: notes }
    if (onEstimateChange) onEstimateChange(task.taskId, estimate)
    setSaved(true)
    setEditing(false)
  }

  const effortScore = hours * difficulty // simple weighted score

  if (!editing && !saved) {
    return (
      <button
        onClick={() => setEditing(true)}
        className="flex items-center gap-1.5 text-sm text-slate-400 hover:text-primary-600 transition-colors"
      >
        <Clock size={13} />
        Ước lượng công sức
      </button>
    )
  }

  if (!editing && saved) {
    return (
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-1 text-sm text-slate-600">
          <Clock size={13} className="text-slate-400" />
          <span>{hours}h</span>
        </div>
        <div className="flex items-center gap-1 text-sm text-slate-600">
          <Zap size={13} className="text-slate-400" />
          <span className={difficultyColors[difficulty]}>{difficultyLabels[difficulty]}</span>
        </div>
        <button
          onClick={() => setEditing(true)}
          className="text-xs text-slate-400 hover:text-primary-600 flex items-center gap-1 transition-colors"
        >
          <Edit2 size={11} /> Sửa
        </button>
      </div>
    )
  }

  return (
    <div className="bg-slate-50 border border-slate-200 rounded-lg p-3 space-y-3">
      <p className="text-xs font-semibold text-slate-600 uppercase tracking-wide">Ước lượng công sức</p>

      {/* Hours */}
      <div className="flex items-center gap-3">
        <label className="text-sm text-slate-600 w-20 flex items-center gap-1.5">
          <Clock size={13} /> Giờ ước lượng
        </label>
        <input
          type="number"
          min={1}
          max={120}
          value={hours}
          onChange={e => setHours(Math.max(1, Number(e.target.value)))}
          className="w-20 text-sm border border-slate-300 rounded-lg px-2 py-1 text-center focus:outline-none focus:ring-2 focus:ring-primary-400"
        />
        <span className="text-sm text-slate-400">giờ</span>
        <div className="flex gap-1">
          {[4, 8, 16, 40].map(h => (
            <button
              key={h}
              onClick={() => setHours(h)}
              className={`text-xs px-2 py-0.5 rounded transition-colors ${
                hours === h ? 'bg-primary-100 text-primary-700' : 'bg-white border border-slate-200 text-slate-500 hover:border-primary-300'
              }`}
            >
              {h}h
            </button>
          ))}
        </div>
      </div>

      {/* Difficulty */}
      <div className="flex items-center gap-3">
        <label className="text-sm text-slate-600 w-20 flex items-center gap-1.5">
          <Zap size={13} /> Độ khó
        </label>
        <div className="flex gap-1">
          {[1, 2, 3, 4, 5].map(level => (
            <button
              key={level}
              onClick={() => setDifficulty(level)}
              className={`w-8 h-8 rounded-lg text-sm font-medium transition-all ${
                difficulty === level
                  ? `${difficultyColors[level]} bg-current/10 ring-2 ring-current`
                  : 'bg-white border border-slate-200 text-slate-400 hover:border-slate-300'
              }`}
              title={difficultyLabels[level]}
            >
              {level}
            </button>
          ))}
        </div>
        <span className={`text-sm font-medium ${difficultyColors[difficulty]}`}>
          {difficultyLabels[difficulty]}
        </span>
      </div>

      {/* Notes */}
      <div>
        <textarea
          value={notes}
          onChange={e => setNotes(e.target.value)}
          placeholder="Ghi chú về công việc..."
          rows={2}
          className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary-400 resize-none"
        />
      </div>

      {/* Effort score indicator */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-500">Điểm công sức:</span>
          <span className={`text-sm font-bold ${
            effortScore < 20 ? 'text-emerald-600' : effortScore < 50 ? 'text-blue-600' : effortScore < 100 ? 'text-amber-600' : 'text-red-600'
          }`}>
            {effortScore}
          </span>
          {effortScore < 20 && <span className="text-xs text-emerald-600">(Nhẹ)</span>}
          {effortScore >= 20 && effortScore < 50 && <span className="text-xs text-blue-600">(Trung bình)</span>}
          {effortScore >= 50 && effortScore < 100 && <span className="text-xs text-amber-600">(Nặng)</span>}
          {effortScore >= 100 && <span className="text-xs text-red-600">(Rất nặng)</span>}
        </div>
        <div className="flex gap-2">
          <button onClick={() => setEditing(false)} className="btn-outline text-xs px-3 py-1.5">
            Hủy
          </button>
          <button onClick={handleSave} className="btn-primary text-xs px-3 py-1.5 flex items-center gap-1">
            <Check size={12} /> Lưu
          </button>
        </div>
      </div>
    </div>
  )
}
