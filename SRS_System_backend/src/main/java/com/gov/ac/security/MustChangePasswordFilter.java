package com.gov.ac.security;

import com.gov.ac.feature.users.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class MustChangePasswordFilter extends OncePerRequestFilter {

  private final AppUserRepository appUserRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UUID userId) {
      boolean required = appUserRepository.findByIdAndDeletedAtIsNull(userId)
          .map(user -> Boolean.TRUE.equals(user.getMustChangePassword()))
          .orElse(false);
      if (required && !isAllowedWhilePasswordChangeRequired(request)) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"errorCode\":\"PASSWORD_CHANGE_REQUIRED\",\"detail\":\"PASSWORD_CHANGE_REQUIRED\"}");
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  private boolean isAllowedWhilePasswordChangeRequired(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/api/v1/profile/") || path.startsWith("/api/v1/auth/");
  }
}
