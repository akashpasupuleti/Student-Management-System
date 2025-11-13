/**
 * Charts and data visualization for the LMS Portal
 * Uses Chart.js library
 */

// Initialize charts when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Initialize all charts on the page
    initializeCharts();
});

/**
 * Initialize all charts on the page
 */
function initializeCharts() {
    // Check if Chart.js is available
    if (typeof Chart === 'undefined') {
        console.warn('Chart.js is not loaded. Charts will not be rendered.');
        return;
    }
    
    // Set default chart options
    Chart.defaults.font.family = "'Poppins', sans-serif";
    Chart.defaults.color = '#4b5563';
    Chart.defaults.responsive = true;
    
    // Initialize different chart types
    initializeGradeDistributionChart();
    initializeStudentProgressChart();
    initializeAttendanceChart();
    initializePerformanceComparisonChart();
    initializeCourseCompletionChart();
    
    // Initialize any custom charts with data attributes
    initializeCustomCharts();
}

/**
 * Initialize grade distribution chart (pie chart)
 */
function initializeGradeDistributionChart() {
    const ctx = document.getElementById('gradeDistributionChart');
    if (!ctx) return;
    
    // Sample data - replace with actual data from your backend
    const data = {
        labels: ['A (90-100%)', 'B (80-89%)', 'C (70-79%)', 'D (60-69%)', 'F (Below 60%)'],
        datasets: [{
            data: [30, 25, 20, 15, 10],
            backgroundColor: [
                '#10b981', // Success - A
                '#4361ee', // Primary - B
                '#f59e0b', // Warning - C
                '#8b5cf6', // Purple - D
                '#ef4444'  // Danger - F
            ],
            borderWidth: 1,
            borderColor: '#ffffff'
        }]
    };
    
    const options = {
        plugins: {
            legend: {
                position: 'right',
                labels: {
                    padding: 20,
                    boxWidth: 15,
                    font: {
                        size: 12
                    }
                }
            },
            tooltip: {
                callbacks: {
                    label: function(context) {
                        const label = context.label || '';
                        const value = context.raw || 0;
                        const total = context.dataset.data.reduce((a, b) => a + b, 0);
                        const percentage = Math.round((value / total) * 100);
                        return `${label}: ${value} students (${percentage}%)`;
                    }
                }
            }
        },
        cutout: '40%'
    };
    
    new Chart(ctx, {
        type: 'doughnut',
        data: data,
        options: options
    });
}

/**
 * Initialize student progress chart (line chart)
 */
function initializeStudentProgressChart() {
    const ctx = document.getElementById('studentProgressChart');
    if (!ctx) return;
    
    // Sample data - replace with actual data from your backend
    const data = {
        labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4', 'Week 5', 'Week 6', 'Week 7', 'Week 8'],
        datasets: [{
            label: 'Current Semester',
            data: [65, 70, 68, 75, 82, 85, 80, 88],
            borderColor: '#4361ee',
            backgroundColor: 'rgba(67, 97, 238, 0.1)',
            tension: 0.3,
            fill: true
        }, {
            label: 'Previous Semester',
            data: [60, 65, 60, 68, 72, 75, 73, 78],
            borderColor: '#10b981',
            backgroundColor: 'rgba(16, 185, 129, 0.1)',
            tension: 0.3,
            fill: true,
            borderDash: [5, 5]
        }]
    };
    
    const options = {
        scales: {
            y: {
                beginAtZero: false,
                min: 50,
                max: 100,
                title: {
                    display: true,
                    text: 'Score (%)'
                }
            },
            x: {
                grid: {
                    display: false
                }
            }
        },
        plugins: {
            legend: {
                position: 'top'
            },
            tooltip: {
                mode: 'index',
                intersect: false
            }
        },
        interaction: {
            mode: 'nearest',
            axis: 'x',
            intersect: false
        }
    };
    
    new Chart(ctx, {
        type: 'line',
        data: data,
        options: options
    });
}

/**
 * Initialize attendance chart (bar chart)
 */
