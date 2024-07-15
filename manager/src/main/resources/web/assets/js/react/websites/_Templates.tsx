import { MouseEvent, useEffect, useState } from 'react'
import Skeleton from '../_common/Skeleton'
import Form, { FormInput } from '../_common/Form'

import css from '../../../css/react/websites/Add.module.css'
import type { Template } from '../../java'
import addJwtParam from '../_common/_auth'
import { shortUuid } from '../_common/shortUuid'

interface TemplatesProps {
	loadTemplate: Function
	current: string
	currentName: string
}

export default function Templates({
	loadTemplate,
	current,
	currentName
}: TemplatesProps) {
	const [templates, setTemplates] = useState([] as Template[])
	const [error, setError] = useState('')
	const [loading, setLoading] = useState(true)
	const [saving, setSaving] = useState(false)
	const [csrfToken, setCsrfToken] = useState('')

	useEffect(() => {
		const csrfMeta: HTMLElement | null = document.querySelector('meta[name="csrf"]');
		if (csrfMeta instanceof HTMLMetaElement) {
			setCsrfToken(csrfMeta.content);
		}

		loadTemplates()
	}, [])

	function loadTemplates() {
		setLoading(true);

		fetch(addJwtParam('/api/templates'))
			.then(d => d.json())
			.then(d => setTemplates(d.templates))
			.catch(e => setError(`An error occurred: ${e}`))
			.finally(() => setLoading(false))
	}

	function _loadTemplate(e) {
		loadTemplate((e.target as HTMLSpanElement).dataset.template);
	}

	function saveCurrent() {
		setSaving(true);
		fetch(addJwtParam('/api/templates/add'), {
			method: 'POST',
			body: new URLSearchParams({
				csrf: csrfToken,
				name: currentName,
				template: current
			})
		}).then(d => d.json())
		.then(d => {
			if (!d.success) {
				setError('An error occurred while saving');
			}
		}).catch(() => {
			setError('An error occurred while saving');
		}).finally(() => {
			setSaving(false);
			loadTemplates();
		})
	}

	function deleteTemplate(id: string) {
		setLoading(true);
		fetch(addJwtParam(`/api/templates/${id}/delete`), {
			method: 'POST',
			body: new URLSearchParams({
				csrf: csrfToken
			})
		}).then(d => d.json())
		.then(d => {
			console.log(d);
			if (!d.success) {
				setError('An error occurred while deleting');
			}
		}).catch(() => {
			setError('An error occurred while deleting');
		}).finally(() => {
			loadTemplates();
		})
	}

	return (
		<>
			<h2>Templates</h2>
			{saving ? <div className={css['template-link']}>Saving...</div> :
				<div className={css['template-link']} onClick={saveCurrent}>Save current form data as template</div>}
			{
				error ? <div className="error" style={{
					margin: 0
				}}>{error}</div> :
				<table>
					<thead>
						<tr>
							<td>ID</td>
							<td>Name</td>
							<td>Load</td>
							<td>Delete</td>
						</tr>
					</thead>
					<tbody>
						{loading ? new Array(2).fill(0).map(e => <tr>
								{new Array(4).fill(0).map(e => <td><Skeleton /></td>)}
							</tr>) :
							<>
							{
								templates.map(e => <tr key={e.id}>
									<td title={e.id}>{shortUuid(e.id)}</td>
									<td>{e.name}</td>
									<td><span className={css['template-link']} data-template={e.template} onClick={_loadTemplate}>Load</span></td>
									<td><button type="submit" onClick={() => deleteTemplate(e.id)}>Delete</button></td>
								</tr>)
							}
							</>}
					</tbody>
				</table>
			}
		</>
	)
}