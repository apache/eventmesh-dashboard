import React from 'react'
import { useRoutes, Navigate } from 'react-router-dom'
import Home from './home/Home'
import ClusterOverView from './cluster/overview/Overview'
import ClusterTopic from './cluster/topic/Topic'
import ClusterRuntime from './cluster/runtime/Runtime'
import ClusterConnection from './cluster/connection/Connection'
import ClusterMessage from './cluster/message/Message'
import ClusterSecurity from './cluster/security/Security'
import Users from './users/Users'
import Logs from './logs/Logs'
import Settings from './settings/Settings'
import Clusters from './cluster/Clusters'

const AppRoutes = () => {
  return useRoutes([
    {
      path: '*',
      element: <Navigate to="home" replace />
    },
    { path: 'home', element: <Home /> },
    {
      path: 'clusters',
      element: <Clusters />,
      children: [
        { path: 'clusters', element: <Clusters /> },
        {
          path: ':clusterId',
          children: [
            {
              path: '*',
              element: <Navigate to="home" replace />
            },
            { path: 'overview', element: <ClusterOverView /> },
            { path: 'runtime', element: <ClusterRuntime /> },
            { path: 'topic', element: <ClusterTopic /> },
            { path: 'connection', element: <ClusterConnection /> },
            { path: 'message', element: <ClusterMessage /> },
            { path: 'security', element: <ClusterSecurity /> }
          ]
        }
      ]
    },
    { path: 'settings', element: <Settings /> },
    { path: 'users', element: <Users /> },
    { path: 'logs', element: <Logs /> }
  ])
}

export default AppRoutes
