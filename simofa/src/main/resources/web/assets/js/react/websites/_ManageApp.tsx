import { useState, useEffect, useRef, KeyboardEvent } from 'react'
import Form, { FormInput } from '../_common/Form'
import render from '../_common/_render'
import DeploymentServer from '../_common/DeploymentServer'
import FormSkeleton from '../_common/FormSkeleton'
import ButtonSkeleton from '../_common/ButtonSkeleton'
import DockerImage from '../_common/DockerImage'
import CloseButton from '../_common/CloseButton'
import Templates from './_Templates'
import { toCamelCase } from '../../util'
import Git from '../_common/Git'

import css from '../../../css/react/websites/Add.module.css'
import addJwtParam from '../_common/_auth'

interface AnyObject {
	[x: string]: any;
}

const DEFAULT_SCRIPT = '#!/bin/bash\nset -e\n';
function getInitValue(key: string): string | number {
	const data = document.getElementById('website-data')?.dataset.data;
	if (data) {
		let returnData: string | AnyObject = JSON.parse(data)[key] || JSON.parse(data)[toCamelCase(key)];
		// "TS2630: Cannot assign to '_typeof' because it is a function" strikes again
		if (returnData && JSON.stringify(returnData).startsWith('{') && !Array.isArray(returnData) && Object.prototype.hasOwnProperty.call(returnData, 'id')) {
			return (returnData as AnyObject).id;
		}

		return returnData as string;
	}
}

function isEditPage(): boolean {
	return !!document.getElementById('website-data');
}

