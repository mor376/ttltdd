document.addEventListener('DOMContentLoaded', function () {
    var app = document.querySelector('.app');
    var collapseButton = document.querySelector('[data-sidebar-collapse]');
    var searchInput = document.querySelector('.quick-search input');
    var accountMenu = document.querySelector('.account-menu');

    if (app && collapseButton) {
        collapseButton.addEventListener('click', function () {
            var collapsed = app.classList.toggle('sidebar-collapsed');
            collapseButton.setAttribute('aria-label', collapsed ? 'Mở rộng menu' : 'Thu gọn menu');
            collapseButton.setAttribute('title', collapsed ? 'Mở rộng menu' : 'Thu gọn menu');
        });
    }

    document.addEventListener('keydown', function (event) {
        if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k' && searchInput) {
            event.preventDefault();
            searchInput.focus();
        }
        if (event.key === 'Escape') {
            if (accountMenu) accountMenu.open = false;
            if (app) app.classList.remove('sidebar-open');
        }
    });
});
