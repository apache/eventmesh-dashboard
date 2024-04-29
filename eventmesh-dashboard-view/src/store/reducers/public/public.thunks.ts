import { createAsyncThunk } from '@reduxjs/toolkit'
import { IValidationErrors } from '../store.types'

export const getUsersAsync = createAsyncThunk<
  any,
  void,
  {
    rejectValue: IValidationErrors
  }
>('users/getUsers', async (_, { rejectWithValue }) => {})
