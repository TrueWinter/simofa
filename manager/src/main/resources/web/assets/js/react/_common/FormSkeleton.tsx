import Skeleton from './Skeleton'

interface FormSkeletonProps {
	labelHeight?: string,
	labelWidth?: string,
	inputHeight?: string,
	counterWidth?: string,
	hideCounter?: boolean
}

export default function FormSkeleton({
	labelHeight = '16px',
	labelWidth = '72px',
	inputHeight = '32px',
	counterWidth = '42px',
	hideCounter = false
}: FormSkeletonProps) {
	return (
		<>
			<Skeleton height={labelHeight} width={labelWidth} />
			<Skeleton height={inputHeight} />
			{!hideCounter && <Skeleton height={labelHeight} width={counterWidth} style={{
				float: 'right'
			}} />}
		</>
	)
}