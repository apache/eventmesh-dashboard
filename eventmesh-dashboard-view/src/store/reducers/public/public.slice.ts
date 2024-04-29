import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { PublicState } from './public.types'


const initialState = {
  seletedClusterId: 1
} as PublicState

export const globalSlice = createSlice({
  name: 'public',
  initialState,
  reducers: {
    setSelectedClusterId(state, action: PayloadAction<PublicState['seletedClusterId']>) {
      state.seletedClusterId = action.payload
    },
  },
  extraReducers: (builder) => { }
})

export default globalSlice.reducer