function initializeAttendanceChart() {
    const ctx = document.getElementById('attendanceChart');
    if (!ctx) return;
    
    // Sample data - replace with actual data from your backend
    const data = {
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
        datasets: [{
            label: 'Present',
            data: [20, 18, 22, 19, 21, 20],
            backgroundColor: '#10b981'
        }, {
            label: 'Absent',
            data: [2, 4, 0, 3, 1, 2],
            backgroundColor: '#ef4444'
        }, {
            label: 'Late',
            data: [1, 2, 3, 2, 1, 3],
            backgroundColor: '#f59e0b'
        }]
    };
    
    const options = {
        scales: {
            y: {
                beginAtZero: true,
                stacked: true,
                title: {
                    display: true,
                    text: 'Number of Days'
                }
            },
            x: {
                stacked: true,
                grid: {
                    display: false
                }
            }
        },
        plugins: {
            legend: {
                position: 'top'
            },
            tooltip: {
                mode: 'index',
                intersect: false
            }
        }
    };
    
    new Chart(ctx, {
        type: 'bar',
        data: data,
        options: options
    });
}

/**
 * Initialize performance comparison chart (radar chart)
 */
function initializePerformanceComparisonChart() {
    const ctx = document.getElementById('performanceComparisonChart');
    if (!ctx) return;
    
    // Sample data - replace with actual data from your backend
    const data = {
        labels: ['Mathematics', 'Science', 'History', 'Language', 'Arts', 'Physical Education'],
        datasets: [{
            label: 'Student Performance',
            data: [85, 75, 90, 80, 95, 70],
            backgroundColor: 'rgba(67, 97, 238, 0.2)',
            borderColor: '#4361ee',
            pointBackgroundColor: '#4361ee',
            pointBorderColor: '#fff',
            pointHoverBackgroundColor: '#fff',
            pointHoverBorderColor: '#4361ee'
        }, {
            label: 'Class Average',
            data: [75, 70, 80, 75, 85, 80],
            backgroundColor: 'rgba(16, 185, 129, 0.2)',
            borderColor: '#10b981',
            pointBackgroundColor: '#10b981',
            pointBorderColor: '#fff',
            pointHoverBackgroundColor: '#fff',
            pointHoverBorderColor: '#10b981'
        }]
    };
    
    const options = {
        scales: {
            r: {
                angleLines: {
                    display: true
                },
                suggestedMin: 50,
                suggestedMax: 100
            }
        },
        plugins: {
            legend: {
                position: 'top'
            },
            tooltip: {
                callbacks: {
                    label: function(context) {
                        return `${context.dataset.label}: ${context.raw}%`;
                    }
                }
            }
        }
    };
    
    new Chart(ctx, {
        type: 'radar',
        data: data,
        options: options
    });
}

/**
 * Initialize course completion chart (progress bar)
 */
function initializeCourseCompletionChart() {
    const ctx = document.getElementById('courseCompletionChart');
    if (!ctx) return;
    
    // Sample data - replace with actual data from your backend
    const data = {
        labels: ['Completed', 'In Progress', 'Not Started'],
        datasets: [{
            data: [65, 25, 10],
            backgroundColor: [
                '#10b981', // Completed
                '#f59e0b', // In Progress
                '#ef4444'  // Not Started
            ],
            borderWidth: 0
        }]
    };
    
    const options = {
        indexAxis: 'y',
        plugins: {
            legend: {
                display: false
            },
            tooltip: {
                callbacks: {
                    label: function(context) {
                        return `${context.label}: ${context.raw}%`;
                    }
                }
            }
        },
        scales: {
            x: {
                stacked: true,
                beginAtZero: true,
                max: 100,
                grid: {
                    display: false
                },
                ticks: {
                    callback: function(value) {
                        return value + '%';
                    }
                }
            },
            y: {
                stacked: true,
                grid: {
                    display: false
                }
            }
        }
    };
    
    new Chart(ctx, {
        type: 'bar',
        data: data,
        options: options
    });
}

/**
 * Initialize custom charts with data attributes
 */
function initializeCustomCharts() {
    const customCharts = document.querySelectorAll('[data-chart]');
    
    customCharts.forEach(container => {
        const canvas = container.querySelector('canvas');
        if (!canvas) return;
        
        const chartType = container.dataset.chart;
        const chartId = canvas.id;
        
        // Check if data is provided via data attribute
        if (container.dataset.chartData) {
            try {
                const chartData = JSON.parse(container.dataset.chartData);
                const chartOptions = container.dataset.chartOptions ? 
                    JSON.parse(container.dataset.chartOptions) : {};
                
                new Chart(canvas, {
                    type: chartType,
                    data: chartData,
                    options: chartOptions
                });
            } catch (error) {
                console.error(`Error initializing chart ${chartId}:`, error);
            }
        }
    });
}

