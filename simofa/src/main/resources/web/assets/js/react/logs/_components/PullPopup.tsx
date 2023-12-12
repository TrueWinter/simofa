import render from '../../_common/_render'
import { KeyboardEvent, useState } from 'react'
import CloseButton from '../../_common/CloseButton'
import Form, { FormInput } from '../../_common/Form'
import type { Website } from '../../../java';
import { toCamelCase } from '../../../util'

import css from '../../../../css/react/logs/LogsApp.module.css'

interface PullPopupProps {
	setPullPopupShown: Function
	website: Website
}

export default function PullPopup({
	setPullPopupShown,
	website
}: PullPopupProps) {
	const [pullUrlError, setPullUrlError] = useState('');
	const [pullFormHasError, setPullFormHasError] = useState(false);

	function getInitValue(key: string): string | number {
		if (website) {
			return website[key] || website[toCamelCase(key)];
		}
	}

	function validatePullURL(e: KeyboardEvent<HTMLInputElement>) {
		try {
			new URL((e.target as HTMLInputElement).value)
			setPullUrlError('');
		} catch(_) {
			setPullUrlError('Invalid URL');
		}
	}

	return (
		<>
		<div className={css.overlay}></div>
		<div className={css.popup}>
			<CloseButton onClick={() => setPullPopupShown(false)} />
			<h1>Pull from git repository</h1>
			<Form action={`/websites/${website.id}/pull`} setHasErrors={setPullFormHasError}>
				<FormInput label="Repository URL" error={pullUrlError}>
					<input type="url" name="repository" defaultValue={getInitValue('git_url')} maxLength={255} onKeyUp={validatePullURL} required={true} />
				</FormInput>
				<FormInput label="Branch">
					<input type="text" name="branch" defaultValue={getInitValue('git_branch')} maxLength={40} required={true} />
				</FormInput>
				<FormInput>
					<>
						<input type="checkbox" name="noCache" style={{
							width: 'unset'
						}} /> No cache
					</>
				</FormInput>
				<FormInput>
					<button type="submit" disabled={!!pullUrlError || pullFormHasError}>Pull</button>
				</FormInput>
			</Form>
		</div>
		</>
	)
}