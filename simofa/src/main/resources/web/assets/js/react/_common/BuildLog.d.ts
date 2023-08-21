export default interface BuildLog {
	type: 'info' | 'warn' | 'error'
	log: string
	uuid: string
	timestamp: string
}