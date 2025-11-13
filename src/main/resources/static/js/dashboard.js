/**
 * Dashboard functionality for the LMS Portal
 */

document.addEventListener('DOMContentLoaded', function() {
    // Initialize sidebar toggle
    initSidebar();
    
    // Initialize dropdown menus
    initDropdowns();
    
    // Initialize charts if they exist
    initCharts();
    
    // Initialize data tables if they exist
    initDataTables();
    
    // Initialize theme toggle
    initThemeToggle();
    
    // Initialize notifications
    initNotifications();
});

/**
 * Initialize sidebar functionality
 */
function initSidebar() {
    const sidebarToggle = document.querySelector('.sidebar-toggle');
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('.main-content');
    
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', function() {
            sidebar.classList.toggle('show');
            mainContent.classList.toggle('expanded');
        });
        
        // Close sidebar when clicking outside on mobile
        document.addEventListener('click', function(e) {
            if (window.innerWidth < 992 && 
                !sidebar.contains(e.target) && 
                !sidebarToggle.contains(e.target) && 
                sidebar.classList.contains('show')) {
                sidebar.classList.remove('show');
                mainContent.classList.remove('expanded');
            }
        });
    }
    
    // Submenu toggle
    const submenuToggles = document.querySelectorAll('.submenu-toggle');
    submenuToggles.forEach(toggle => {
        toggle.addEventListener('click', function(e) {
            e.preventDefault();
            const parent = this.parentElement;
            const submenu = parent.querySelector('.sidebar-submenu');
            
            // Close other submenus
            document.querySelectorAll('.sidebar-submenu.show').forEach(menu => {
                if (menu !== submenu) {
                    menu.classList.remove('show');
                    menu.previousElementSibling.classList.remove('active');
                }
            });
            
            // Toggle current submenu
            submenu.classList.toggle('show');
            this.classList.toggle('active');
        });
    });
}

/**
 * Initialize dropdown menus
 */
function initDropdowns() {
    const dropdownToggles = document.querySelectorAll('.dropdown-toggle');
    
    dropdownToggles.forEach(toggle => {
        toggle.addEventListener('click', function(e) {
            e.preventDefault();
            const dropdown = this.nextElementSibling;
            
            // Close other dropdowns
            document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
                if (menu !== dropdown) {
                    menu.classList.remove('show');
                }
            });
            
            // Toggle current dropdown
            dropdown.classList.toggle('show');
        });
    });
    
    // Close dropdowns when clicking outside
    document.addEventListener('click', function(e) {
        if (!e.target.matches('.dropdown-toggle') && !e.target.closest('.dropdown-menu')) {
            document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
                menu.classList.remove('show');
            });
        }
    });
}

/**
 * Initialize charts using Chart.js if available
 */
function initCharts() {
    if (typeof Chart === 'undefined') return;
    
    // Sample chart initialization - replace with actual data
    const chartElements = document.querySelectorAll('.chart-container');
    
    chartElements.forEach(container => {
        const canvas = container.querySelector('canvas');
        const type = container.dataset.chartType || 'line';
        const chartId = canvas.id;
        
        // Sample data - replace with actual data from your backend
        let data, options;
        
        switch (type) {
            case 'line':
                data = {
                    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
                    datasets: [{
                        label: 'Student Progress',
                        data: [65, 59, 80, 81, 56, 55],
                        borderColor: '#4361ee',
                        tension: 0.1,
                        fill: false
                    }]
                };
                options = {
                    responsive: true,
                    maintainAspectRatio: false
                };
                break;
                
            case 'bar':
                data = {
                    labels: ['Math', 'Science', 'History', 'Language', 'Arts', 'PE'],
                    datasets: [{
                        label: 'Grades',
                        data: [85, 72, 90, 80, 95, 88],
                        backgroundColor: [
                            '#4361ee', '#10b981', '#f59e0b', 
                            '#ef4444', '#8b5cf6', '#ec4899'
                        ]
                    }]
                };
                options = {
                    responsive: true,
                    maintainAspectRatio: false
                };
                break;
                
            case 'pie':
                data = {
                    labels: ['A', 'B', 'C', 'D', 'F'],
                    datasets: [{
                        data: [30, 40, 15, 10, 5],
                        backgroundColor: [
                            '#10b981', '#4361ee', '#f59e0b', 
                            '#8b5cf6', '#ef4444'
                        ]
                    }]
                };
                options = {
                    responsive: true,
                    maintainAspectRatio: false
                };
                break;
                
            case 'doughnut':
                data = {
                    labels: ['Complete', 'In Progress', 'Not Started'],
                    datasets: [{
                        data: [65, 25, 10],
                        backgroundColor: [
                            '#10b981', '#f59e0b', '#ef4444'
                        ]
                    }]
                };
                options = {
                    responsive: true,
                    maintainAspectRatio: false
                };
                break;
        }
        
        // Create chart
        new Chart(canvas, {
            type: type,
            data: data,
            options: options
        });
    });
}

