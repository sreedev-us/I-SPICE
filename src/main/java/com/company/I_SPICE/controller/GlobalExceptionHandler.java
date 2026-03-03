package com.company.I_SPICE.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String handleGlobalException(Exception ex, Model model) {
        logger.error("An unexpected error occurred: ", ex);
        model.addAttribute("errorTitle", "Oops! Something went wrong.");
        model.addAttribute("errorMessage",
                "We encountered an unexpected issue while processing your request. Please try again later.");
        return "error";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFoundError(NoHandlerFoundException ex, Model model) {
        logger.warn("Page not found: {}", ex.getMessage());
        model.addAttribute("errorTitle", "404 - Page Not Found");
        model.addAttribute("errorMessage", "The page you are looking for does not exist or has been moved.");
        return "error";
    }
}
