import { Fragment, useEffect, useState } from 'react'
import render from '../_common/_render'
import Skeleton from '../_common/Skeleton'
import Website, { WebsitesResponse } from '../_common/Website'
import DeploymentServer, { DeploymentServersResponse } from '../_common/DeploymentServer'
import Form, { FormInput } from '../_common/Form'
import addJwtParam from '../_common/_auth'

function App() {
	const [websites, setWebsites] = useState([] as Website[]);
	const [servers, setServers] = useState(new Map() as Map<number, DeploymentServer>);
	const [error, setError] = useState('');
	const [loading, setLoading] = useState(true);

	useEffect(() => {
		Promise.all([
			fetch(addJwtParam('/api/deployment-servers')),
			fetch(addJwtParam('/api/websites'))
		]).then(d => {
			d[0].json().then((s: DeploymentServersResponse) => {
				if (!s.success) {
					return setError(s.error || 'An error occurred');
				}
				setServers(new Map(s.servers.map(v => [v.id, v])));
			});

			d[1].json().then((s: WebsitesResponse) => {
				if (!s.success) {
					setError(s.error || 'An error occurred');
				}
				setWebsites(s.websites);
			});
		}).catch(e => {
			console.error(e);
			setError(`An error occurred while fetching data: ${e}`);
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
							<td>Server</td>
							<td>Edit</td>
							<td>Logs</td>
							<td>Delete</td>
						</tr>
					</thead>
					<tbody>
						{loading ?
							new Array(2).fill(0).map((e, i) => <Fragment key={i}>
								<tr>
									{new Array(6).fill(0).map((e, i) => <td key={i}><Skeleton /></td>)}
								</tr>
							</Fragment>) :
							<>
								{websites.map(e => {
									return (
										<tr key={e.id}>
											<td>{e.id}</td>
											<td>{e.name}</td>
											<td>[{e.deploymentServer}] {servers.get(e.deploymentServer)?.name}</td>
											<td><a href={addJwtParam(`/websites/${e.id}/edit`)}>Edit</a></td>
											<td><a href={addJwtParam(`/websites/${e.id}/logs`)}>Logs</a></td>
											<td>
												<Form action={`/websites/${e.id}/delete`}>
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