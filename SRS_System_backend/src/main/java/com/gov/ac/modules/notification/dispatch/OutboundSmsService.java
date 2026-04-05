package com.gov.ac.modules.notification.dispatch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OutboundSmsService {

  public void send(String phoneE164, String message) {
    log.info("SMS (stub) -> {} : {}", phoneE164, message);
  }
}
