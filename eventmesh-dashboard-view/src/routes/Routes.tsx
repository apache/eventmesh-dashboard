import React from 'react'
import { useRoutes, Navigate } from 'react-router-dom'
import RootLayout from './RootLayout'
import Home from './home/Home'
import Topic from './topic/Topic'
import Runtime from './runtime/Runtime'
import Connection from './connection/Connection'
import Message from './message/Message'
import Security from './security/Security'
import Users from './users/Users'
import Logs from './logs/Logs'
import Settings from './settings/Settings'

const AppRoutes = () => {
  return useRoutes([
    {
      path: '/',
      element: <RootLayout />,
      children: [
        {
          path: '*',
          element: <Navigate to="/home" replace />
        },
        { path: '', element: <Navigate to="/home" /> },
        { path: 'home', element: <Home /> },
        { path: 'runtime', element: <Runtime /> },
        { path: 'topic', element: <Topic /> },
        { path: 'connection', element: <Connection /> },
        { path: 'message', element: <Message /> },
        { path: 'security', element: <Security /> },
        { path: 'settings', element: <Settings /> },
        { path: 'users', element: <Users /> },
        { path: 'logs', element: <Logs /> }
      ]
    }
  ])
}

export default AppRoutes
