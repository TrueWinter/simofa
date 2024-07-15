import type { Account } from '../../java'

export interface AccountsResponse {
	success: boolean
	error?: string
	accounts?: Account[]
}