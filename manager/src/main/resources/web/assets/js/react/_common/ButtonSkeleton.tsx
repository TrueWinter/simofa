import Skeleton from './Skeleton'

export default function ButtonSkeleton() {
	return (
		<Skeleton width="100px" height="calc(1em + (2 * 8px))" style={{
			float: 'right'
		}} />
	)
}