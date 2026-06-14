import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// ── Response interceptor: unwrap ApiResponse<T> ────────────────────
api.interceptors.response.use(
  (response) => {
    // If the response has the TeamUp wrapper shape, return data directly
    if (response.data && 'success' in response.data && 'data' in response.data) {
      return response.data.data
    }
    return response.data
  },
  (error) => {
    const message =
      error.response?.data?.message ||
      error.response?.data ||
      error.message ||
      'An unexpected error occurred'
    return Promise.reject(new Error(message))
  }
)

// ── Auth header helper ─────────────────────────────────────────────
// Call this after login to attach the user ID to all requests.
export function setAuthUserId(userId) {
  api.defaults.headers.common['X-User-Id'] = String(userId)
}

// ══════════════════════════════════════════════════════════════════
// TASKS
// ══════════════════════════════════════════════════════════════════

export const taskApi = {
  /** List all tasks for a group */
  getByGroup: (groupId)            => api.get(`/tasks/group/${groupId}`),

  /** List tasks assigned to a user */
  getByAssignee: (userId)          => api.get(`/tasks/assignee/${userId}`),

  /** Get single task */
  getOne: (taskId)                 => api.get(`/tasks/${taskId}`),

  /** Create a new task */
  create: (data)                   => api.post('/tasks', data),

  /** Update task details (name, description, deadline) */
  update: (taskId, data)           => api.put(`/tasks/${taskId}`, data),

  /** Update progress (0–100) */
  updateProgress: (taskId, progress) =>
    api.patch(`/tasks/${taskId}/progress`, null, { params: { progress } }),

  /**
   * Submit task for review — legacy URL-based method.
   * For PDF uploads, use the /files/upload API instead.
   */
  submit: (taskId, fileUrl) =>
    api.post(`/tasks/${taskId}/submit`, null, { params: { fileUrl } }),

  /**
   * Submit task with a PDF file.
   * The file is uploaded to MongoDB GridFS and the task is marked PENDING_REVIEW.
   * Returns the updated task.
   *
   * Usage:
   *   const formData = new FormData()
   *   formData.append('file', pdfFile)
   *   formData.append('uploaderId', currentUser.userId)
   *   await taskApi.submitFile(taskId, formData)
   */
  submitFile: (taskId, formData) =>
    api.post(`/tasks/${taskId}/submit-file`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  /** Leader approves task → status becomes DONE */
  approve: (taskId, leaderId) =>
    api.post(`/tasks/${taskId}/approve`, null, { params: { leaderId } }),

  /** Leader rejects task → status reverts to IN_PROGRESS */
  reject: (taskId, leaderId) =>
    api.post(`/tasks/${taskId}/reject`, null, { params: { leaderId } }),

  /** Delete a task */
  delete: (taskId)                => api.delete(`/tasks/${taskId}`),

  /** Get submission history for a task */
  getSubmissions: (taskId)         => api.get(`/tasks/${taskId}/submissions`),

  /** Get contribution % breakdown for all members in a group */
  getContributions: (groupId)      => api.get(`/tasks/contributions/group/${groupId}`),
}

// ══════════════════════════════════════════════════════════════════
// FILES (MongoDB GridFS)
// ══════════════════════════════════════════════════════════════════

export const fileApi = {
  /**
   * Upload a PDF file to MongoDB GridFS.
   * Returns { gridFsFileId, originalFilename, fileSizeBytes, contentType }
   *
   * Usage:
   *   const formData = new FormData()
   *   formData.append('file', pdfFile)
   *   formData.append('uploaderId', String(userId))
   *   formData.append('taskId', String(taskId))
   *   const { gridFsFileId } = await fileApi.upload(formData)
   */
  upload: (formData) => api.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),

  /**
   * Get the download URL for a GridFS file.
   * Use this to construct the URL for <a href={downloadUrl}> links.
   *
   * Example:
   *   const url = fileApi.getDownloadUrl(gridFsFileId)  // => /api/files/{gridFsFileId}
   */
  getDownloadUrl: (gridFsFileId) =>
    `${import.meta.env.VITE_API_URL || '/api'}/files/${gridFsFileId}`,

  /**
   * Check if a file exists in GridFS (HEAD request).
   * Returns true if the file exists, false otherwise.
   */
  exists: (gridFsFileId) =>
    api.head(`/files/${gridFsFileId}`),

  /**
   * Delete a file from GridFS.
   */
  delete: (gridFsFileId) => api.delete(`/files/${gridFsFileId}`),
}

