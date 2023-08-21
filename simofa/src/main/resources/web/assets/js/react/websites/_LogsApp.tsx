import { useEffect, useRef, useState } from 'react'
import render from '../_common/_render'
import BuildLog from '../_common/BuildLog'
import Skeleton from '../_common/Skeleton'
import addJwtParam from '../_common/_auth'

export default function LogsApp() {
	const [logs, setLogs] = useState([] as BuildLog[])
	const [error, setError] = useState('')
	const [loading, setLoading] = useState(true)
	const [updating, setUpdating] = useState(false)
	const [status, setStatus] = useState('<unknown>')
	const interval = useRef(null)
	const logRef = useRef(null as HTMLDivElement)
	const fetchingLogs = useRef(false)
	const lastFetch = useRef(0)

	function stopUpdating(id: number) {
		clearInterval(id)
		setUpdating(false)
	}

	function getDate(timestamp: string): string {
		let d: Date = new Date(parseInt(timestamp));
		let date = d.toISOString().split('T')[0];
		let time = d.toISOString().split('T')[1].split('.')[0];

		return `${date} ${time} GMT`;
	}

	function loadLogs(websiteId: string, buildId: string) {
		if (fetchingLogs.current) {
			console.log('Still waiting for previous log fetch to finish');
			return;
		}

		fetchingLogs.current = true

		fetch(addJwtParam(`/api/websites/${websiteId}/build/${buildId}/logs${lastFetch.current > 0 ? `?after=${lastFetch.current.toString()}` : ''}`))
			.then(d => d.json())
			.then(d => {
				setLogs(l => l.concat(d.logs))

				if (['error', 'stopped', 'deployed'].includes(d.status)) {
					console.log('Build complete, clearing interval');
					stopUpdating(interval.current)
				} else {
					setUpdating(true)
				}

				setStatus(d.status);

				if (d.logs.length > 0) {
					lastFetch.current = d.logs[d.logs.length - 1].timestamp
				}

				// wait until next tick
				setTimeout(() => {
					logRef.current?.scrollTo(0, logRef.current.scrollHeight)
				}, 0);
			})
			.catch(err => {
				console.error(err)
				if (interval.current) {
					stopUpdating(interval.current)
					setStatus('<unknown>')
				}
			})
			.finally(() => {
				setLoading(false)
				fetchingLogs.current = false
			})
	}

	useEffect(() => {
		const websiteId = document.getElementById('website-id').dataset.id;
		const buildId = document.getElementById('build-id').dataset.id;

		loadLogs(websiteId, buildId)

		// WebSockets would be a better solution, but that
		// requires keeping track of active sessions on the
		// server, closing old connections, and handling
		// auth before sending any data.
		interval.current = setInterval(() => {
			loadLogs(websiteId, buildId)
		}, 5 * 1000)
		
		return () => {
			stopUpdating(interval.current)
		}
	}, [])

	return (
		<>
			<div className="success" style={{
				display: 'flex',
				justifyContent: 'space-around'
			}}>
				<span>Status: {status}</span>
				{updating && <span>Updating every 5 seconds</span>}
			</div>
			<hr/>
	
			{error ? <div className="error">{error}</div> :
				(loading ? new Array(10).fill(0).map(() => <Skeleton />) :
					<div ref={logRef} style={{
						maxHeight: '60vh',
						height: '60vh',
						overflow: 'auto'
					}}>
						{logs.map(e => <div style={{
							color: e.type === 'info' ? 'white' : 'orangered'
						}} key={e.uuid}><span style={{
							color: '#888',
							fontSize: 'small'
						}}>{getDate(e.timestamp)}</span> [{e.type}] {e.log}</div>)}
					</div>
				)
			}
		</>
	)
}

render(<LogsApp />)