import addJwtParam from "./react/_common/_auth";

function handleScreenSize() {
    if (document.querySelector('.navbar') === null) return;

    let navItems = document.getElementsByClassName('navbar-item');

    if (window.innerWidth > 600) {
        document.querySelector('.navbar').style.minHeight = null;

        for (var i = 0; i < navItems.length; i++) {
            navItems[i].classList.remove('navbar-hidden');
            navItems[i].classList.remove('navbar-shown');
        }

        return;
    }

    document.querySelector('.navbar').style.minHeight = `calc(1em + 16px + (2 * 8px))`
}

handleScreenSize();

window.addEventListener('resize', () => {
    handleScreenSize();
});

(function() {
    var navIsHidden = true;
    if (document.querySelector('.navbar-collapse')) {
        document.querySelector('.navbar-collapse').addEventListener('click', (e) => {
            e.preventDefault();
            let navItems = document.getElementsByClassName('navbar-item');
            if (navIsHidden) {
                for (var i = 0; i < navItems.length; i++) {
                    navItems[i].classList.remove('navbar-hidden')
                    navItems[i].classList.add('navbar-shown');
                }
                navIsHidden = false;
            } else {
                for (var i = 0; i < navItems.length; i++) {
                    navItems[i].classList.add('navbar-hidden')
                    navItems[i].classList.remove('navbar-shown');
                }
                navIsHidden = true;
            }
        });
    }

	if (location.hostname !== 'localhost' && location.protocol === 'http:') {
		console.warn('HTTPS is required for some parts of the Simofa dashboard to work');

		let navbar = document.getElementsByClassName('navbar');
		if (navbar.length > 0) {
			let alertDiv = document.createElement('div');
			alertDiv.style.backgroundColor = 'orangered';
			alertDiv.style.padding = '8px';
			alertDiv.style.borderRadius = '8px';
			alertDiv.style.margin = '8px';
			alertDiv.style.textAlign = 'center';
			alertDiv.innerText = 'HTTPS is required for some parts of the Simofa dashboard to work.';
			navbar[0].after(alertDiv);
		}
	}

	window.addEventListener('click', (e) => {
		let pageUrl = new URL(window.location.href);
		if (e.target instanceof HTMLAnchorElement && pageUrl.searchParams.has("jwt")) {
			e.target.href = addJwtParam(e.target.href);
		}
	});

	window.addEventListener('submit', (e) => {
		let pageUrl = new URL(window.location.href);
		if (e.target instanceof HTMLFormElement && pageUrl.searchParams.has("jwt")) {
			e.target.action = addJwtParam(e.target.action);
		}
	});
})();