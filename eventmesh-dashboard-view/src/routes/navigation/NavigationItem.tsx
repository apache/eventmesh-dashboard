import React, { forwardRef } from 'react'
import { Button, ButtonProps, Stack, Typography, Chip } from '@mui/material'
import { styled } from '@mui/material/styles'
import { grey } from '@mui/material/colors'

const NavButton = styled(Button)({
  boxShadow: 'none',
  width: '100%',
  paddingLeft: 15,
  paddingRight: 15,
  paddingTop: 8,
  paddingBottom: 8,
  borderRadius: 8,
  textTransform: 'none',
  color: '#43497a',
  justifyContent: 'flex-start',

  '& .MuiButton-startIcon': {
    padding: 10,
    marginRight: 15,
    color: 'inherit',
    backgroundColor: 'white',
    borderRadius: 8,
    boxShadow: '1px 1px 5px 1px rgba(0,0,0,0.05)'
  },

  '&.active': {
    backgroundColor: 'white',
    boxShadow: '2px 2px 20px 5px rgba(0,0,0,0.05)',

    '& .MuiButton-startIcon': {
      boxShadow: 'none',
      color: 'white',
      backgroundColor: '#17c8eb'
    }
  }
})

interface NavigationItemProps extends ButtonProps {
  icon: React.ReactNode
  text: string
  count?: number
  active: boolean
}

const NavigationItem = forwardRef<typeof NavButton, NavigationItemProps>(
  ({ text, count, icon, active, onClick, ...props }, ref) => {
    return (
      <NavButton
        startIcon={icon}
        variant={active ? 'contained' : 'text'}
        className={active ? 'active' : ''}
        // sx={{ color: active ? 'white' : 'inherit' }}
        onClick={onClick}>
        <Stack
          sx={{ width: 1 }}
          direction="row"
          justifyContent="space-between"
          alignItems="center">
          <Typography fontSize="inherit">{text}</Typography>
          {(count ?? 0) > 0 && (
            <Chip
              sx={{
                height: 15,
                fontSize: 13,
                bgcolor: active ? grey[200] : 'white'
              }}
              size="small"
              color="default"
              label={count}
            />
          )}
        </Stack>
      </NavButton>
    )
  }
)

NavigationItem.displayName = 'NavigationItem'
export default NavigationItem
