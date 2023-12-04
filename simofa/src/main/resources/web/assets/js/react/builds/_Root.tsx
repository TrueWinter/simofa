import { Link as RRLink, Outlet, useLocation } from 'react-router-dom'
import css from '../../../css/react/builds/Root.module.css'

function Link({
	to,
	children
}) {
	const page = useLocation();

	// {...props} results in error "TS2630: Cannot assign to '_extends' because it is a function.", so not using that here
	return (
		<RRLink className={[css['nav-link'], page.pathname === to && css.active].filter(e=>e).join(' ')} to={to}>{children}</RRLink>
	)
}

export default function Root() {
	return (
		<>
			<div className={css.sidebar}>
				<h1 className={css.heading}>Builds</h1>
				<Link to="/">Images</Link>
				<Link to="/containers">Containers</Link>
				<Link to="/queue">Queue</Link>
			</div>
			<div className={css.content}>
				<Outlet />
			</div>
		</>
	)
}