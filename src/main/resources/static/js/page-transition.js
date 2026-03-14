(function () {
    const transitionDuration = 700;
    const eligibleProtocols = ["http:", "https:"];
    const routeMessages = [
        { match: /^\/admin(\/|$)/, title: "Admin Portal", subtitle: "Entering the control room", theme: "admin" },
        { match: /^\/shop(\/|$)/, title: "Opening Shop", subtitle: "Curating your next flavor find", theme: "shop" },
        { match: /^\/product\/.+/, title: "Opening Product", subtitle: "Loading the details behind the flavor", theme: "product" },
        { match: /^\/wishlist(\/|$)/, title: "Opening Wishlist", subtitle: "Bringing back your saved favorites", theme: "wishlist" },
        { match: /^\/orders(\/|$)/, title: "Opening Orders", subtitle: "Gathering your recent spice journeys", theme: "orders" },
        { match: /^\/cart(\/|$)/, title: "Opening Cart", subtitle: "Preparing your checkout essentials", theme: "cart" },
        { match: /^\/checkout(\/|$)/, title: "Opening Checkout", subtitle: "Getting everything ready to complete your order", theme: "checkout" },
        { match: /^\/subscriptions(\/|$)/, title: "Opening Memberships", subtitle: "Unlocking your member experience", theme: "subscriptions" },
        { match: /^\/support(\/|$)/, title: "Opening Support", subtitle: "Connecting you with help and priority care", theme: "support" },
        { match: /^\/profile(\/|$)/, title: "Opening Profile", subtitle: "Loading your personal spice hub", theme: "profile" },
        { match: /^\/dashboard(\/|$)|^\/$|^\/home(\/|$)/, title: "Opening Dashboard", subtitle: "Welcome back to I-SPICE", theme: "dashboard" },
        { match: /^\/login(\/|$)/, title: "Opening Login", subtitle: "Securing your return to I-SPICE", theme: "auth" },
        { match: /^\/register(\/|$)/, title: "Creating Account", subtitle: "Setting up your I-SPICE journey", theme: "auth" }
    ];
    let overlay;
    let isTransitioning = false;

    function createOverlay() {
        if (overlay) {
            return overlay;
        }

        overlay = document.createElement("div");
        overlay.className = "page-transition-overlay is-entering";
        overlay.setAttribute("aria-hidden", "true");
        overlay.innerHTML = [
            '<div class="page-transition-card">',
            '  <div class="page-transition-logo-shell">',
            '    <div class="page-transition-ring"></div>',
            '    <img class="page-transition-logo" src="/I-SPICE/logo/logo.png" alt="I-SPICE logo">',
            '  </div>',
            '  <div class="page-transition-title">I-SPICE</div>',
            '  <div class="page-transition-subtitle">Preparing your next flavor-filled experience</div>',
            '  <div class="page-transition-progress"></div>',
            "</div>"
        ].join("");

        document.body.appendChild(overlay);
        requestAnimationFrame(() => {
            document.body.classList.add("page-enter-active");
            overlay.classList.add("is-visible");
        });

        window.setTimeout(() => {
            overlay.classList.remove("is-visible", "is-entering");
            document.body.classList.remove("page-enter-active");
        }, 540);

        return overlay;
    }

    function shouldHandleLink(anchor) {
        if (!anchor || isTransitioning) {
            return false;
        }

        if (anchor.target && anchor.target !== "_self") {
            return false;
        }

        if (anchor.hasAttribute("download") || anchor.getAttribute("rel") === "external") {
            return false;
        }

        const href = anchor.getAttribute("href");
        if (!href || href.startsWith("#") || href.startsWith("javascript:") || href.startsWith("mailto:") || href.startsWith("tel:")) {
            return false;
        }

        const url = new URL(anchor.href, window.location.href);
        if (!eligibleProtocols.includes(url.protocol) || url.origin !== window.location.origin) {
            return false;
        }

        if (url.href === window.location.href) {
            return false;
        }

        return true;
    }

    function getTransitionCopy(urlString) {
        const url = new URL(urlString, window.location.href);
        const pathname = url.pathname;

        for (const route of routeMessages) {
            if (route.match.test(pathname)) {
                return route;
            }
        }

        const fallbackTitle = pathname
            .split("/")
            .filter(Boolean)
            .pop();

        if (fallbackTitle) {
            const friendly = fallbackTitle
                .replace(/[-_]+/g, " ")
                .replace(/\b\w/g, (char) => char.toUpperCase());
            return {
                title: "Opening " + friendly,
                subtitle: "Preparing your next I-SPICE experience",
                theme: "default"
            };
        }

        return {
            title: "I-SPICE",
            subtitle: "Preparing your next flavor-filled experience",
            theme: "default"
        };
    }

    function updateOverlayCopy(url) {
        if (!overlay) {
            return;
        }

        const copy = getTransitionCopy(url);
        const title = overlay.querySelector(".page-transition-title");
        const subtitle = overlay.querySelector(".page-transition-subtitle");

        if (title) {
            title.textContent = copy.title;
        }
        if (subtitle) {
            subtitle.textContent = copy.subtitle;
        }

        overlay.dataset.theme = copy.theme || "default";
    }

    function extractInlineNavigationTarget(element) {
        if (!element) {
            return null;
        }

        const clickable = element.closest("[onclick]");
        if (!clickable) {
            return null;
        }

        const onclick = clickable.getAttribute("onclick") || "";
        const match = onclick.match(/(?:window\.)?location\.href\s*=\s*['"]([^'"]+)['"]/)
            || onclick.match(/location\.assign\(\s*['"]([^'"]+)['"]\s*\)/)
            || onclick.match(/window\.location\s*=\s*['"]([^'"]+)['"]/);

        if (!match || !match[1]) {
            return null;
        }

        const url = new URL(match[1], window.location.href);
        if (!eligibleProtocols.includes(url.protocol) || url.origin !== window.location.origin || url.href === window.location.href) {
            return null;
        }

        return {
            element: clickable,
            href: url.href
        };
    }

    function navigateWithTransition(url) {
        if (isTransitioning) {
            return;
        }

        isTransitioning = true;
        createOverlay();
        updateOverlayCopy(url);
        overlay.classList.add("is-visible", "is-leaving");
        document.body.classList.add("page-is-transitioning");

        window.setTimeout(() => {
            window.location.href = url;
        }, transitionDuration);
    }

    function bindLinks() {
        document.addEventListener("click", (event) => {
            const anchor = event.target.closest("a[href]");
            if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0) {
                return;
            }

            if (shouldHandleLink(anchor)) {
                event.preventDefault();
                navigateWithTransition(anchor.href);
                return;
            }

            const inlineTarget = extractInlineNavigationTarget(event.target);
            if (!inlineTarget) {
                return;
            }

            if (event.target.closest("button, form")) {
                return;
            }

            event.preventDefault();
            event.stopPropagation();
            navigateWithTransition(inlineTarget.href);
        });
    }

    function bindHistoryFallback() {
        window.addEventListener("pageshow", () => {
            isTransitioning = false;
            document.body.classList.remove("page-is-transitioning");
            if (overlay) {
                overlay.classList.remove("is-visible", "is-leaving");
            }
        });
    }

    document.addEventListener("DOMContentLoaded", () => {
        createOverlay();
        bindLinks();
        bindHistoryFallback();
    });
})();
