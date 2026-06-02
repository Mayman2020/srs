package com.gov.ac.feature.correspondence.readtracking.dto;

import java.util.List;
import java.util.UUID;

/**
 * Aggregate of read receipts for a single correspondence, used by the cross-user read-status
 * panel. Available only to callers with {@code CORRESPONDENCE_READ_STATUS_VIEW}.
 */
public record CorrespondenceReadStatusSummaryDto(
    UUID correspondenceId,
    int totalReaders,
    int acknowledgedReaders,
    List<CorrespondenceReadReceiptDto> receipts) {}
