import React from 'react'
import { createTheme, ThemeProvider, CssBaseline } from '@mui/material'
import './App.css'
import AppRoutes from './routes/Routes'

function App() {
  const theme = React.useMemo(
    () =>
      createTheme({
        palette: {
          // mode: prefersDarkMode ? 'dark' : 'light',
          primary: {
            main: '#1f95fc'
          }
        }
      }),
    []
  ) // [prefersDarkMode]

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AppRoutes />
    </ThemeProvider>
  )
}

export default App