// ══════════════════════════════════════════════════════════════════
// GROUPS
// ══════════════════════════════════════════════════════════════════

export const groupApi = {
  getAll: ()                      => api.get('/groups'),
  getOne: (groupId)              => api.get(`/groups/${groupId}`),
  create: (data)                  => api.post('/groups', data),
  addMember: (groupId, userId)    => api.post(`/groups/${groupId}/members/${userId}`),
  removeMember: (groupId, userId) => api.delete(`/groups/${groupId}/members/${userId}`),
  getContributions: (groupId)     => api.get(`/groups/${groupId}/contributions`),
  delete: (groupId)              => api.delete(`/groups/${groupId}`),
}

// ══════════════════════════════════════════════════════════════════
// USERS
// ══════════════════════════════════════════════════════════════════

export const userApi = {
  getAll: ()                      => api.get('/users'),
  getOne: (userId)              => api.get(`/users/${userId}`),
  create: (data)                  => api.post('/users', data),
  delete: (userId)               => api.delete(`/users/${userId}`),
}

// ══════════════════════════════════════════════════════════════════
// DASHBOARD
// ══════════════════════════════════════════════════════════════════

export const dashboardApi = {
  getGroupDashboard: (groupId)    => api.get(`/dashboard/group/${groupId}`),
}

// ══════════════════════════════════════════════════════════════════
// PEER REVIEWS  (anonymous)
// ══════════════════════════════════════════════════════════════════

export const peerReviewApi = {
  /** Submit a peer review for a teammate */
  submit: (reviewerId, data)     => api.post('/peer-reviews', data, {
    params: { reviewerId },
  }),

  /** View reviews I received (anonymized — no reviewer info) */
  getReceived: (userId)           => api.get(`/peer-reviews/received/${userId}`),

  /** View reviews I gave (self-audit) */
  getGiven: (reviewerId)          => api.get(`/peer-reviews/given/${reviewerId}`),

  /** All reviews for a group — teacher only */
  getGroupReviews: (groupId, includeReviewer = false) =>
    api.get(`/peer-reviews/group/${groupId}`, { params: { includeReviewer } }),

  /** Average peer score for a user in a group */
  getAverageScore: (userId, groupId) =>
    api.get(`/peer-reviews/average/${userId}`, { params: { groupId } }),
}

// ══════════════════════════════════════════════════════════════════
// REPORTS  (teacher only)
// ══════════════════════════════════════════════════════════════════

export const reportApi = {
  /** Get report data as JSON */
  getReportData: (groupId, teacherId) =>
    api.get(`/reports/group/${groupId}`, { params: { teacherId } }),

  /** Download Excel report (.xlsx) */
  downloadExcel: (groupId, teacherId) =>
    api.get(`/reports/group/${groupId}/excel`, {
      params: { teacherId },
      responseType: 'blob',
    }),

  /** Download PDF report */
  downloadPdf: (groupId, teacherId) =>
    api.get(`/reports/group/${groupId}/pdf`, {
      params: { teacherId },
      responseType: 'blob',
    }),
}

// ══════════════════════════════════════════════════════════════════
// NOTIFICATIONS
// ══════════════════════════════════════════════════════════════════

export const notificationApi = {
  getAll: (userId, unreadOnly = false) =>
    api.get(`/notifications/user/${userId}`, { params: { unreadOnly } }),

  getUnreadCount: (userId)         =>
    api.get(`/notifications/user/${userId}/unread/count`),

  markAsRead: (notificationId)     =>
    api.patch(`/notifications/${notificationId}/read`),

  markAllAsRead: (userId)          =>
    api.patch(`/notifications/user/${userId}/read-all`),
}

export default api
