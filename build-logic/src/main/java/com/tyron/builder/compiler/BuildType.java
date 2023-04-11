package com.tyron.builder.compiler;

import java.io.IOException;

public enum BuildType {
  RELEASE("RELEASE"),
  DEBUG("DEBUG"),
  AAB("AAB");

  private final String stringValue;

  BuildType(String stringValue) {
    this.stringValue = stringValue;
  }

  public String getStringValue() {
    return stringValue;
  }

  public static BuildType fromStringValue(String stringValue) {
    for (BuildType buildType : BuildType.values()) {
      if (buildType.stringValue.equals(stringValue)) {
        return buildType;
      }
    }
    try {
      throw new IOException("Unknown build type string value: " + stringValue);
    } catch (IOException e) {
    }
    return null;
  }
}
