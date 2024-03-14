import React from 'react'
import { createTheme, ThemeProvider, CssBaseline } from '@mui/material'
import AppRoutes from './routes/Routes'

function App() {
  const theme = React.useMemo(
    () =>
      createTheme({
        palette: {
          primary: {
            main: '#1f95fc'
          }
        },
        typography: {
          fontSize: 14
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
