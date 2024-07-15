import { MouseEventHandler } from 'react'
import css from '../../../css/react/_common/CloseButton.module.css'

interface CloseButtonProps {
	onClick: MouseEventHandler
}

export default function CloseButton({
	onClick
}: CloseButtonProps) {
	return (
		<span className={css.x} onClick={onClick}>x</span>
	)
}