package com.gov.ac.feature.letter_templates.entity;

import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "correspondence_letter_template", schema = "srs_system")
@Getter
@Setter
public class CorrespondenceLetterTemplateEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String code;

  @Column(name = "name_ar", nullable = false)
  private String nameAr;

  @Column(name = "name_en", nullable = false)
  private String nameEn;

  @Column(name = "body_html", nullable = false, columnDefinition = "text")
  private String bodyHtml = "";

  /** Optional path under {@code ac.storage.root} to load HTML instead of {@link #bodyHtml}. */
  @Column(name = "template_file_path", length = 500)
  private String templateFilePath;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;
}
