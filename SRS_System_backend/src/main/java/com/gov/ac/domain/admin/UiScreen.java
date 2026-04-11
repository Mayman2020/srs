package com.gov.ac.domain.admin;

import com.gov.ac.domain.base.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ui_screen", schema = "srs_system")
@Getter
@Setter
public class UiScreen extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 128)
  private String code;

  @Column(name = "route_path", nullable = false, length = 512)
  private String routePath;

  @Column(name = "name_ar", nullable = false)
  private String nameAr;

  @Column(name = "name_en", nullable = false)
  private String nameEn;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;

  @Column(name = "required_permission_id")
  private Long requiredPermissionId;

  @Column(name = "icon_key", nullable = false, length = 64)
  private String iconKey = "apps";

  @Column(name = "show_in_shell_nav", nullable = false)
  private Boolean showInShellNav = false;
}
