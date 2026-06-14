import ProgressSlider from '../shared/ProgressSlider.jsx'
import EffortEstimator from '../shared/EffortEstimator.jsx'
import { Clock, Calendar, CheckCircle, AlertTriangle, FileText, User } from 'lucide-react'

const STATUS_CONFIG = {
  TODO:           { label: 'Việc mới',     badge: 'badge-todo',      color: 'text-slate-500' },
  IN_PROGRESS:    { label: 'Đang làm',    badge: 'badge-progress',  color: 'text-blue-600' },
  PENDING_REVIEW: { label: 'Chờ duyệt',   badge: 'badge-pending',   color: 'text-amber-600' },
  DONE:           { label: 'Hoàn thành',  badge: 'badge-done',      color: 'text-emerald-600' },
}

export default function TaskCard({ task, isLeader, onProgressChange, onSubmit, onViewDetails }) {
  const cfg = STATUS_CONFIG[task.status] || STATUS_CONFIG.TODO
  const isMine = task.assignedTo?.userId !== undefined // simplified check
  const hasDeadline = !!task.deadline
  const isOverdue = hasDeadline && new Date(task.deadline) < new Date() && task.status !== 'DONE'

  return (
    <div className="card flex flex-col overflow-hidden hover:shadow-md transition-shadow">
      {/* Status strip */}
      <div className={`h-1 w-full ${
        task.status === 'DONE' ? 'bg-emerald-500' :
        task.status === 'PENDING_REVIEW' ? 'bg-amber-400' :
        task.status === 'IN_PROGRESS' ? 'bg-blue-500' : 'bg-slate-200'
      }`} />

      <div className="p-4 flex flex-col gap-3 flex-1">
        {/* Header */}
        <div className="flex items-start justify-between gap-2">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <span className={`badge ${cfg.badge}`}>{cfg.label}</span>
              {isOverdue && (
                <span className="badge bg-red-100 text-red-700 flex items-center gap-1">
                  <AlertTriangle size={10} /> Trễ deadline
                </span>
              )}
            </div>
            <h3 className="font-semibold text-slate-800 mt-1 leading-snug">{task.taskName}</h3>
          </div>
        </div>

        {/* Meta */}
        <div className="flex flex-wrap gap-3 text-xs text-slate-500">
          {task.assignedTo && (
            <div className="flex items-center gap-1">
              <User size={11} />
              <span>{task.assignedTo.name}</span>
            </div>
          )}
          {hasDeadline && (
            <div className={`flex items-center gap-1 ${isOverdue ? 'text-red-500' : ''}`}>
              <Calendar size={11} />
              <span>{new Date(task.deadline).toLocaleDateString('vi-VN')}</span>
            </div>
          )}
        </div>

        {/* Description */}
        {task.description && (
          <p className="text-sm text-slate-500 line-clamp-2">{task.description}</p>
        )}

        {/* Effort estimator */}
        {task.status !== 'DONE' && (
          <EffortEstimator task={task} />
        )}

        {/* Progress slider — only for members who own this task */}
        {task.status !== 'DONE' && task.status !== 'PENDING_REVIEW' && (
          <ProgressSlider
            task={task}
            onProgressChange={onProgressChange}
            onSubmitRequest={onSubmit}
          />
        )}

        {/* Pending review notice */}
        {task.status === 'PENDING_REVIEW' && (
          <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm">
            <p className="text-amber-700 font-medium flex items-center gap-1.5">
              <FileText size={14} />
              Đang chờ trưởng nhóm duyệt
            </p>
            <p className="text-amber-600 text-xs mt-1">
              Trưởng nhóm đang xem xét bài nộp của bạn.
            </p>
          </div>
        )}

        {/* Done notice */}
        {task.status === 'DONE' && (
          <div className="flex items-center gap-2 text-emerald-600 text-sm font-medium">
            <CheckCircle size={16} />
            Đã hoàn thành & được phê duyệt
          </div>
        )}

        {/* Footer — leader sees all */}
        <div className="flex items-center justify-between mt-auto pt-2 border-t border-slate-100">
          {task.lastProgressUpdate && (
            <span className="text-xs text-slate-400 flex items-center gap-1">
              <Clock size={10} />
              {new Date(task.lastProgressUpdate).toLocaleDateString('vi-VN')}
            </span>
          )}
          <button
            onClick={onViewDetails}
            className="text-xs text-primary-500 hover:text-primary-700 font-medium ml-auto"
          >
            Chi tiết →
          </button>
        </div>
      </div>
    </div>
  )
}
