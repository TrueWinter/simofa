import type { Website } from "../../java"

export default interface Build {
	id: string
	commit: string
	status: 'queued' | 'building' | 'deploying' | 'deployed' | 'error' | 'stopped'
	website: Website
	runTime: number
}