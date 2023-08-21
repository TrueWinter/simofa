export default function addJwtParam(url: string): string {
	let pageUrl = new URL(window.location.href);
	if (!pageUrl.searchParams.has("jwt")) return url;

	let u = new URL(url.startsWith('/') ? `${location.protocol}//${location.host}${url}` : url);
	u.searchParams.set("jwt", pageUrl.searchParams.get("jwt"));

	let r = new RegExp(`^${location.protocol}//${location.host}`);
	return u.href.replace(r, '');
}