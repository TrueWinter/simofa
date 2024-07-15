import { createRoot } from 'react-dom/client';

export default function render(jsx: React.JSX.Element, id: string = 'app') {
	const root = createRoot(document.getElementById(id));
	root.render(jsx);
}