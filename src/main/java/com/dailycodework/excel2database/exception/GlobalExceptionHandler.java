package com.dailycodework.excel2database.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EmptyResultDataAccessException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleEmptyResultDataAccessException(EmptyResultDataAccessException ex, Model model, HttpServletRequest request) {
        log.error("❌ EmptyResultDataAccessException: {}", ex.getMessage());
        model.addAttribute("error", "The requested data was not found. Please check your input and try again.");

        // Return appropriate view based on the request path
        return getViewNameBasedOnPath(request);
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleDatabaseConnectionError(CannotGetJdbcConnectionException ex, Model model, HttpServletRequest request) {
        log.error("❌ Database connection error: {}", ex.getMessage(), ex);
        model.addAttribute("error", "❌ Unable to connect to the database. Please try again later or contact support.");

        // Return appropriate view based on the request path
        return getViewNameBasedOnPath(request);
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleDataAccessException(DataAccessException ex, Model model, HttpServletRequest request) {
        log.error("❌ DataAccessException: {}", ex.getMessage(), ex);
        model.addAttribute("error", "❌ A database error occurred. Please try again later or contact support.");

        // Return appropriate view based on the request path
        return getViewNameBasedOnPath(request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleException(HttpServletRequest request, Exception ex) {
        log.error("❌ Exception for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        // For AJAX requests, we'll handle differently
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            ModelAndView mav = new ModelAndView();
            mav.addObject("error", "❌ An unexpected error occurred. Please try again later.");
            mav.setViewName("error-fragment"); // A minimal error view for AJAX
            return mav;
        }

        // For regular requests
        String viewName = getViewNameBasedOnPath(request);
        ModelAndView mav = new ModelAndView();
        mav.addObject("error", "❌ An unexpected error occurred. Please try again later.");
        mav.addObject("url", request.getRequestURL());
        mav.setViewName(viewName);
        return mav;
    }

    /**
     * Helper method to determine which view to return based on the request path
     */
    private String getViewNameBasedOnPath(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path.contains("/login") && !path.contains("/teacher")) {
            return "login";
        } else if (path.contains("/signup") && !path.contains("/teacher")) {
            return "signup";
        } else if (path.contains("/teacher/login")) {
            return "teacher-login";
        } else if (path.contains("/teacher/signup")) {
            return "teacher-signup";
        } else if (path.contains("/student-dashboard")) {
            return "student-dashboard";
        } else {
            return "error-page";
        }
    }
}
