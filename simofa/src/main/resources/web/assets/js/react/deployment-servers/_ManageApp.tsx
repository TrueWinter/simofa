import { KeyboardEvent, useState } from 'react'
import Form, { FormInput } from '../_common/Form'
import render from '../_common/_render'

function getInitValue(key: string): string | number {
	const data = document.getElementById('server-data')?.dataset.data;
	if (data) {
		return JSON.parse(data)[key];
	}
}

function isEditPage(): boolean {
	return !!document.getElementById('server-data');
}

function App() {
	const [formHasError, setFormHasError] = useState(false);
	const [urlError, setUrlError] = useState('');

	function validateUrl(e: KeyboardEvent<HTMLInputElement>) {
		try {
			new URL((e.target as HTMLInputElement).value)
			setUrlError('');
		} catch(e) {
			setUrlError('Invalid URL');
		}
	}

	return (
		<Form setHasErrors={setFormHasError}>
			<FormInput label="Name">
				<input type="text" name="name" defaultValue={getInitValue('name')} required={true} maxLength={20} />
			</FormInput>
			<FormInput label="URL" error={urlError}>
				<input type="text" name="url" onKeyUp={validateUrl} defaultValue={getInitValue('url')} required={true} maxLength={256} />
			</FormInput>
			<FormInput label="Key">
				<input type="text" name="key" defaultValue={getInitValue('key')} required={true} maxLength={60} />
			</FormInput>
			<FormInput>
				<button type="submit" disabled={!!urlError || formHasError}>{isEditPage() ? 'Edit' : 'Add'} Deployment Server</button>
			</FormInput>
		</Form>
	)
}

render(<App />)