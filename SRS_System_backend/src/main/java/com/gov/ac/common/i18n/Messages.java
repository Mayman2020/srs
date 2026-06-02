package com.gov.ac.common.i18n;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around {@link MessageSource} so services/controllers can resolve translated
 * messages without pulling the Spring API into business code.
 *
 * <p>Resolves against the request locale (set by {@link MessagesConfig#localeResolver()} from
 * the {@code Accept-Language} header) or an explicit locale when provided.
 */
@Component
@RequiredArgsConstructor
public class Messages {

  private final MessageSource messageSource;

  public String get(String key, Object... args) {
    return get(LocaleContextHolder.getLocale(), key, args);
  }

  public String get(Locale locale, String key, Object... args) {
    return messageSource.getMessage(key, args, key, locale);
  }
}
