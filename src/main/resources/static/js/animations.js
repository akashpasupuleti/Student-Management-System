/**
 * Animations and interactions for the application
 */

document.addEventListener('DOMContentLoaded', function() {
    // Add animation classes to elements
    animateElements();

    // Add hover effects to buttons
    setupButtonEffects();

    // Add form validation and animations
    setupFormAnimations();

    // Initialize password toggles
    initPasswordToggles();
});

/**
 * Add animation classes to elements
 */
function animateElements() {
    // Animate cards with staggered delay
    const cards = document.querySelectorAll('.card');
    cards.forEach((card, index) => {
        card.style.animationDelay = `${index * 0.1}s`;
    });

    // Animate headings
    const headings = document.querySelectorAll('h1, h2, h3');
    headings.forEach(heading => {
        heading.classList.add('animate-fade-in');
    });
}

/**
 * Setup button hover and click effects
 */
function setupButtonEffects() {
    const buttons = document.querySelectorAll('.btn, button');

    buttons.forEach(button => {
        // Add ripple effect on click
        button.addEventListener('click', function(e) {
            const rect = this.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;

            const ripple = document.createElement('span');
            ripple.classList.add('ripple');
            ripple.style.left = `${x}px`;
            ripple.style.top = `${y}px`;

            this.appendChild(ripple);

            setTimeout(() => {
                ripple.remove();
            }, 600);
        });
    });
}

/**
 * Setup form animations and validation
 */
function setupFormAnimations() {
    const forms = document.querySelectorAll('form');

    forms.forEach(form => {
        const inputs = form.querySelectorAll('input');

        // Add focus animations
        inputs.forEach(input => {
            input.addEventListener('focus', function() {
                this.parentElement.classList.add('input-focused');
            });

            input.addEventListener('blur', function() {
                if (!this.value) {
                    this.parentElement.classList.remove('input-focused');
                }
            });

            // If input has value on page load, add focused class
            if (input.value) {
                input.parentElement.classList.add('input-focused');
            }
        });

        // Add form submission animation
        form.addEventListener('submit', function(e) {
            const submitButton = this.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.classList.add('btn-loading');

                // Add loading spinner
                const spinner = document.createElement('span');
                spinner.classList.add('spinner');
                submitButton.appendChild(spinner);
            }
        });
    });
}

/**
 * Show notification message
 * @param {string} message - The message to display
 * @param {string} type - The type of message (success, error, info)
 * @param {number} duration - How long to show the message in ms
 */
function showNotification(message, type = 'info', duration = 3000) {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;

    // Add to document
    document.body.appendChild(notification);

    // Animate in
    setTimeout(() => {
        notification.classList.add('show');
    }, 10);

    // Animate out and remove
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => {
            notification.remove();
        }, 300);
    }, duration);
}

/**
 * Add floating animation to element
 * @param {HTMLElement} element - The element to animate
 */
function addFloatingAnimation(element) {
    element.classList.add('floating');
}

/**
 * Add pulse animation to element
 * @param {HTMLElement} element - The element to animate
 */
function addPulseAnimation(element) {
    element.classList.add('pulse');
}

/**
 * Add shake animation to element
 * @param {HTMLElement} element - The element to animate
 * @param {number} duration - How long to shake in ms
 */
function addShakeAnimation(element, duration = 500) {
    element.classList.add('shake');
    setTimeout(() => {
        element.classList.remove('shake');
    }, duration);
}

/**
 * Toggle password visibility with animation
 * @param {string} inputId - The ID of the password input field
 * @param {HTMLElement} toggleElement - The toggle button element
 */
function togglePasswordVisibility(inputId, toggleElement) {
    const passwordInput = document.getElementById(inputId);
    const icon = toggleElement.querySelector('i');

    // Don't create ripple effect to avoid movement

    if (passwordInput.type === 'password') {
        // Show password
        passwordInput.type = 'text';

        // Animate the transition
        icon.style.animation = 'togglePop 0.2s forwards';

        // Use setTimeout to change the icon class after a slight delay
        // This creates a smoother transition
        setTimeout(() => {
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
            toggleElement.classList.add('active');
        }, 100);

        setTimeout(() => {
            icon.style.animation = '';
        }, 200);
    } else {
        // Hide password
        passwordInput.type = 'password';

        // Animate the transition
        icon.style.animation = 'togglePop 0.2s forwards';

        // Use setTimeout to change the icon class after a slight delay
        setTimeout(() => {
            icon.classList.remove('fa-eye-slash');
            icon.classList.add('fa-eye');
            toggleElement.classList.remove('active');
        }, 100);

        setTimeout(() => {
            icon.style.animation = '';
        }, 200);
    }
}

/**
 * Initialize password toggles for all password fields
 */
function initPasswordToggles() {
    const passwordFields = document.querySelectorAll('input[type="password"]');

    passwordFields.forEach(field => {
        // Skip if already has a toggle
        if (field.parentElement.querySelector('.password-toggle')) return;

        // Create toggle button with animation
        const toggle = document.createElement('span');
        toggle.className = 'password-toggle animate-fade-in';
        toggle.innerHTML = '<i class="fas fa-eye"></i>';
        toggle.style.opacity = '0';

        // Add click event
        toggle.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            togglePasswordVisibility(field.id, this);
        });

        // Add ID to field if it doesn't have one
        if (!field.id) {
            field.id = 'pwd-' + Math.random().toString(36).substr(2, 9);
        }

        // Add toggle to parent
        field.parentElement.appendChild(toggle);

        // Position the toggle correctly
        toggle.style.right = '18px';

        // Animate the toggle appearance with a slight delay
        setTimeout(() => {
            toggle.style.opacity = '1';
        }, 300);

        // Add focus/blur events to change color without moving
        field.addEventListener('focus', function() {
            toggle.style.opacity = '1';
            toggle.style.color = 'var(--accent-color)';
        });

        field.addEventListener('blur', function() {
            if (field.type === 'password') {
                toggle.style.color = 'rgba(255, 255, 255, 0.7)';
            }
        });
    });
}
