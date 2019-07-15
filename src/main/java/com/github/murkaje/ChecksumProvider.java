package com.github.murkaje;

import java.io.IOException;

public interface ChecksumProvider {
  String getChecksum(String algorithm) throws IOException;
}
