import css from '../../../../css/react/docker/StatusIndicator.module.css'

interface StatusIndicatorProps {
	state: string
}

export default function StatusIndicator({
	state
}) {
	return (
		<span className={[css.status, state === 'running' ? css.running : css['not-running']].join(' ')}></span>
	)
}