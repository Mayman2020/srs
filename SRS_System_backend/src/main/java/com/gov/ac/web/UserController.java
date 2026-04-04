package com.gov.ac.web;

import com.gov.ac.domain.org.Department;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.web.dto.UserListDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final AppUserRepository appUserRepository;

  @GetMapping
  public Page<UserListDto> page(@PageableDefault(size = 50) Pageable pageable) {
    return appUserRepository.findByDeletedAtIsNull(pageable).map(this::toDto);
  }

  private UserListDto toDto(AppUser u) {
    Department d = u.getDepartment();
    String deptCode = d != null ? d.getCode() : null;
    return new UserListDto(
        u.getId(),
        u.getUsername(),
        u.getFullNameAr(),
        u.getFullNameEn(),
        u.getEmail(),
        deptCode,
        u.getActive());
  }
}
