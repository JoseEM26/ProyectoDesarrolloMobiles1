document.addEventListener('DOMContentLoaded', () => {
    const loading = document.getElementById('loadingOverlay');

    // Mostrar loading
    window.showLoading = () => loading.style.display = 'flex';
    window.hideLoading = () => loading.style.display = 'none';

    // Simular carga
    setTimeout(() => {
        hideLoading();
    }, 1000);
});