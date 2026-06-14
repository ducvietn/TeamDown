import { createContext, useContext, useState, useCallback } from 'react'
import { setAuthUserId } from '../api/index.js'

const AppContext = createContext(null)

export function AppProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(() => {
    const stored = localStorage.getItem('teamup_user')
    return stored ? JSON.parse(stored) : null
  })
  const [currentGroup, setCurrentGroup] = useState(() => {
    const stored = localStorage.getItem('teamup_group')
    return stored ? JSON.parse(stored) : null
  })

  const login = useCallback((user) => {
    localStorage.setItem('teamup_user', JSON.stringify(user))
    setCurrentUser(user)
    setAuthUserId(user.userId)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('teamup_user')
    setCurrentUser(null)
  }, [])

  const selectGroup = useCallback((group) => {
    localStorage.setItem('teamup_group', JSON.stringify(group))
    setCurrentGroup(group)
  }, [])

  const isLeader = useCallback(() => {
    if (!currentUser || !currentGroup) return false
    return currentGroup.leader?.userId === currentUser.userId
  }, [currentUser, currentGroup])

  const isTeacher = useCallback(() => {
    return currentUser?.role === 'TEACHER'
  }, [currentUser])

  return (
    <AppContext.Provider value={{
      currentUser,
      currentGroup,
      login,
      logout,
      selectGroup,
      isLeader,
      isTeacher,
    }}>
      {children}
    </AppContext.Provider>
  )
}

export const useApp = () => {
  const ctx = useContext(AppContext)
  if (!ctx) throw new Error('useApp must be used inside <AppProvider>')
  return ctx
}
