package com.gov.ac.persistence;

import com.gov.ac.domain.correspondence.CorrespondenceComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrespondenceCommentRepository extends JpaRepository<CorrespondenceComment, Long> {}
