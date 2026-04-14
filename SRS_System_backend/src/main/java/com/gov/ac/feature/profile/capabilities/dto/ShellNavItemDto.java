package com.gov.ac.feature.profile.capabilities.dto;

/**
 * Shell sidebar row. Stable {@code code} matches {@code ui_screen.code} and the frontend i18n prefix
 * {@code shellNav.<code>} in {@code ar.json}/{@code en.json}. {@code nameAr}/{@code nameEn} are
 * server-side labels (UTF-8) for admin/API fallbacks when no bundle key exists.
 */
public record ShellNavItemDto(
    String code,
    String routePath,
    String nameAr,
    String nameEn,
    Integer sortOrder,
    String iconKey) {}
