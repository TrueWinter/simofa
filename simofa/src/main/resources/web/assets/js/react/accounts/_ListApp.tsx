import { Fragment, useEffect, useState } from 'react'
import render from '../_common/_render'
import Account, { AccountsResponse } from '../_common/Account'
import Skeleton from '../_common/Skeleton';
import Form, { FormInput } from '../_common/Form';
import addJwtParam from '../_common/_auth';
import Modal from '../_common/Modal';

function App() {
	const [accounts, setAccounts] = useState([] as Account[]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState('');
	const [deleteModalData, setDeleteModalData] = useState(0);

	useEffect(() => {
		fetch(addJwtParam('/api/accounts')).then(accounts => {
			accounts.json().then((a: AccountsResponse) => {
				if (!a.success) {
					return setError(a.error || 'An error occurred');
				}

				setAccounts(a.accounts);
			}).catch(e => {
				console.error(e);
				setError(`An error occurred while fetching deployment servers: ${e}`);
			}).finally(() => {
				setLoading(false);
			});
		})
	}, [])

	return (
		<>
			<table>
				<thead>
					<tr>
						<th>ID</th>
						<th>Username</th>
						<th>Edit</th>
						<th>Delete</th>
					</tr>
				</thead>
				<tbody>
					{error ? <div className="error">{error}</div> :
						<>
							{loading ? new Array(2).fill(0).map((e, i) => <tr key={i}>
								{new Array(4).fill(0).map((e, i) => <td key={i}><Skeleton /></td>)}
							</tr>) :
								<>
									{accounts.map(a => <tr key={a.id}>
										<td>{a.id}</td>
										<td>{a.username}</td>
										<td><a href={addJwtParam(`/accounts/${a.id}/edit`)}>Edit</a></td>
										<td>
											<FormInput>
												<button style={{ float: 'unset' }} onClick={() => setDeleteModalData(a.id)}>Delete</button>
											</FormInput>
										</td>
									</tr>)}
								</>
							}
						</>
					}
				</tbody>
			</table>

			{!!deleteModalData && <Modal title="Delete Account" close={() => setDeleteModalData(0)}>
					<p>Are you sure you want to delete account with ID {deleteModalData}?</p>
					<Form action={`/accounts/${deleteModalData}/delete`}>
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