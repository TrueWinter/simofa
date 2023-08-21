import { useEffect, useState, useRef, ReactElement } from 'react'
import Skeleton from '../../_common/Skeleton'
import Website from '../../_common/Website'
import Build from '../../_common/Build'
import Form, { FormInput } from '../../_common/Form'
import addJwtParam from '../../_common/_auth'

interface BuildsProps {
	website?: Website
}

interface TableRowFunction {
	(b: Build): ReactElement
}

type TableRowPage = 'website' | 'docker'

interface TableRow {
	name: string
	pages: TableRowPage[]
	data: TableRowFunction
}

export default function Builds({
	website
}: BuildsProps) {
	const [loading, setLoading] = useState(true)
	const [error, setError] = useState('')
	const [builds, setBuilds] = useState([] as Build[])
	const interval = useRef(null)

	// This component is shared between the website logs page
	// and the Docker queue page, so defining the rows like this
	// makes it easier to manage
	const tableRows: TableRow[] = [{
		name: 'ID',
		pages: ['docker', 'website'],
		data: b => (
			<td title={b.id}>{b.id.split('-')[0]}</td>
		)
	}, {
		name: 'Website',
		pages: ['docker'],
		data: b => (
			<td>{b.website.name}</td>
		)
	},
	{
		name: 'Commit Message',
		pages: ['docker', 'website'],
		data: b => (
			<>
				{b.commit ?
					<td title={b.commit}>{b.commit.length > 24 ? `${b.commit.substring(0, 24)}...` : b.commit}</td>:
					<td>{'<none>'}</td>
				}
			</>
		)
	}, {
		name: 'Status',
		pages: ['docker', 'website'],
		data: b => (
			<td>{b.status}</td>
		)
	}, {
		name: 'Duration',
		pages: ['docker', 'website'],
		data: b => {
			function parseTime(ms: number): string {
				let seconds = Math.floor(ms / 1000);

				if (seconds < 60) {
					return `${seconds.toString()}s`;
				}

				let buildMinutes = Math.floor(seconds / 60);
				let buildSeconds = seconds - (buildMinutes * 60);

				return `${buildMinutes.toString()}m ${buildSeconds.toString()}s`;
			}

			return (<td>
				{parseTime(b.runTime)}
			</td>)
		}
	}, {
		name: 'Logs',
		pages: ['docker', 'website'],
		data: b => (
			<td><a href={addJwtParam(`/websites/${b.website.id}/build/${b.id}/logs`)}>Logs</a></td>
		)
	}, {
		name: 'Stop',
		pages: ['docker', 'website'],
		data: b => (
			<td>
				<Form action={`/websites/${b.website.id}/build/${b.id}/stop`}>
					<FormInput>
						<button style={{
							float: 'unset'
						}} type='submit' disabled={!['queued', 'building'].includes(b.status)}>Stop</button>
					</FormInput>
				</Form>
			</td>
		)
	}]

	const thisPage: TableRowPage = website ? 'website' : 'docker'
	const thisPageTableRows = tableRows.filter(r => r.pages.includes(thisPage))

	function loadLogs() {
		fetch(addJwtParam(website ? `/api/queue?website=${website.id}` : '/api/queue'))
			.then(d => d.json())
			.then(d => {
				if (!d.success) {
					setError(d.error || 'An error occurred')
				} else {
					setBuilds(d.queue)
				}
			})
			.catch(e => {
				console.error(e)
				if (interval.current) {
					clearInterval(interval.current)
				}
			})
			.finally(() => setLoading(false))
	}

	useEffect(() => {
		loadLogs();

		interval.current = setInterval(() => {
			loadLogs();
		}, 5 * 1000)

		return () => {
			if (interval.current) {
				clearInterval(interval.current)
			}
		}
	}, [])

	return (
		<>
		{error ? <div className='error'>{error}</div> :
		<table>
			<thead>
				<tr>
					{thisPageTableRows.map(r => <td>{r.name}</td>)}
				</tr>
			</thead>
			<tbody>
			{loading ? new Array(1).fill(0).map(() => <tr>
					{new Array(thisPageTableRows.length).fill(0).map(() => <td><Skeleton /></td>)}
				</tr>)
				: (builds.length === 0 ?
					<tr>
						<td colSpan={thisPageTableRows.length}>{thisPage === 'docker' ? 'No builds queued' : 'No builds queued for this website'}</td>
					</tr>
				: builds.map(b => <tr>{thisPageTableRows.map(r => r.data(b))}</tr>))}
			</tbody>
		</table>}
		</>
	)
}