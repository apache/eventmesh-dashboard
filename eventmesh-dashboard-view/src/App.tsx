import React from 'react'
import { createTheme, ThemeProvider, CssBaseline } from '@mui/material'
import AppRoutes from './routes/Routes'
import GlobalStyles from '@mui/material/GlobalStyles'
import { AppBackgroundColor, AppThemeConfig } from './app.const'


function App() {
  const theme = React.useMemo(() => createTheme(AppThemeConfig), []) // [prefersDarkMode]

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <GlobalStyles styles={{ backgroundColor: AppBackgroundColor }} />
      <AppRoutes />
    </ThemeProvider>
  )
}

export default App