/**
 * Create a grade history chart for a specific student
 * @param {string} canvasId - ID of the canvas element
 * @param {Array} semesters - Array of semester labels
 * @param {Array} grades - Array of grade values
 */
function createGradeHistoryChart(canvasId, semesters, grades) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    
    const data = {
        labels: semesters,
        datasets: [{
            label: 'GPA',
            data: grades,
            borderColor: '#4361ee',
            backgroundColor: 'rgba(67, 97, 238, 0.1)',
            tension: 0.3,
            fill: true
        }]
    };
    
    const options = {
        scales: {
            y: {
                beginAtZero: false,
                min: Math.max(0, Math.min(...grades) - 0.5),
                max: Math.min(4.0, Math.max(...grades) + 0.5),
                title: {
                    display: true,
                    text: 'GPA'
                }
            },
            x: {
                grid: {
                    display: false
                }
            }
        },
        plugins: {
            legend: {
                display: false
            },
            tooltip: {
                callbacks: {
                    label: function(context) {
                        return `GPA: ${context.raw}`;
                    }
                }
            }
        }
    };
    
    return new Chart(ctx, {
        type: 'line',
        data: data,
        options: options
    });
}

/**
 * Create a subject performance chart
 * @param {string} canvasId - ID of the canvas element
 * @param {Array} subjects - Array of subject labels
 * @param {Array} scores - Array of score values
 */
function createSubjectPerformanceChart(canvasId, subjects, scores) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    
    // Generate colors based on scores
    const colors = scores.map(score => {
        if (score >= 90) return '#10b981'; // A
        if (score >= 80) return '#4361ee'; // B
        if (score >= 70) return '#f59e0b'; // C
        if (score >= 60) return '#8b5cf6'; // D
        return '#ef4444'; // F
    });
    
    const data = {
        labels: subjects,
        datasets: [{
            label: 'Score',
            data: scores,
            backgroundColor: colors,
            borderColor: colors,
            borderWidth: 1
        }]
    };
    
    const options = {
        scales: {
            y: {
                beginAtZero: true,
                max: 100,
                title: {
                    display: true,
                    text: 'Score (%)'
                }
            },
            x: {
                grid: {
                    display: false
                }
            }
        },
        plugins: {
            legend: {
                display: false
            },
            tooltip: {
                callbacks: {
                    label: function(context) {
                        const score = context.raw;
                        let grade = '';
                        
                        if (score >= 90) grade = 'A';
                        else if (score >= 80) grade = 'B';
                        else if (score >= 70) grade = 'C';
                        else if (score >= 60) grade = 'D';
                        else grade = 'F';
                        
                        return `Score: ${score}% (${grade})`;
                    }
                }
            }
        }
    };
    
    return new Chart(ctx, {
        type: 'bar',
        data: data,
        options: options
    });
}

/**
 * Update chart data
 * @param {Chart} chart - Chart.js instance
 * @param {Array} labels - New labels
 * @param {Array} data - New data values
 * @param {number} datasetIndex - Index of dataset to update (default: 0)
 */
function updateChartData(chart, labels, data, datasetIndex = 0) {
    if (!chart) return;
    
    chart.data.labels = labels;
    chart.data.datasets[datasetIndex].data = data;
    chart.update();
}

/**
 * Create a comparison chart between student and class average
 * @param {string} canvasId - ID of the canvas element
 * @param {Array} categories - Array of category labels
 * @param {Array} studentData - Array of student scores
 * @param {Array} averageData - Array of class average scores
 */
function createComparisonChart(canvasId, categories, studentData, averageData) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    
    const data = {
        labels: categories,
        datasets: [{
            label: 'Your Score',
            data: studentData,
            backgroundColor: 'rgba(67, 97, 238, 0.7)',
            borderColor: '#4361ee',
            borderWidth: 1
        }, {
            label: 'Class Average',
            data: averageData,
            backgroundColor: 'rgba(16, 185, 129, 0.7)',
            borderColor: '#10b981',
            borderWidth: 1
        }]
    };
    
    const options = {
        scales: {
            y: {
                beginAtZero: true,
                max: 100,
                title: {
                    display: true,
                    text: 'Score (%)'
                }
            },
            x: {
                grid: {
                    display: false
                }
            }
        },
        plugins: {
            legend: {
                position: 'top'
            },
            tooltip: {
                mode: 'index',
                intersect: false
            }
        }
    };
    
    return new Chart(ctx, {
        type: 'bar',
        data: data,
        options: options
    });
}
