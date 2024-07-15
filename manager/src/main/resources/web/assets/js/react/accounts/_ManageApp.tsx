import { KeyboardEvent, useRef, useState } from 'react'
import Form, { FormInput } from '../_common/Form'
import render from '../_common/_render'

function getInitValue(key: string): string | number {
	const data = document.getElementById('account-data')?.dataset.data;
	if (data) {
		return JSON.parse(data)[key];
	}
}

function isEditPage(): boolean {
	return !!document.getElementById('account-data');
}

function App() {
	const passRef = useRef(null as HTMLInputElement);
	const passConfirmRef = useRef(null as HTMLInputElement);
	const [passError, setPassError] = useState('');
	const [hasLocalError, setHasLocalError] = useState(false);

	function validatePasswords(e: KeyboardEvent<HTMLInputElement>) {
		console.log('validate');
		if (passRef.current?.value !== passConfirmRef.current?.value) {
			setPassError('Passwords must match');
		} else {
			setPassError('');
		}
	}

	return (
		<Form setHasErrors={setHasLocalError}>
			<FormInput label="Username">
				<input type="text" name="username" defaultValue={getInitValue('username')} required={true} maxLength={20} />
			</FormInput>
			<FormInput label="Password">
				<input ref={passRef} onKeyUp={validatePasswords} type="password" name="password" placeholder={isEditPage() ? 'Leave blank to keep current password' : undefined} required={!isEditPage()} maxLength={72} />
			</FormInput>
			<FormInput label="Confirm password" error={passError}>
				<input ref={passConfirmRef} onKeyUp={validatePasswords} type="password" name="confirm_password" placeholder={isEditPage() ? 'Leave blank to keep current password' : undefined} required={!isEditPage()} maxLength={72} />
			</FormInput>
			<FormInput>
				<button type="submit" disabled={!!passError || hasLocalError}>{isEditPage() ? 'Edit' : 'Add'} Account</button>
			</FormInput>
		</Form>
	)
}

render(<App />)