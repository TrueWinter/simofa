import { CSSProperties } from 'react'
import S from 'react-loading-skeleton'
import { SkeletonProps } from 'react-loading-skeleton'

import c from 'react-loading-skeleton/dist/skeleton.css'

interface Props {
	height?: string
	width?: string
	style?: CSSProperties
	className?: string
}

export default function Skeleton({
	height = '24px',
	width = '100%',
	style: s = {},
	className = ''
}: Props) {
	// Using the spread operator in the style prop
	// resulted in an error, so doing it this way.
	s.display = 'inline-block';

	return (
		<S className={[className, c['react-loading-skeleton']].filter(e => e).join(' ')} baseColor="#444" highlightColor="#666" height={height} width={width} style={s} />
	)
}