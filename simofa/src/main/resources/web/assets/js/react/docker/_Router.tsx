import render from '../_common/_render'
import {
	createBrowserRouter,
	RouterProvider
} from 'react-router-dom'
import Root from './_Root'
import { Suspense, lazy } from 'react'

function Loading() {
	return (
		<>Loading...</>
	)
}

const Images = lazy(() => import('./_pages/Images'))
const Containers = lazy(() => import('./_pages/Containers'))
const Queue = lazy(() => import('./../logs/_components/Builds'))

const router = createBrowserRouter([
	{
		element: <Root />,
		children: [
			{
				path: '/',
				element: <Suspense fallback={<Loading />}>
					<Images />
				</Suspense>
			},
			{
				path: '/containers',
				element: <Suspense fallback={<Loading />}>
					<Containers />
				</Suspense>
			},
			{
				path: '/queue',
				element: <Suspense fallback={<Loading />}>
					<Queue />
				</Suspense>
			}
		]
	}
], {
	basename: '/docker/'
})

render(<RouterProvider router={router} />)