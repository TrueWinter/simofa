import { useEffect, useState } from 'react'
import type { Image as DockerImage } from '../../../java'
import Skeleton from '../../_common/Skeleton'
import addJwtParam from '../../_common/_auth';

export default function Images() {
	const [images, setImages] = useState([] as DockerImage[])
	const [error, setError] = useState('');
	const [loading, setLoading] = useState(true);

	useEffect(() => {
		fetch(addJwtParam('/api/builds/images'))
			.then(d => d.json())
			.then(setImages)
			.catch(e => setError(`An error occurred: ${e}`))
			.finally(() => setLoading(false))
	}, [])

	return (
		<>
			{error ? <div className="error">{error}</div> :
			<table>
				<thead>
					<tr>
						<th>Name</th>
						<th>Size</th>
					</tr>
				</thead>
				<tbody>
					{loading ? new Array(2).fill(0).map(e => <tr>
						{new Array(2).fill(0).map(e => <td><Skeleton /></td>)}
					</tr>) : images.map(e => <tr>
						<td>{e.name}</td>
						<td>{e.size}</td>
					</tr>)}
				</tbody>
			</table>}
		</>
	)
}