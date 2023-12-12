import render from '../_common/_render'
import { useState } from 'react'
import PullPopup from './_components/PullPopup'

import css from '../../../css/react/logs/LogsApp.module.css'
import Builds from './_components/Builds';
import type { Website } from '../../java';

export default function LogsApp() {
	const website: Website = JSON.parse(document.getElementById('data').dataset.website);
	const [pullPopupShown, setPullPopupShown] = useState(false);

	return (
		<>
			<div className={css.link} onClick={() => setPullPopupShown(true)}>Pull from git repository</div>
			{pullPopupShown && <PullPopup website={website} setPullPopupShown={setPullPopupShown} />}

			<Builds website={website} />
		</>
	)
}

render(<LogsApp />)