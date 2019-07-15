package com.github.murkaje;

public class NoOpChecksumValidator implements ChecksumValidator {
  @Override
  public boolean isValid() {
    return true;
  }
}
