package com.gov.ac.feature.acting.dto;

import java.util.List;

public record ActingAssignmentListDto(
    List<ActingAssignmentDto> asAbsent,
    List<ActingAssignmentDto> asActing,
    List<ActingAssignmentDto> upcoming,
    List<ActingAssignmentDto> inactive) {}
