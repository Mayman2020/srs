package com.gov.ac.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  /** Placeholder until notification module maps {@code notification} table (Phase C extension). */
  @GetMapping
  public List<Object> inbox() {
    return List.of();
  }
}
