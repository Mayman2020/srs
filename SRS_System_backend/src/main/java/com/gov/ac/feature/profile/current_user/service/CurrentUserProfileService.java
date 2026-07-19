package com.gov.ac.feature.profile.current_user.service;

import com.gov.ac.feature.attachment.service.AttachmentStorageProperties;
import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.profile.current_user.dto.CurrentUserProfileDto;
import com.gov.ac.feature.profile.current_user.mapper.CurrentUserProfileMapper;
import com.gov.ac.feature.profile.current_user.dto.UpdateCurrentUserPasswordRequestDto;
import com.gov.ac.feature.profile.current_user.dto.UpdateCurrentUserProfileRequestDto;
import com.gov.ac.feature.profile.current_user.dto.UpdateUserUiPreferencesRequestDto;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CurrentUserProfileService {

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final AttachmentStorageProperties storageProperties;
  private final CurrentUserProfileMapper currentUserProfileMapper;

  @Transactional(readOnly = true)
  public CurrentUserProfileDto getCurrentProfile(UUID userId) {
    return currentUserProfileMapper.toDto(loadCurrentUser(userId));
  }

  @Transactional
  public CurrentUserProfileDto updateUiPreferences(
      UUID userId, UpdateUserUiPreferencesRequestDto request) {
    AppUserEntity user = loadCurrentUser(userId);
    user.setUiTheme(request.uiTheme());
    user.setUiLocale(request.uiLocale());
    appUserRepository.save(user);
    return currentUserProfileMapper.toDto(user);
  }

  @Transactional
  public CurrentUserProfileDto updateCurrentProfile(
      UUID userId, UpdateCurrentUserProfileRequestDto request) {
    AppUserEntity user = loadCurrentUser(userId);
    String normalizedEmail = request.email().trim().toLowerCase();
    if (appUserRepository.existsByEmailAndDeletedAtIsNullAndIdNot(
        normalizedEmail, userId)) {
      throw new BadRequestException("profile.errors.emailInUse");
    }

    user.setFullNameAr(request.fullNameAr().trim());
    user.setFullNameEn(request.fullNameEn().trim());
    user.setEmail(normalizedEmail);
    user.setPhone(normalizeNullable(request.phone()));
    user.setNationalId(normalizeNullable(request.nationalId()));
    appUserRepository.save(user);
    return currentUserProfileMapper.toDto(user);
  }

  @Transactional
  public void changePassword(UUID userId, UpdateCurrentUserPasswordRequestDto request) {
    AppUserEntity user = loadCurrentUser(userId);
    String currentHash = user.getPasswordHash();
    if (currentHash == null || currentHash.isBlank()) {
      throw new BadRequestException("profile.errors.passwordNotConfigured");
    }
    if (!passwordEncoder.matches(request.currentPassword(), currentHash)) {
      throw new BadRequestException("profile.errors.currentPasswordIncorrect");
    }

    String newPassword = request.newPassword().trim();
    if (passwordEncoder.matches(newPassword, currentHash)) {
      throw new BadRequestException("profile.errors.passwordReuse");
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setPasswordChangedAt(Instant.now());
    user.setFailedLoginCount(0);
    user.setLockedUntil(null);
    user.setMustChangePassword(false);
    appUserRepository.save(user);
  }

  @Transactional
  public CurrentUserProfileDto updateProfileImage(UUID userId, MultipartFile file) throws IOException {
    AppUserEntity user = loadCurrentUser(userId);
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("profile.errors.avatarRequired");
    }

    String extension = resolveImageExtension(file.getOriginalFilename(), file.getContentType());
    String relativePath = "SRS/profile-images/" + user.getId() + "/avatar" + extension;
    Path root = storageRoot();
    Path target = root.resolve(relativePath).normalize();
    if (!target.startsWith(root)) {
      throw new IllegalStateException("Invalid profile image path");
    }

    Files.createDirectories(target.getParent());
    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

    String previousPath = user.getProfileImagePath();
    if (previousPath != null && !previousPath.isBlank() && !previousPath.equals(relativePath)) {
      deleteQuietly(root.resolve(previousPath).normalize(), root);
    }

    String contentType =
        file.getContentType() != null && !file.getContentType().isBlank()
            ? file.getContentType()
            : Files.probeContentType(target);
    if (contentType == null || contentType.isBlank()) {
      contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    user.setProfileImagePath(relativePath);
    user.setProfileImageContentType(contentType);
    appUserRepository.save(user);
    return currentUserProfileMapper.toDto(user);
  }

  @Transactional(readOnly = true)
  public StoredProfileImage getProfileImage(UUID userId) {
    AppUserEntity user = loadCurrentUser(userId);
    String relativePath = normalizeNullable(user.getProfileImagePath());
    if (relativePath == null) {
      throw new NotFoundException("profile.errors.avatarMissing");
    }

    Path root = storageRoot();
    Path path = root.resolve(relativePath).normalize();
    if (!path.startsWith(root) || !Files.exists(path)) {
      throw new NotFoundException("profile.errors.avatarMissing");
    }

    String contentType = normalizeNullable(user.getProfileImageContentType());
    if (contentType == null) {
      try {
        contentType = Files.probeContentType(path);
      } catch (IOException ignored) {
        contentType = null;
      }
    }
    if (contentType == null) {
      contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
    return new StoredProfileImage(path, contentType);
  }

  private AppUserEntity loadCurrentUser(UUID userId) {
    AppUserEntity user =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new NotFoundException("Current user not found"));
    if (!Boolean.TRUE.equals(user.getActive())) {
      throw new ForbiddenException("profile.errors.accountInactive");
    }
    return user;
  }

  private Path storageRoot() {
    return Paths.get(storageProperties.root()).toAbsolutePath().normalize();
  }

  private String resolveImageExtension(String originalFilename, String contentType) {
    String ext = "";
    if (originalFilename != null) {
      int dot = originalFilename.lastIndexOf('.');
      if (dot >= 0 && dot < originalFilename.length() - 1) {
        ext = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
      }
    }
    if (".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext) || ".gif".equals(ext)) {
      return ext;
    }
    if (contentType != null) {
      return switch (contentType.toLowerCase(Locale.ROOT)) {
        case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
        case MediaType.IMAGE_PNG_VALUE -> ".png";
        case "image/webp" -> ".webp";
        case MediaType.IMAGE_GIF_VALUE -> ".gif";
        default -> throw new BadRequestException("profile.errors.avatarInvalidType");
      };
    }
    throw new BadRequestException("profile.errors.avatarInvalidType");
  }

  private void deleteQuietly(Path path, Path root) {
    try {
      if (path.startsWith(root)) {
        Files.deleteIfExists(path);
      }
    } catch (IOException ignored) {
      // Ignore old-avatar cleanup failures; latest upload already succeeded.
    }
  }

  private String normalizeNullable(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isBlank() ? null : normalized;
  }

  public record StoredProfileImage(Path path, String contentType) {}
}
