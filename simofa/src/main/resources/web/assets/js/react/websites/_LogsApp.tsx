import { useEffect, useRef, useState } from 'react'
import render from '../_common/_render'
import BuildLog from '../_common/BuildLog'
import Skeleton from '../_common/Skeleton'
import addJwtParam from '../_common/_auth'
import { parseTime } from '../logs/_components/Builds'
import { useDebouncedState } from '../_common/useDebouncedState'

export default function LogsApp() {
	const [logs, setLogs] = useDebouncedState<BuildLog[]>([], 100, doScroll)
	const [error, setError] = useState('')
	const [loading, setLoading] = useState(true)
	const [status, setStatus] = useState('<unknown>')
	const [duration, setDuration] = useState(0)
	const logRef = useRef(null as HTMLDivElement)
	const fetchingLogs = useRef(false)
	const shouldScrollDown = useRef(true)
  const eventSource = useRef<EventSource>(null);

	function stopUpdating() {
    if (eventSource.current) {
      eventSource.current.close();
    }
	}

	function getDate(timestamp: string): string {
		let d: Date = new Date(parseInt(timestamp));
		let date = d.toISOString().split('T')[0];
		let time = d.toISOString().split('T')[1].split('.')[0];

		return `${date} ${time} GMT`;
	}

	function doScroll() {
		if (shouldScrollDown.current) {
      // Wait until next tick
      setTimeout(() => {
        logRef.current?.scrollTo(0, logRef.current.scrollHeight);
      }, 0);
		}
	}

  function update(d) {
    setStatus(d.status);
    setDuration(d.duration);
    setLogs(l => l.concat(d.logs));
  }

	function loadLogs(websiteId: string, buildId: string) {
		if (fetchingLogs.current) {
			console.log('Still waiting for previous log fetch to finish');
			return;
		}

		fetchingLogs.current = true

		fetch(addJwtParam(`/api/websites/${websiteId}/build/${buildId}/logs`))
			.then(d => d.json())
			.then(d => {
				if (['error', 'stopped', 'deployed'].includes(d.status)) {
					stopUpdating()
				}

				update(d);

        const finishedStatuses = ['STOPPED', 'ERROR', 'DEPLOYED'];
        if (!eventSource.current && !finishedStatuses.includes(d.status)) {
          eventSource.current = new EventSource(addJwtParam(`/api/sse/websites/${websiteId}/build/${buildId}/logs`));
          eventSource.current.onerror = () => {
            // TODO: Show error notification
            eventSource.current.close();
          };
          eventSource.current.onmessage = (event) => {
            const logs = JSON.parse(event.data);
            update(logs);
            if (finishedStatuses.includes(logs.status)) {
              // Wait 10 seconds for final logs to come through
              setTimeout(() => {
                eventSource.current.close();
              }, 10000);
            }
          };
        }
			})
			.catch(err => {
				console.error(err)
					stopUpdating()
					setStatus('<unknown>')
					setDuration(0)
			})
			.finally(() => {
				setLoading(false)
				fetchingLogs.current = false
			})
	}

	function handleScroll() {
		shouldScrollDown.current = logRef.current.scrollHeight - logRef.current.scrollTop <= logRef.current.clientHeight + 32
	}

	function handleResize() {
		// .toString() to make TypeScript happy
		logRef.current.style.height = `${(window.innerHeight - (document.body.clientHeight - logRef.current.clientHeight + 8)).toString()}px`;
		doScroll();
	}

	useEffect(() => {
		const websiteId = document.getElementById('website-id').dataset.id;
		const buildId = document.getElementById('build-id').dataset.id;

		loadLogs(websiteId, buildId);
		handleResize()

		logRef.current.addEventListener('scroll', handleScroll)
		window.addEventListener('resize', handleResize)

		return () => {
			stopUpdating()
			logRef.current.removeEventListener('scroll', handleScroll)
			window.removeEventListener('resize', handleResize)
		}
	}, [])

	return (
		<>
			<div className="success" style={{
				display: 'flex',
				justifyContent: 'space-around',
				gap: '16px'
			}}>
				<span>Status: {status.toLowerCase()}</span>
				<span>Duration: {duration === 0 ? '0s' : parseTime(duration)}</span>
			</div>
			<hr/>
	
			{error ? <div className="error">{error}</div> :
				<div ref={logRef} style={{
					overflow: 'auto'
				}}>
					{loading ? new Array(10).fill(0).map(() => <Skeleton />) :
						<>
						{logs.map(e => <div style={{
							color: e.type === 'info' ? 'white' : 'orangered'
						}} key={e.uuid}><span style={{
							color: '#888',
							fontSize: 'small'
						}}>{getDate(e.timestamp)}</span> [{e.type}] {e.log}</div>)}
						</>
					}
				</div>
			}
		</>
	)
}

render(<LogsApp />)