package com.gov.ac.feature.correspondence.mapper;

import com.gov.ac.feature.correspondence.dto.CorrespondenceListItemDto;
import com.gov.ac.feature.correspondence.dto.DepartmentSummaryDto;
import com.gov.ac.feature.correspondence.dto.LookupLabelDto;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.PriorityEntity;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import org.springframework.stereotype.Component;

@Component
public class CorrespondenceListMapper {

  public CorrespondenceListItemDto toListItem(CorrespondenceEntity c) {
    return CorrespondenceListItemDto.builder()
        .id(c.getId())
        .referenceNumber(c.getReferenceNumber())
        .subject(c.getSubject())
        .createdAt(c.getCreatedAt())
        .updatedAt(c.getUpdatedAt())
        .dueDate(c.getDueDate())
        .correspondenceType(toTypeLabel(c.getCorrespondenceType()))
        .correspondenceStatus(toStatusLabel(c.getCorrespondenceStatus()))
        .priority(toPriorityLabel(c.getPriority()))
        .ownerDepartment(toDepartmentSummary(c.getOwnerDepartment()))
        .build();
  }

  private static LookupLabelDto toTypeLabel(CorrespondenceTypeEntity t) {
    if (t == null) {
      return null;
    }
    return LookupLabelDto.builder().code(t.getCode()).nameAr(t.getNameAr()).nameEn(t.getNameEn()).build();
  }

  private static LookupLabelDto toStatusLabel(CorrespondenceStatusEntity s) {
    if (s == null) {
      return null;
    }
    return LookupLabelDto.builder()
        .code(s.getCode())
        .nameAr(s.getNameAr())
        .nameEn(s.getNameEn())
        .uiVariant(s.getUiVariant())
        .build();
  }

  private static LookupLabelDto toPriorityLabel(PriorityEntity p) {
    if (p == null) {
      return null;
    }
    return LookupLabelDto.builder().code(p.getCode()).nameAr(p.getNameAr()).nameEn(p.getNameEn()).build();
  }

  private static DepartmentSummaryDto toDepartmentSummary(DepartmentEntity d) {
    if (d == null) {
      return null;
    }
    return DepartmentSummaryDto.builder()
        .id(d.getId())
        .code(d.getCode())
        .nameAr(d.getNameAr())
        .nameEn(d.getNameEn())
        .build();
  }
}
