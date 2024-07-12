import { useEffect, useRef, useState } from 'react'
import Skeleton from '../../_common/Skeleton'
import type { Container as DockerContainer } from '../../../java';
import Form, { FormInput } from '../../_common/Form';
import StatusIndicator from '../_components/StatusIndicator';
import addJwtParam from '../../_common/_auth';
import Modal from '../../_common/Modal';

export default function Containers() {
	const [containers, setContainers] = useState([] as DockerContainer[])
	const [error, setError] = useState('')
	const [loading, setLoading] = useState(true)
	const interval = useRef(null)
	const [deleteModalData, setDeleteModalData] = useState('')

	function load() {
    function handleError() {
      if (interval.current) {
        clearInterval(interval.current)
      }
    }

		fetch(addJwtParam('/api/builds/containers'))
			.then(d => d.json())
			.then(d => {
        if (d.error) {
					setError(d.error)
          handleError()
				} else {
					setContainers(d)
				}
      })
			.catch(e => {
				console.error(e)
        handleError()
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
					<div className="table">
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
										<FormInput>
											<button style={{
												float: 'unset'
											}} onClick={() => setDeleteModalData(e.id)}>Delete</button>
										</FormInput>
									</td>
								</tr>)}
							</tbody>
						</table>
					</div>
				}


				{deleteModalData && <Modal title="Delete Container" close={() => setDeleteModalData(null)}>
						<p style={{
							wordBreak: 'break-word'
						}}>Are you sure you want to delete container with ID {deleteModalData}?</p>
						<Form action={`/builds/containers/${deleteModalData}/delete`}>
							<FormInput>
								<button type="submit">Delete</button>
							</FormInput>
						</Form>
					</Modal>
				}
			</>
			}
		</>
	)
}