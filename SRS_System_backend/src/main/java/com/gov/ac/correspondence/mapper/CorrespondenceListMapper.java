package com.gov.ac.correspondence.mapper;

import com.gov.ac.correspondence.dto.CorrespondenceListItemDto;
import com.gov.ac.correspondence.dto.DepartmentSummaryDto;
import com.gov.ac.correspondence.dto.LookupLabelDto;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.lookup.CorrespondenceStatus;
import com.gov.ac.domain.lookup.CorrespondenceType;
import com.gov.ac.domain.lookup.Priority;
import com.gov.ac.domain.org.Department;
import org.springframework.stereotype.Component;

@Component
public class CorrespondenceListMapper {

  public CorrespondenceListItemDto toListItem(Correspondence c) {
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

  private static LookupLabelDto toTypeLabel(CorrespondenceType t) {
    if (t == null) {
      return null;
    }
    return LookupLabelDto.builder().code(t.getCode()).nameAr(t.getNameAr()).nameEn(t.getNameEn()).build();
  }

  private static LookupLabelDto toStatusLabel(CorrespondenceStatus s) {
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

  private static LookupLabelDto toPriorityLabel(Priority p) {
    if (p == null) {
      return null;
    }
    return LookupLabelDto.builder().code(p.getCode()).nameAr(p.getNameAr()).nameEn(p.getNameEn()).build();
  }

  private static DepartmentSummaryDto toDepartmentSummary(Department d) {
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
