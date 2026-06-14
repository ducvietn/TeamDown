import { useState, useEffect } from 'react'
import { useApp } from '../context/AppContext.jsx'
import { peerReviewApi, groupApi } from '../api/index.js'
import { Star, Send, RefreshCw, Shield } from 'lucide-react'

const STAR_LABELS = ['', 'Rất kém', 'Kém', 'Bình thường', 'Tốt', 'Xuất sắc']

export default function PeerReviewPage() {
  const { currentUser, currentGroup } = useApp()
  const [mode, setMode] = useState('received') // received | give | given
  const [reviews, setReviews] = useState([])
  const [members, setMembers] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // Review form state
  const [selectedReviewee, setSelectedReviewee] = useState('')
  const [score, setScore] = useState(0)
  const [comment, setComment] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitted, setSubmitted] = useState(false)

  useEffect(() => { loadReviews() }, [mode])
  useEffect(() => { loadMembers() }, [])

  const loadReviews = async () => {
    setLoading(true)
    setError('')
    try {
      let data
      if (mode === 'received') {
        data = await peerReviewApi.getReceived(currentUser.userId)
      } else if (mode === 'given') {
        data = await peerReviewApi.getGiven(currentUser.userId)
      }
      setReviews(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const loadMembers = async () => {
    try {
      const groups = await groupApi.getAll()
      const g = groups?.find(g => g.groupId === currentGroup?.groupId)
      const allMembers = g?.members || currentGroup?.members || []
      const others = allMembers.filter(m => m.userId !== currentUser?.userId)
      setMembers(others)
    } catch (_) {}
  }

  const handleSubmitReview = async (e) => {
    e.preventDefault()
    if (!selectedReviewee) { alert('Vui lòng chọn người cần đánh giá.'); return }
    if (score === 0) { alert('Vui lòng chọn điểm đánh giá.'); return }
    if (!comment.trim()) { alert('Vui lòng nhập nhận xét.'); return }

    setSubmitting(true)
    try {
      await peerReviewApi.submit(currentUser.userId, {
        groupId:      currentGroup.groupId,
        revieweeId:   Number(selectedReviewee),
        score,
        comment:      comment.trim(),
      })
      setSubmitted(true)
      setScore(0); setComment(''); setSelectedReviewee('')
      setTimeout(() => setSubmitted(false), 4000)
    } catch (e) {
      alert(e.message)
    } finally {
      setSubmitting(false)
    }
  }

  const ScoreStars = ({ value, onChange, interactive }) => (
    <div className="flex gap-1">
      {[1, 2, 3, 4, 5].map(s => (
        <button
          key={s}
          type="button"
          onClick={() => interactive && onChange(s)}
          disabled={!interactive}
          className={`text-2xl transition-all ${interactive ? 'cursor-pointer hover:scale-110' : 'cursor-default'} ${
            s <= value ? 'text-amber-400' : 'text-slate-200'
          }`}
        >
          ★
        </button>
      ))}
    </div>
  )

  return (
    <div className="p-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Đánh giá đồng nghiệp</h1>
        <p className="text-sm text-slate-500 mt-0.5 flex items-center gap-1">
          <Shield size={13} className="text-success" />
          Đánh giá ẩn danh — người được đánh giá không biết ai đã đánh giá họ
        </p>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        {[
          { key: 'received', label: 'Nhận được', icon: '↓' },
          { key: 'give',      label: 'Viết đánh giá', icon: '↑' },
          { key: 'given',     label: 'Đã gửi', icon: '✓' },
        ].map(tab => (
          <button
            key={tab.key}
            onClick={() => setMode(tab.key)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              mode === tab.key
                ? 'bg-primary-600 text-white'
                : 'bg-white border border-slate-200 text-slate-600 hover:border-primary-300'
            }`}
          >
            <span className="mr-1.5">{tab.icon}</span>
            {tab.label}
          </button>
        ))}
      </div>

      {/* ── Tab: Reviews I received ── */}
      {mode === 'received' && (
        <div className="space-y-4">
          {loading ? (
            <p className="text-slate-400 text-center py-8">Đang tải...</p>
          ) : reviews.length === 0 ? (
            <div className="card p-8 text-center">
              <p className="text-slate-400">Bạn chưa nhận được đánh giá nào.</p>
              <p className="text-slate-400 text-sm mt-1">Đánh giá sẽ hiển thị sau khi kỳ học kết thúc.</p>
            </div>
          ) : (
            reviews.map(review => (
              <div key={review.reviewId} className="card p-5">
                <div className="flex items-start justify-between mb-3">
                  <ScoreStars value={review.score} onChange={() => {}} interactive={false} />
                  <span className="text-xs text-slate-400">
                    {review.createdAt && new Date(review.createdAt).toLocaleString('vi-VN')}
                  </span>
                </div>
                <p className="text-slate-700 leading-relaxed">{review.comment}</p>
                <p className="text-xs text-slate-400 mt-3">Nhóm: {review.group?.groupName}</p>
              </div>
            ))
          )}
        </div>
      )}

      {/* ── Tab: Write a review ── */}
      {mode === 'give' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Form */}
          <div className="card p-6">
            <h3 className="font-semibold text-slate-800 mb-4">Viết đánh giá mới</h3>
            {submitted && (
              <div className="bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-lg p-3 text-sm mb-4">
                ✓ Đánh giá đã được gửi thành công!
              </div>
            )}
            <form onSubmit={handleSubmitReview} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Người được đánh giá</label>
                <select
                  value={selectedReviewee}
                  onChange={e => setSelectedReviewee(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400 bg-white"
                >
                  <option value="">— Chọn thành viên —</option>
                  {members.map(m => (
                    <option key={m.userId} value={m.userId}>{m.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Điểm đánh giá (1–5)</label>
                <ScoreStars value={score} onChange={setScore} interactive />
                {score > 0 && (
                  <p className="text-xs text-amber-600 mt-1">{STAR_LABELS[score]} — {score}/5 sao</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Nhận xét</label>
                <textarea
                  value={comment}
                  onChange={e => setComment(e.target.value)}
                  placeholder="Viết nhận xét về đồng nghiệp..."
                  rows={4}
                  maxLength={1000}
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400 resize-none"
                />
                <p className="text-xs text-slate-400 text-right mt-1">{comment.length}/1000</p>
              </div>

              <button
                type="submit"
                disabled={submitting}
                className="btn-primary w-full flex items-center justify-center gap-2"
              >
                {submitting ? <RefreshCw size={14} className="animate-spin" /> : <Send size={14} />}
                {submitting ? 'Đang gửi...' : 'Gửi đánh giá'}
              </button>
            </form>
          </div>

          {/* Tips */}
          <div className="card p-6 bg-slate-50">
            <h3 className="font-semibold text-slate-700 mb-3 flex items-center gap-2">
              <Shield size={16} className="text-success" />
              Cam kết đánh giá ẩn danh
            </h3>
            <ul className="space-y-2 text-sm text-slate-600">
              {[
                'Người được đánh giá sẽ KHÔNG biết ai đã đánh giá họ.',
                'Điểm đánh giá (1–5 sao) phản ánh thái độ hợp tác, không phải năng lực chuyên môn.',
                'Nhận xét phải mang tính xây dựng và tôn trọng.',
                'Đánh giá sẽ chỉ hiển thị cho giáo viên sau khi kỳ học kết thúc.',
              ].map((tip, i) => (
                <li key={i} className="flex items-start gap-2">
                  <span className="text-success mt-0.5">✓</span>
                  {tip}
                </li>
              ))}
            </ul>
            <div className="mt-4 p-3 bg-white rounded-lg border border-slate-200">
              <p className="text-xs text-slate-500">
                <strong>Tiêu chí đánh giá:</strong><br />
                5★ — Hợp tác xuất sắc, luôn hỗ trợ team<br />
                4★ — Hợp tác tốt<br />
                3★ — Hợp tác bình thường<br />
                2★ — Cần cải thiện<br />
                1★ — Không hợp tác
              </p>
            </div>
          </div>
        </div>
      )}

      {/* ── Tab: Reviews I gave ── */}
      {mode === 'given' && (
        <div className="space-y-4">
          {loading ? (
            <p className="text-slate-400 text-center py-8">Đang tải...</p>
          ) : reviews.length === 0 ? (
            <div className="card p-8 text-center">
              <p className="text-slate-400">Bạn chưa gửi đánh giá nào.</p>
              <button onClick={() => setMode('give')} className="btn-primary mt-3 text-sm">
                Viết đánh giá đầu tiên
              </button>
            </div>
          ) : (
            reviews.map(review => (
              <div key={review.reviewId} className="card p-5">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <p className="font-medium text-slate-700">Đánh giá: <strong>{review.reviewee?.name}</strong></p>
                    <p className="text-xs text-slate-400">Nhóm: {review.group?.groupName}</p>
                  </div>
                  <ScoreStars value={review.score} onChange={() => {}} interactive={false} />
                </div>
                <p className="text-slate-700 leading-relaxed">{review.comment}</p>
                <p className="text-xs text-slate-400 mt-3">
                  Đã gửi: {review.createdAt && new Date(review.createdAt).toLocaleString('vi-VN')}
                </p>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}
