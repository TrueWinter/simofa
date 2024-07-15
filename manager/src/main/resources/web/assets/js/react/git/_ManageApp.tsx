import { useState } from 'react'
import Form, { FormInput } from '../_common/Form'
import render from '../_common/_render'

function getInitValue(key: string): string | number {
	const data = document.getElementById('git-data')?.dataset.data;
	if (data) {
		return JSON.parse(data)[key];
	}
}

function isEditPage(): boolean {
	return !!document.getElementById('git-data');
}

function App() {
	const [formHasError, setFormHasError] = useState(false);

	return (
		<Form setHasErrors={setFormHasError}>
			<FormInput label="Username">
				<input type="text" name="username" defaultValue={getInitValue('username')} required={true} maxLength={40} />
			</FormInput>
			<FormInput label="Password">
				<input type="password" name="password" defaultValue={getInitValue('password')} required={true} maxLength={80} />
			</FormInput>
			<FormInput>
				<button type="submit" disabled={formHasError}>{isEditPage() ? 'Edit' : 'Add'} Git Credential</button>
			</FormInput>
		</Form>
	)
}

render(<App />)