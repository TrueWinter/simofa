import { Fragment, useEffect, useState } from 'react'
import render from '../_common/_render'
import Skeleton from '../_common/Skeleton'
import Form, { FormInput } from '../_common/Form'
import Git from '../_common/Git'
import addJwtParam from '../_common/_auth'
import Modal from '../_common/Modal'

function App() {
	const [git, setGit] = useState([] as Git[]);
	const [error, setError] = useState('');
	const [loading, setLoading] = useState(true);
	const [deleteModalData, setDeleteModalData] = useState(0);

	useEffect(() => {
		fetch(addJwtParam('/api/git')).then(d => {
			d.json().then(s => {
				if (!s.success) {
					return setError(s.error || 'An error occurred');
				}
				setGit(s.git);
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
							<td>Username</td>
							<td>Edit</td>
							<td>Delete</td>
						</tr>
					</thead>
					<tbody>
						{loading ?
							new Array(2).fill(0).map((e, i) => <Fragment key={i}>
								<tr>
									{new Array(4).fill(0).map((e, i) => <td key={i}><Skeleton/></td>)}
								</tr>
							</Fragment>) :
							<>
								{git.map(e => {
									return (
										<tr key={e.id}>
											<td>{e.id}</td>
											<td>{e.username}</td>
											<td><a href={addJwtParam(`/git/${e.id}/edit`)}>Edit</a></td>
											<td>
												<FormInput>
													<button style={{ float: 'unset' }} onClick={() => setDeleteModalData(e.id)}>Delete</button>
												</FormInput>
											</td>
										</tr>
									)
								})}
							</>
						}
					</tbody>
				</table>
			}

			{!!deleteModalData && <Modal title="Delete Git Credential" close={() => setDeleteModalData(0)}>
					<p>Are you sure you want to delete Git credential with ID {deleteModalData}?</p>
					<Form action={`/git/${deleteModalData}/delete`}>
						<FormInput>
							<button type="submit">Delete</button>
						</FormInput>
					</Form>
				</Modal>
			}
		</>
	)
}

render(<App />)