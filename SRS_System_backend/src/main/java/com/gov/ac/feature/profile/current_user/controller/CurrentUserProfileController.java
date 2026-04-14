package com.gov.ac.feature.profile.current_user.controller;

import com.gov.ac.feature.profile.current_user.dto.CurrentUserProfileDto;
import com.gov.ac.feature.profile.current_user.dto.UpdateCurrentUserPasswordRequestDto;
import com.gov.ac.feature.profile.current_user.dto.UpdateCurrentUserProfileRequestDto;
import com.gov.ac.feature.profile.current_user.dto.UpdateUserUiPreferencesRequestDto;
import com.gov.ac.feature.profile.current_user.service.CurrentUserProfileService;
import com.gov.ac.security.SecurityUtils;
import java.io.IOException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class CurrentUserProfileController {

  private final CurrentUserProfileService currentUserProfileService;

  @GetMapping("/me")
  public CurrentUserProfileDto getMyProfile() {
    return currentUserProfileService.getCurrentProfile(SecurityUtils.requireCurrentUserId());
  }

  @GetMapping("/me/avatar")
  public ResponseEntity<FileSystemResource> getMyAvatar() {
    var image = currentUserProfileService.getProfileImage(SecurityUtils.requireCurrentUserId());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .body(new FileSystemResource(image.path()));
  }

  @PutMapping("/me")
  public CurrentUserProfileDto updateMyProfile(
      @Valid @RequestBody UpdateCurrentUserProfileRequestDto request) {
    return currentUserProfileService.updateCurrentProfile(
        SecurityUtils.requireCurrentUserId(), request);
  }

  @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CurrentUserProfileDto updateMyAvatar(@RequestPart("file") MultipartFile file)
      throws IOException {
    return currentUserProfileService.updateProfileImage(SecurityUtils.requireCurrentUserId(), file);
  }

  @PutMapping("/me/ui")
  public CurrentUserProfileDto updateMyUiPreferences(
      @Valid @RequestBody UpdateUserUiPreferencesRequestDto request) {
    return currentUserProfileService.updateUiPreferences(
        SecurityUtils.requireCurrentUserId(), request);
  }

  @PutMapping("/me/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateMyPassword(@Valid @RequestBody UpdateCurrentUserPasswordRequestDto request) {
    currentUserProfileService.changePassword(SecurityUtils.requireCurrentUserId(), request);
  }
}
