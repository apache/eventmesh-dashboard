export type ListApiRespone<D> = {
  code: number
  message: string

  currentPage: number
  data: D[]
  pageSize: number
  total: number
}

export type FetchRespone<T> = {
  code: number
  message: string
  data: T
}
