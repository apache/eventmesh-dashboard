import { configureStore } from '@reduxjs/toolkit'
import { TypedUseSelectorHook, useDispatch, useSelector } from 'react-redux'
import publicSlice from './reducers/public/public.slice'

export const store = configureStore({
  reducer: {
    public: publicSlice,
  }
})

export const useAppSelector: TypedUseSelectorHook<
  ReturnType<typeof store.getState>
> = useSelector
export const useAppDispatch = () => useDispatch<typeof store.dispatch>()
