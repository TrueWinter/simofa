import type { Website } from '../../java';

export interface WebsitesResponse {
	success: boolean
	websites?: Website[]
	error?: string
}