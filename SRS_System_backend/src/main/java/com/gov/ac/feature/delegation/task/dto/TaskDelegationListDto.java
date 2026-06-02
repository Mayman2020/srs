package com.gov.ac.feature.delegation.task.dto;

import java.util.List;

/**
 * Bundle returned to the Delegations screen: outgoing (I am the delegator), incoming
 * (I am the delegate), and inactive (revoked or expired). Each list is independently
 * sorted by the service.
 */
public record TaskDelegationListDto(
    List<TaskDelegationDto> outgoingActive,
    List<TaskDelegationDto> incomingActive,
    List<TaskDelegationDto> inactive) {}
