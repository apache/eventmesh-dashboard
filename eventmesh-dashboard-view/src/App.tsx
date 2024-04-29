import React from 'react'
import { createTheme, ThemeProvider, CssBaseline } from '@mui/material'
import AppRoutes from './routes/Routes'
import GlobalStyles from '@mui/material/GlobalStyles'

const inputGlobalStyles = (
  <GlobalStyles styles={{ backgroundColor: '#f9fafb' }} />
)

function App() {
  const theme = React.useMemo(
    () =>
      createTheme({
        palette: {
          primary: {
            main: '#43497a'
          },
          background: {}
        },
        typography: {
          fontSize: 12
        }
      }),
    []
  ) // [prefersDarkMode]

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {inputGlobalStyles}
      <AppRoutes />
    </ThemeProvider>
  )
}

export default App
