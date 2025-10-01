package com.locationservicemaster.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Application Email Service
 * Equivalent to Ruby's ApplicationMailer
 * Provides email sending functionality with Thymeleaf templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationEmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.default-from:from@example.com}")
    private String defaultFromAddress;
    
    /**
     * Send a simple text email
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param text Plain text content
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(defaultFromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
            throw new EmailSendException("Failed to send email", e);
        }
    }
    
    /**
     * Send an HTML email using Thymeleaf template
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param templateName Template name (without .html extension)
     * @param variables Template variables
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(defaultFromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Process the template
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process("email/" + templateName, context);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("HTML email sent successfully to: {} using template: {}", to, templateName);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new EmailSendException("Failed to send HTML email", e);
        }
    }
    
    /**
     * Send an email with both HTML and plain text versions
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlTemplateName HTML template name
     * @param textTemplateName Text template name
     * @param variables Template variables
     */
    public void sendMultipartEmail(String to, String subject, String htmlTemplateName, 
                                   String textTemplateName, Map<String, Object> variables) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(defaultFromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Process templates
            Context context = new Context();
            context.setVariables(variables);
            
            String textContent = templateEngine.process("email/" + textTemplateName, context);
            String htmlContent = templateEngine.process("email/" + htmlTemplateName, context);
            
            helper.setText(textContent, htmlContent);
            
            mailSender.send(mimeMessage);
            log.info("Multipart email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send multipart email to: {}", to, e);
            throw new EmailSendException("Failed to send multipart email", e);
        }
    }
    
    /**
     * Send email with custom from address
     */
    public void sendHtmlEmailWithCustomFrom(String from, String to, String subject, 
                                           String templateName, Map<String, Object> variables) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process("email/" + templateName, context);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("HTML email sent successfully from: {} to: {}", from, to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new EmailSendException("Failed to send HTML email", e);
        }
    }
    
    /**
     * Custom exception for email sending failures
     */
    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}