import { Fragment, useEffect, useState } from 'react'
import render from '../_common/_render'
import Skeleton from '../_common/Skeleton'
import DeploymentServer from '../_common/DeploymentServer'
import Form, { FormInput } from '../_common/Form'
import addJwtParam from '../_common/_auth'

function App() {
	const [servers, setServers] = useState([] as DeploymentServer[]);
	const [error, setError] = useState('');
	const [loading, setLoading] = useState(true);

	useEffect(() => {
		fetch(addJwtParam('/api/deployment-servers')).then(d => {
			d.json().then(s => {
				if (!s.success) {
					return setError(s.error || 'An error occurred');
				}
				setServers(s.servers);
			});
		}).catch(e => {
			console.error(e);
			setError(`An error occurred while fetching deployment servers: ${e}`);
		}).finally(() => {
			setLoading(false);
		});
	}, [])

	return (
		<>
			{error ? <div className="error">{error}</div> :
				<table>
					<thead>
						<tr>
							<td>ID</td>
							<td>Name</td>
							<td>URL</td>
							<td>Edit</td>
							<td>Delete</td>
						</tr>
					</thead>
					<tbody>
						{loading ?
							new Array(2).fill(0).map((e, i) => <Fragment key={i}>
								<tr>
									{new Array(5).fill(0).map((e, i) => <td key={i}><Skeleton/></td>)}
								</tr>
							</Fragment>) :
							<>
								{servers.map(e => {
									return (
										<tr key={e.id}>
											<td>{e.id}</td>
											<td>{e.name}</td>
											<td>{e.url}</td>
											<td><a href={addJwtParam(`/deployment-servers/${e.id}/edit`)}>Edit</a></td>
											<td>
												<Form action={`/deployment-servers/${e.id}/delete`}>
													<FormInput>
														<button type="submit" style={{ float: 'unset' }}>Delete</button>
													</FormInput>
												</Form>
											</td>
										</tr>
									)
								})}
							</>
						}
					</tbody>
				</table>
			}
		</>
	)
}

render(<App />)