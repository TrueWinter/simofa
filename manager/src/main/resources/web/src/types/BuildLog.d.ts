import type { SimofaLog } from './java';

// @ts-ignore
export default interface BuildLog extends SimofaLog {
  type: 'info' | 'warn' | 'error'
  log: string
  uuid: string
  timestamp: string
}
