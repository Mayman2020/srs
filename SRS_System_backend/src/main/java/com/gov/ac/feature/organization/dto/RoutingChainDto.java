package com.gov.ac.feature.organization.dto;

import java.util.List;

/**
 * Result of {@code OrgRoutingService#computeChain(originator, target)}. Carries the resolved
 * ordered list of {@link RoutingStopDto}s plus the originator/target nodes for audit display.
 *
 * <p>Used by:
 *
 * <ul>
 *   <li>Workflow start delegates: persist as {@code workflow_instance.routing_chain_json} and
 *       drive the per-stop multi-instance subprocess.
 *   <li>Frontend routing-preview API: render the chain badge before submission.
 * </ul>
 */
public record RoutingChainDto(
    RoutingStopDto originator,
    RoutingStopDto target,
    List<RoutingStopDto> stops,
    String reasonKey) {}
