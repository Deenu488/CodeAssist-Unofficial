package com.tyron.resolver;

public enum ScopeType {
  API("api"),
  IMPLEMENTATION("implementation"),
  COMPILE_ONLY("compileOnly"),
  RUNTIME_ONLY("runtimeOnly");

  private final String stringValue;

  ScopeType(String stringValue) {
    this.stringValue = stringValue;
  }

  public String getStringValue() {
    return stringValue;
  }

  public static ScopeType fromStringValue(String stringValue) {
    for (ScopeType scopeType : ScopeType.values()) {
      if (scopeType.stringValue.equals(stringValue)) {
        return scopeType;
      }
    }
    throw new IllegalArgumentException("Unknown scope type string value: " + stringValue);
  }
}
