import React, { forwardRef } from 'react'
import { Button, ButtonProps, Stack, Typography, Chip } from '@mui/material'
import { styled } from '@mui/material/styles'
import { grey } from '@mui/material/colors'

const NavButton = styled(Button)({
  boxShadow: 'none',
  width: '100%',
  textTransform: 'none',
  fontSize: 15,
  color: grey[800],
  justifyContent: 'flex-start',

  '& .MuiButton-startIcon': {
    marginRight: 15,
    color: 'inherit'
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
        sx={{ color: active ? 'white' : 'inherit' }}
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
                bgcolor: active ? 'white' : grey[200]
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
