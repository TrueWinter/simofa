import { useEffect, useRef, useState } from 'react'
import Skeleton from '../../_common/Skeleton'
import DockerContainer from '../../_common/DockerContainer';
import Form, { FormInput } from '../../_common/Form';
import StatusIndicator from '../_components/StatusIndicator';
import addJwtParam from '../../_common/_auth';

export default function Containers() {
	const [containers, setContainers] = useState([] as DockerContainer[])
	const [error, setError] = useState('')
	const [loading, setLoading] = useState(true)
	const interval = useRef(null)

	function load() {
		fetch(addJwtParam('/api/docker/containers'))
			.then(d => d.json())
			.then(setContainers)
			.catch(e => {
				console.error(e)
				if (interval.current) {
					clearInterval(interval.current)
				}
			})
			.finally(() => setLoading(false))
	}

	useEffect(() => {
		load()
		
		interval.current = setInterval(() => {
			load()
		}, 5 * 1000)

		return () => {
			if (interval.current) {
				clearInterval(interval.current)
			}
		}
	}, [])

	return (
		<>
			{error ? <div className="error">{error}</div> :
			<>
				{!loading && containers.length === 0 ? 'No containers found' :
					<table>
						<thead>
							<tr>
								<th colSpan={2}>ID</th>
								<th>Name</th>
								<th>Status</th>
								<th>Actions</th>
							</tr>
						</thead>
						<tbody>
							{loading ? new Array(2).fill(0).map(e => <tr>
								{new Array(4).fill(0).map((e, i) => <td colSpan={i === 0 ? 2 : undefined}><Skeleton /></td>)}
							</tr>) : containers.map(e => <tr>
								<td style={{
									borderRightColor: 'transparent',
									paddingRight: 0
								}}><StatusIndicator state={e.state} /></td>
								<td style={{
									paddingLeft: 0
								}}>{e.id.substring(0, 12)}</td>
								<td>{e.name}</td>
								<td>{e.status}</td>
								<td>
									<Form action={`/docker/containers/${e.id}/delete`}>
										<FormInput>
											<button type="submit" style={{
												float: 'unset'
											}}>Delete</button>
										</FormInput>
									</Form>
								</td>
							</tr>)}
						</tbody>
					</table>
				}
			</>
			}
		</>
	)
}