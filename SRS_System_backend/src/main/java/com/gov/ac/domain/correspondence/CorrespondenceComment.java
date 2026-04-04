package com.gov.ac.domain.correspondence;

import com.gov.ac.domain.base.SoftDeletableEntity;
import com.gov.ac.domain.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "correspondence_comment")
@Getter
@Setter
public class CorrespondenceComment extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private Correspondence correspondence;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_user_id", nullable = false)
  private AppUser author;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_comment_id")
  private CorrespondenceComment parentComment;
}
