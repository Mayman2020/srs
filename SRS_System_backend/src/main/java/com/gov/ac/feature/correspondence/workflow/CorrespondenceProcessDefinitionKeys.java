package com.gov.ac.feature.correspondence.workflow;

public final class CorrespondenceProcessDefinitionKeys {

  private CorrespondenceProcessDefinitionKeys() {}

  public static final String INBOUND = "inbound-correspondence";
  public static final String OUTBOUND = "outbound-correspondence";
  public static final String INTERNAL = "internal-correspondence";

  public static String forCorrespondenceTypeCode(String typeCode) {
    if (typeCode == null) {
      return INTERNAL;
    }
    return switch (typeCode.toUpperCase()) {
      case "INBOUND" -> INBOUND;
      case "OUTBOUND" -> OUTBOUND;
      default -> INTERNAL;
    };
  }
}
