import { ref, computed } from 'vue'

interface PaginationOptions {
  pageSize?: number
}

export function usePagination<T>(options: PaginationOptions = {}) {
  const { pageSize = 10 } = options

  const currentPage = ref(1)
  const pageSizeRef = ref(pageSize)
  const total = ref(0)
  const list = ref<T[]>([])

  const totalPages = computed(() => Math.ceil(total.value / pageSizeRef.value))

  const setList = (data: T[], totalCount: number) => {
    list.value = data
    total.value = totalCount
  }

  const reset = () => {
    currentPage.value = 1
    total.value = 0
    list.value = []
  }

  return {
    currentPage,
    pageSize: pageSizeRef,
    total,
    list,
    totalPages,
    setList,
    reset,
  }
}
