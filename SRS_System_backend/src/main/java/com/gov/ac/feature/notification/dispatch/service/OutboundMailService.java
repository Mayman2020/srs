package com.gov.ac.feature.notification.dispatch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundMailService {

  private final JavaMailSender mailSender;

  public void send(String to, String subject, String text) {
    try {
      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setTo(to);
      msg.setSubject(subject);
      msg.setText(text);
      mailSender.send(msg);
    } catch (Exception e) {
      log.warn("Email send failed (configure spring.mail): {}", e.getMessage());
    }
  }
}
