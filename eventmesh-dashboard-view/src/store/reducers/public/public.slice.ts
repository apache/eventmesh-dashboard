import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { PublicState } from './public.types'
import { NavMenuIdEnum } from '../../../routes/navigation/navigation.types'

const initialState = {
  navigation: { activeMenuId: NavMenuIdEnum.Home, pinSubmenuIds: [] }
} as PublicState

export const globalSlice = createSlice({
  name: 'public',
  initialState,
  reducers: {
    setNavigationActiveMenuId(state, action: PayloadAction<NavMenuIdEnum>) {
      state.navigation.activeMenuId = action.payload
    },
    setNavigationPinMenuIds(state, action: PayloadAction<NavMenuIdEnum[]>) {
      state.navigation.pinSubmenuIds = action.payload
    }
  },
  extraReducers: (builder) => {}
})

export default globalSlice.reducer