function App() {
	const [servers, setServers] = useState([] as DeploymentServer[]);
	const [images, setImages] = useState([] as DockerImage[]);
	const [git, setGit] = useState([] as Git[]);
	const [error, setError] = useState('');
	const [memoryError, setMemoryError] = useState('');
	const [cpuError, setCPUError] = useState('');
	const [urlError, setUrlError] = useState('');
	const [formHasError, setFormHasError] = useState(false);
	const [templateLoaderVisible, setTemplateLoaderVisible] = useState(false);
	const [loading, setLoading] = useState(true);
	const deployToken = useRef(crypto.randomUUID());

	const nameRef = useRef(null);
	const refs = {
		memory: useRef(null),
		cpu: useRef(null),
		buildCommand: useRef(null),
		postBuildCommand: useRef(null),
		deploymentCommand: useRef(null),
		postDeploymentCommand: useRef(null),
		deploymentFailedCommand: useRef(null),
		dockerImage: useRef(null)
	};

	useEffect(() => {
		Promise.all([
			fetch(addJwtParam('/api/deployment-servers')),
			fetch(addJwtParam('/api/docker/images')),
			fetch(addJwtParam('/api/git'))
		]).then(data => {
		
			data[0].json().then(s => {
				if (!s.success) {
					setError(s.error || 'An error occurred');
				}
				setServers(s.servers);

				if (s.servers.length === 0) {
					setError('No deployment servers added yet');
				}
			});

			data[1].json().then(d => {
				setImages(d);

				if (d.length === 0) {
					setError('No Docker images available');
				}
			});

			data[2].json().then(d => {
				setGit(d.git);
			});
		}).catch(e => {
			console.error(e);
			setError(`An error occurred while fetching deployment servers: ${e}`);
		}).finally(() => {
			setLoading(false);

			// This page uses the crypto API which only works on localhost and HTTPS pages
			if (location.hostname !== 'localhost' && location.protocol === 'http:') {
				alert('HTTPS is required for some parts of the Simofa dashboard to work');
			}
		});
	}, [])

	function validateMemory(e: KeyboardEvent<HTMLInputElement>) {
		if (!(e.target as HTMLInputElement).value.match(/^[0-9]{0,6}$/)) {
			setMemoryError('Invalid amount');
		} else {
			setMemoryError('');
		}
	}

	function validateCPU(e: KeyboardEvent<HTMLInputElement>) {
		if (!(e.target as HTMLInputElement).value.match(/^[0-9]{0,2}(?:\.[0-9]{0,2})?$/)) {
			setCPUError('Invalid amount');
		} else {
			setCPUError('');
		}
	}

	function validateURL(e: KeyboardEvent<HTMLInputElement>) {
		try {
			new URL((e.target as HTMLInputElement).value);
			setUrlError('');
		} catch (_) {
			setUrlError('Invalid URL')
		}
	}

	function getCurrentInputValues(): string {
		let values = {};

		for (let ref in refs) {
			if (!refs[ref].current) continue;
			if (!refs[ref].current.value) continue;
			values[ref] = refs[ref].current.value;
		}

		return JSON.stringify(values);
	}

	function loadTemplate(t: string) {
		let template = JSON.parse(t);

		for (let v in template) {
			if (!refs[v]) continue;
			if (!refs[v].current) continue;
			refs[v].current.value = template[v];
		}

		setTemplateLoaderVisible(false);
	}

	return (
		<>
			{error ? <div className="error">{error}</div> : <>
				{loading ? <div className={css.skeletons}>
					{['32px', '64px', '64px', '64px', '64px'].map((e, i) => <div key={i}>
						<FormSkeleton inputHeight={e} />
					</div>)}
					<ButtonSkeleton />
				</div> :
				<>
					<div className={css['template-link']} onClick={() => setTemplateLoaderVisible(true)}>Load template</div>
					{
						templateLoaderVisible && <>
							<div className={css.overlay}></div>
							<div className={css.popup}>
								<CloseButton onClick={() => setTemplateLoaderVisible(false)} />
								<Templates current={getCurrentInputValues()} currentName={nameRef.current?.value} loadTemplate={loadTemplate} />
							</div>
						</>
					}
					<Form setHasErrors={setFormHasError}>
						<FormInput label="Website name">
							<input type="text" ref={nameRef} name="name" defaultValue={getInitValue('name')} required={true} maxLength={40}/>
						</FormInput>
						<FormInput label="Docker Image">
							<select name="docker_image" required={true} ref={refs.dockerImage}>
								<option disabled={true} selected={true}>Select a server</option>
								{images.map(e => <option value={e.name} selected={getInitValue('docker_image') === e.name}>{e.name} ({e.size})</option>)}
							</select>
						</FormInput>
						<FormInput label="Memory (MB)" styles={{
							marginTop: '16px'
						}} error={memoryError}>
							<input type="number" ref={refs.memory} onKeyUp={validateMemory} step="64" name="memory" defaultValue={getInitValue('memory') || '256'} required={true} maxLength={6}/>
						</FormInput>
						<FormInput label="CPU Cores" error={cpuError}>
							<input type="number" ref={refs.cpu} onKeyUp={validateCPU} step="0.05" name="cpu" defaultValue={getInitValue('cpu') || '0.5'} required={true} maxLength={5}/>
						</FormInput>
						<FormInput label="Git URL" error={urlError}>
							<input type="text" name="git_url" onKeyUp={validateURL} defaultValue={getInitValue('git_url')} maxLength={255} required={true} />
						</FormInput>
						<FormInput label="Git Branch">
							<input type="text" name="git_branch" defaultValue={getInitValue('git_branch')} maxLength={40} required={true} />
						</FormInput>
						<FormInput label="Git Credential">
							<select name="git_credential" required={true} ref={refs.dockerImage}>
								<option selected={true} value="anonymous">anonymous</option>
								{git.map(e => <option value={e.id} selected={getInitValue('git_credential') === e.id}>[{e.id}] {e.username}</option>)}
							</select>
						</FormInput>
						<FormInput label="Build Command" styles={{
							marginTop: '16px'
						}}>
							<textarea name="build_command" ref={refs.buildCommand} defaultValue={getInitValue('build_command') || DEFAULT_SCRIPT} required={true} maxLength={512} rows={8}></textarea>
						</FormInput>
						<FormInput label="Deployment Command">
							<textarea name="deployment_command" ref={refs.deploymentCommand} defaultValue={getInitValue('deployment_command')  || DEFAULT_SCRIPT} required={true} maxLength={512} rows={8}></textarea>
						</FormInput>
						<FormInput label="Deployment Failed Command">
							<textarea name="deployment_failed_command" ref={refs.deploymentFailedCommand} defaultValue={getInitValue('deployment_failed_command')  || DEFAULT_SCRIPT} required={true} maxLength={512} rows={8}></textarea>
						</FormInput>
						<FormInput label="Deployment Server">
							<select name="deployment_server" required={true}>
								<option disabled={true} selected={true}>Select a server</option>
								{servers.map(e => <option value={e.id} selected={getInitValue('deployment_server') === e.id}>[{e.id}] {e.name}</option>)}
							</select>
						</FormInput>
						<FormInput label="Deploy Token" styles={{
							marginTop: '16px'
						}}>
							<input type="text" name="deploy_token" defaultValue={getInitValue('deploy_token') || deployToken.current} readOnly={true} required={true} maxLength={36} />
						</FormInput>
						<FormInput>
							<button type="submit" disabled={formHasError}>{isEditPage() ? 'Edit' : 'Add'} Website</button>
						</FormInput>
					</Form>
				</>}
			</>}
		</>
	)
}

render(<App />)