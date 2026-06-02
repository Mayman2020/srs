package com.gov.ac.common.i18n;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Server-side i18n setup. Backend exception messages and validation defaults resolve through
 * {@link MessageSource} using the {@code Accept-Language} header (ar / en). Falls back to {@code
 * en} so the system never emits a raw bundle key. Resource bundles live under
 * {@code src/main/resources/i18n/messages_*.properties}.
 */
@Configuration
public class MessagesConfig {

  static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  @Bean
  MessageSource messageSource() {
    var source = new ReloadableResourceBundleMessageSource();
    source.setBasenames("classpath:i18n/messages", "classpath:i18n/errors");
    source.setDefaultEncoding(StandardCharsets.UTF_8.name());
    source.setDefaultLocale(DEFAULT_LOCALE);
    source.setFallbackToSystemLocale(false);
    source.setUseCodeAsDefaultMessage(true);
    source.setCacheSeconds(60);
    return source;
  }

  @Bean
  LocaleResolver localeResolver() {
    var resolver = new AcceptHeaderLocaleResolver();
    resolver.setDefaultLocale(DEFAULT_LOCALE);
    resolver.setSupportedLocales(java.util.List.of(Locale.ENGLISH, new Locale("ar")));
    return resolver;
  }
}
