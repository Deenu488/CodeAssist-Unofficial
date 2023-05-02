package com.tyron.resolver;

import java.io.IOException;

public enum ScopeType {
  API("api"),
  IMPLEMENTATION("implementation"),
  COMPILE_ONLY("compileOnly"),
  COMPILE_ONLY_API("compileOnlyApi"),
  RUNTIME_ONLY("runtimeOnly"),
  RUNTIME_ONLY_API("runtimeOnlyApi");

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
    try {
      throw new IOException("Unknown scope type string value: " + stringValue);
    } catch (IOException e) {
    }
    return null;
  }
}
