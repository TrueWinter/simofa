export interface AccountsResponse {
	success: boolean
	error?: string
	accounts?: Account[]
}

export default interface Account {
	id: number
	username: string
}