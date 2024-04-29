export type ListApiRespone<D> = {
  code: number
  currentPage: number
  data: D[]
  message: string
  pageSize: number
  total: number
}

export type FetchRespone<T> = {
  data: T
}