/**
 * Initialize data tables if DataTables library is available
 */
function initDataTables() {
    if (typeof $.fn.DataTable === 'undefined') return;
    
    $('.data-table').DataTable({
        responsive: true,
        pageLength: 10,
        lengthMenu: [5, 10, 25, 50],
        language: {
            search: "_INPUT_",
            searchPlaceholder: "Search...",
            lengthMenu: "Show _MENU_ entries",
            paginate: {
                first: '<i class="fas fa-angle-double-left"></i>',
                previous: '<i class="fas fa-angle-left"></i>',
                next: '<i class="fas fa-angle-right"></i>',
                last: '<i class="fas fa-angle-double-right"></i>'
            }
        }
    });
}

/**
 * Initialize theme toggle (light/dark mode)
 */
function initThemeToggle() {
    const themeToggle = document.querySelector('.theme-toggle');
    
    if (themeToggle) {
        // Check for saved theme preference or respect OS preference
        const savedTheme = localStorage.getItem('theme');
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        
        if (savedTheme === 'dark' || (!savedTheme && prefersDark)) {
            document.body.classList.add('dark-mode');
            themeToggle.querySelector('i').classList.replace('fa-moon', 'fa-sun');
        }
        
        // Toggle theme on click
        themeToggle.addEventListener('click', function() {
            document.body.classList.toggle('dark-mode');
            
            const isDark = document.body.classList.contains('dark-mode');
            localStorage.setItem('theme', isDark ? 'dark' : 'light');
            
            // Toggle icon
            const icon = this.querySelector('i');
            if (isDark) {
                icon.classList.replace('fa-moon', 'fa-sun');
            } else {
                icon.classList.replace('fa-sun', 'fa-moon');
            }
        });
    }
}

/**
 * Initialize notifications system
 */
function initNotifications() {
    // Check for new notifications (example)
    const notificationBell = document.querySelector('.notification-bell');
    const notificationDropdown = document.querySelector('.notification-dropdown');
    
    if (notificationBell && notificationDropdown) {
        // Fetch notifications (example)
        fetchNotifications();
        
        // Toggle notifications dropdown
        notificationBell.addEventListener('click', function(e) {
            e.preventDefault();
            notificationDropdown.classList.toggle('show');
            
            // Mark as read when opened
            if (notificationDropdown.classList.contains('show')) {
                markNotificationsAsRead();
            }
        });
    }
}

/**
 * Fetch notifications from server (example)
 */
function fetchNotifications() {
    // This is a placeholder - replace with actual API call
    const notificationCount = document.querySelector('.notification-count');
    const notificationList = document.querySelector('.notification-list');
    
    // Simulate API call
    setTimeout(() => {
        // Example notifications
        const notifications = [
            { id: 1, title: 'New assignment', message: 'You have a new assignment in Math course', time: '5 min ago', read: false },
            { id: 2, title: 'Grade posted', message: 'Your Science exam has been graded', time: '1 hour ago', read: false },
            { id: 3, title: 'Course update', message: 'History course materials have been updated', time: '3 hours ago', read: true }
        ];
        
        // Update notification count
        const unreadCount = notifications.filter(n => !n.read).length;
        if (notificationCount) {
            notificationCount.textContent = unreadCount;
            notificationCount.style.display = unreadCount > 0 ? 'flex' : 'none';
        }
        
        // Update notification list
        if (notificationList) {
            notificationList.innerHTML = '';
            
            if (notifications.length === 0) {
                notificationList.innerHTML = '<div class="notification-empty">No notifications</div>';
            } else {
                notifications.forEach(notification => {
                    const item = document.createElement('div');
                    item.className = `notification-item ${notification.read ? 'read' : 'unread'}`;
                    item.dataset.id = notification.id;
                    
                    item.innerHTML = `
                        <div class="notification-content">
                            <div class="notification-title">${notification.title}</div>
                            <div class="notification-message">${notification.message}</div>
                            <div class="notification-time">${notification.time}</div>
                        </div>
                        <button class="notification-mark-read" title="Mark as read">
                            <i class="fas fa-check"></i>
                        </button>
                    `;
                    
                    notificationList.appendChild(item);
                });
            }
        }
    }, 1000);
}

/**
 * Mark notifications as read (example)
 */
function markNotificationsAsRead() {
    // This is a placeholder - replace with actual API call
    const unreadNotifications = document.querySelectorAll('.notification-item.unread');
    const notificationCount = document.querySelector('.notification-count');
    
    unreadNotifications.forEach(notification => {
        // Simulate API call to mark as read
        setTimeout(() => {
            notification.classList.remove('unread');
            notification.classList.add('read');
            
            // Update count
            if (notificationCount) {
                const currentCount = parseInt(notificationCount.textContent);
                const newCount = Math.max(0, currentCount - 1);
                notificationCount.textContent = newCount;
                notificationCount.style.display = newCount > 0 ? 'flex' : 'none';
            }
        }, 500);
    });
}

