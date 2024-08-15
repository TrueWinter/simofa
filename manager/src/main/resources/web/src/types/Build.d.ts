import type { Website } from './java';

export default interface Build {
  id: string
  commit: string
  status: 'QUEUED' | 'PREPARING' | 'BUILDING' | 'DEPLOYING' | 'DEPLOYED' | 'ERROR' | 'STOPPED'
  website: Website
  runTime: number
}
