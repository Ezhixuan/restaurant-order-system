import { ref } from 'vue'

export function useLoading() {
  const loading = ref(false)

  const withLoading = async <T>(fn: () => Promise<T>): Promise<T> => {
    loading.value = true
    try {
      const result = await fn()
      return result
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    withLoading,
  }
}
