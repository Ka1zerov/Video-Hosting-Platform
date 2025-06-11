function togglePasswordVisibility() {
    const passwordInput = document.getElementById("password");
    const passwordIcon = document.querySelector(".toggle-password i");

    if (passwordInput.type === "password") {
        passwordInput.type = "text";
        passwordIcon.classList.remove("fa-eye");
        passwordIcon.classList.add("fa-eye-slash");
    } else {
        passwordInput.type = "password";
        passwordIcon.classList.remove("fa-eye-slash");
        passwordIcon.classList.add("fa-eye");
    }
}

const FRONTEND_BASE_URL = document.body.getAttribute("data-frontend-url");

function redirectToRegister() {
    window.location.href = `${FRONTEND_BASE_URL}/register`;
}

function redirectToForgotPassword() {
    window.location.href = `${FRONTEND_BASE_URL}/forgot-password`;
}

function dismissError(errorId) {
    const errorToast = document.getElementById(errorId);
    if (errorToast) {
        errorToast.style.animation = 'fadeOut 0.3s forwards';
        setTimeout(() => {
            errorToast.style.display = 'none';
        }, 300);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const verificationErrorToast = document.getElementById('verification-error-toast');
    const credentialsErrorToast = document.getElementById('credentials-error-toast');

    if (verificationErrorToast) {
        setTimeout(() => {
            dismissError('verification-error-toast');
        }, 7000);
    }

    if (credentialsErrorToast) {
        setTimeout(() => {
            dismissError('credentials-error-toast');
        }, 7000);
    }
});
