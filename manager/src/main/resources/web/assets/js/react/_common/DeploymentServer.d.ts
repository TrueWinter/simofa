export interface DeploymentServersResponse {
	success: boolean
	servers?: DeploymentServer[]
	error?: string
}

export default interface DeploymentServer {
	id: string
	name: string
	url: string
	key: string
}