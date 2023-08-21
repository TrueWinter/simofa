export interface WebsitesResponse {
	success: boolean
	websites?: Website[]
	error?: string
}

export default interface Website {
	id: number
	name: string
	buildCommand: string
	dockerImage: string
	memory: number
	cpu: number
	gitUrl: string
	gitBranch: string
	postBuildCommand?: string
	deploymentCommand: string
	postDeploymentCommand?: string
	deploymentFailedCommand: string
	deploymentServer: number
}