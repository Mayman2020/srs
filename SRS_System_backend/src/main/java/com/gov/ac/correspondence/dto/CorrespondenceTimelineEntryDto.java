package com.gov.ac.correspondence.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

/**
 * Workflow timeline entry derived from {@code workflow_history}. {@code action} prefers
 * {@code workflow_action_type.code} when present, otherwise {@code workflow_history_event_type.code}.
 */
@Value
@Builder
public class CorrespondenceTimelineEntryDto {
  Long historyId;
  int sequenceNo;
  /** Business action code (workflow action type, or event type as fallback). */
  String action;
  /** Original event classification code (e.g. CREATE, USER_ACTION). */
  String eventTypeCode;
  /** Actor for this timeline point; null for system-only rows. */
  UserSummaryDto user;
  Instant timestamp;
  String comment;
  /** Status after this event ({@code new_correspondence_status.code}). */
  String status;
  String previousStatusCode;
}
