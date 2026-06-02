package com.gov.ac.feature.reports.dto;

import java.time.Instant;

/**
 * One point on the routing-time SLA chart: the calendar month bucket, the average end-to-end
 * routing duration (seconds) for workflows that completed in that month, and how many workflows
 * contributed to the average.
 */
public record WorkflowSlaPointDto(Instant bucketStart, long averageRoutingSeconds, long completedCount) {}
