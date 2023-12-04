import { ReactNode } from 'react';
import css from '../../../css/react/_common/Modal.module.css';

interface ModalProps {
	title: string
	close: Function
	children: ReactNode
}

export default function Modal({ title, close, children }: ModalProps) {
	return (
		<>
			<div className={css.overlay}></div>
			<div className={css.modal}>
				<div className={css.heading}>
					<h1 className={css.title}>{title}</h1>
					<span className={css.close} onClick={() => close()}>x</span>
				</div>
				{children}
			</div>
		</>
	)
}