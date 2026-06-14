// ── API Response Types ──────────────────────────────────────────────

export const ApiResponseSchema = {
  success:  true,
  message:  'string',
  data:     null,
}

export const UserSchema = {
  userId:   0,
  name:     '',
  email:    '',
  role:     'STUDENT',   // STUDENT | TEACHER
  createdAt: '',
}

export const GroupSchema = {
  groupId:    0,
  groupName:  '',
  classId:    '',
  createdAt:  '',
  leader:     UserSchema,
  members:    [],
  tasks:      [],
}

export const TaskSchema = {
  taskId:             0,
  taskName:           '',
  description:        '',
  deadline:           '',
  progress:           0,
  status:             'TODO',
  lastProgressUpdate: '',
  createdAt:          '',
  updatedAt:          '',
  group:  { groupId: 0, groupName: '', classId: '' },
  assignedTo: UserSchema,
}

export const SubmissionSchema = {
  submissionId: 0,
  fileUrl:      '',
  submittedAt:  '',
  taskId:       0,
  taskName:     '',
}

export const PeerReviewSchema = {
  reviewId:  0,
  score:    0,
  comment:  '',
  group:    { groupId: 0, groupName: '' },
  reviewee: { userId: 0, name: '' },
  createdAt: '',
  // reviewer field intentionally omitted — NEVER exposed to students
}

export const ContributionSchema = {
  userId:              0,
  userName:            '',
  contributionPercent: 0,
  taskCount:           0,
  averageProgress:     0,
  role:                '',
}

export const DashboardSchema = {
  groupId:       0,
  groupName:     '',
  classId:       '',
  generatedAt:   '',
  members: [{
    userId:                0,
    name:                 '',
    email:                '',
    role:                 '',
    contributionPercent:  0,
    taskCount:            0,
    averageProgress:      0,
    completedTaskCount:    0,
    pendingTaskCount:     0,
  }],
  stats: {
    totalTasks:             0,
    completedTasks:         0,
    pendingTasks:           0,
    overdueTasks:          0,
    overallCompletionPercent: 0,
  },
}

export const NotificationSchema = {
  notificationId: 0,
  message:        '',
  isRead:         false,
  type:           '',
  taskId:         null,
  userId:         0,
  createdAt:      '',
}

// ── Request DTO types ──────────────────────────────────────────────

export const CreateTaskRequest = {
  taskName:    '',
  description: '',
  deadline:    '',    // ISO string
  progress:    0,
  groupId:     null,
  assignedToId: null,
}

export const SubmitTaskRequest = {
  taskId:  0,
  fileUrl: '',
}
