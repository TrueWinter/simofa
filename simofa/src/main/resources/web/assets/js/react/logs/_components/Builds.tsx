import { useEffect, useState, useRef, ReactElement } from 'react'
import Skeleton from '../../_common/Skeleton'
import type { Website } from '../../../java';
import Build from '../../_common/Build'
import Form, { FormInput } from '../../_common/Form'
import addJwtParam from '../../_common/_auth'
import Modal from '../../_common/Modal'

interface BuildsProps {
	website?: Website
}

interface TableRowFunction {
	(b: Build): ReactElement
}

type TableRowPage = 'website' | 'builds'

interface TableRow {
	name: string
	pages: TableRowPage[]
	data: TableRowFunction
}

interface BuildStopIds {
	website: number
	build: string
}

export function parseTime(ms: number): string {
	let seconds = Math.floor(ms / 1000);

	if (seconds < 60) {
		return `${seconds.toString()}s`;
	}

	let buildMinutes = Math.floor(seconds / 60);
	let buildSeconds = seconds - (buildMinutes * 60);

	return `${buildMinutes.toString()}m ${buildSeconds.toString()}s`;
}

export default function Builds({
	website
}: BuildsProps) {
	const [loading, setLoading] = useState(true)
	const [error, setError] = useState('')
	const [builds, setBuilds] = useState([] as Build[])
	const [deleteModalData, setDeleteModalData] = useState(null as BuildStopIds)
	const interval = useRef(null)

	// This component is shared between the website logs page
	// and the build queue page, so defining the rows like this
	// makes it easier to manage
	const tableRows: TableRow[] = [{
		name: 'ID',
		pages: ['builds', 'website'],
		data: b => (
			<td title={b.id}>{b.id.split('-')[0]}</td>
		)
	}, {
		name: 'Website',
		pages: ['builds'],
		data: b => (
			<td>{b.website.name}</td>
		)
	},
	{
		name: 'Commit Message',
		pages: ['builds', 'website'],
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
		pages: ['builds', 'website'],
		data: b => (
			<td>{b.status}</td>
		)
	}, {
		name: 'Duration',
		pages: ['builds', 'website'],
		data: b => (
			<>
				<td>
				{parseTime(b.runTime)}
				</td>
			</>
		)
	}, {
		name: 'Logs',
		pages: ['builds', 'website'],
		data: b => (
			<td><a href={addJwtParam(`/websites/${b.website.id}/build/${b.id}/logs`)}>Logs</a></td>
		)
	}, {
		name: 'Stop',
		pages: ['builds', 'website'],
		data: b => (
			<td>
				<FormInput>
					<button style={{
						float: 'unset'
					}} disabled={!['queued', 'building'].includes(b.status)}
					onClick={() => setDeleteModalData({
						website: b.website.id,
						build: b.id
					})}>Stop</button>
				</FormInput>
			</td>
		)
	}]

	const thisPage: TableRowPage = website ? 'website' : 'builds'
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

	useEffect(() => {
		if (deleteModalData === null) return;
		let b = builds.filter(e => e.id === deleteModalData.build);
		if (b.length === 0) return;
		if (!['queued', 'building'].includes(b[0].status)) {
			setDeleteModalData(null);
		}
	}, [builds])

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
						<td colSpan={thisPageTableRows.length}>{thisPage === 'builds' ? 'No builds queued' : 'No builds queued for this website'}</td>
					</tr>
				: builds.map(b => <tr>{thisPageTableRows.map(r => r.data(b))}</tr>))}
			</tbody>
		</table>}

		{deleteModalData && <Modal title="Stop Build" close={() => setDeleteModalData(null)}>
				<p>Are you sure you want to stop build with ID {deleteModalData.build.split('-')[0]}?</p>
				<Form action={`/websites/${deleteModalData.website}/build/${deleteModalData.build}/stop${thisPage === 'builds' ? '?redirectTo=%2Fbuilds%2Fqueue' : ''}`}>
					<FormInput>
						<button type='submit'>Stop</button>
					</FormInput>
				</Form>
			</Modal>
		}
		</>
	)
}