/**
 * Show toast notification
 * @param {string} message - Message to display
 * @param {string} type - Type of notification (success, error, warning, info)
 * @param {number} duration - Duration in milliseconds
 */
function showToast(message, type = 'info', duration = 3000) {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    toast.innerHTML = `
        <div class="toast-icon">
            <i class="fas fa-${type === 'success' ? 'check-circle' : 
                              type === 'error' ? 'exclamation-circle' : 
                              type === 'warning' ? 'exclamation-triangle' : 
                              'info-circle'}"></i>
        </div>
        <div class="toast-content">${message}</div>
        <button class="toast-close"><i class="fas fa-times"></i></button>
    `;
    
    // Add to container or create one
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container';
        document.body.appendChild(toastContainer);
    }
    
    toastContainer.appendChild(toast);
    
    // Show with animation
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);
    
    // Close button
    toast.querySelector('.toast-close').addEventListener('click', () => {
        toast.classList.remove('show');
        setTimeout(() => {
            toast.remove();
        }, 300);
    });
    
    // Auto close
    if (duration > 0) {
        setTimeout(() => {
            if (toast.parentElement) {
                toast.classList.remove('show');
                setTimeout(() => {
                    toast.remove();
                }, 300);
            }
        }, duration);
    }
}

/**
 * Handle form submissions with AJAX
 * @param {HTMLFormElement} form - The form element
 * @param {Function} successCallback - Function to call on success
 * @param {Function} errorCallback - Function to call on error
 */
function handleFormSubmit(form, successCallback, errorCallback) {
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const submitBtn = form.querySelector('[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.classList.add('btn-loading');
        }
        
        const formData = new FormData(form);
        
        fetch(form.action, {
            method: form.method || 'POST',
            body: formData
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            if (typeof successCallback === 'function') {
                successCallback(data);
            } else {
                showToast(data.message || 'Success!', 'success');
            }
        })
        .catch(error => {
            if (typeof errorCallback === 'function') {
                errorCallback(error);
            } else {
                showToast(error.message || 'An error occurred', 'error');
            }
        })
        .finally(() => {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.classList.remove('btn-loading');
            }
        });
    });
}

/**
 * Load content dynamically via AJAX
 * @param {string} url - URL to fetch content from
 * @param {string} targetSelector - Selector for target element to update
 * @param {Function} callback - Function to call after content is loaded
 */
function loadContent(url, targetSelector, callback) {
    const target = document.querySelector(targetSelector);
    
    if (!target) return;
    
    // Show loading indicator
    target.innerHTML = '<div class="loading-spinner"></div>';
    
    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.text();
        })
        .then(html => {
            target.innerHTML = html;
            
            if (typeof callback === 'function') {
                callback();
            }
        })
        .catch(error => {
            target.innerHTML = `<div class="alert alert-danger">
                Error loading content: ${error.message}
            </div>`;
        });
}

/**
 * Initialize file upload with preview
 * @param {string} inputSelector - Selector for file input
 * @param {string} previewSelector - Selector for preview container
 */
function initFileUpload(inputSelector, previewSelector) {
    const input = document.querySelector(inputSelector);
    const preview = document.querySelector(previewSelector);
    
    if (!input || !preview) return;
    
    input.addEventListener('change', function() {
        preview.innerHTML = '';
        
        if (this.files && this.files.length > 0) {
            for (let i = 0; i < this.files.length; i++) {
                const file = this.files[i];
                const reader = new FileReader();
                
                reader.onload = function(e) {
                    const div = document.createElement('div');
                    div.className = 'file-preview-item';
                    
                    if (file.type.startsWith('image/')) {
                        div.innerHTML = `
                            <img src="${e.target.result}" alt="${file.name}">
                            <div class="file-preview-info">
                                <span class="file-name">${file.name}</span>
                                <span class="file-size">${formatFileSize(file.size)}</span>
                            </div>
                        `;
                    } else {
                        div.innerHTML = `
                            <div class="file-icon">
                                <i class="fas fa-file"></i>
                            </div>
                            <div class="file-preview-info">
                                <span class="file-name">${file.name}</span>
                                <span class="file-size">${formatFileSize(file.size)}</span>
                            </div>
                        `;
                    }
                    
                    preview.appendChild(div);
                };
                
                reader.readAsDataURL(file);
            }
        }
    });
}

/**
 * Format file size in human-readable format
 * @param {number} bytes - File size in bytes
 * @returns {string} Formatted file size
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}
