package com.github.murkaje;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.aether.util.ChecksumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChecksumValidator {

  private static final Logger log = LoggerFactory.getLogger("Robust-Local-Cache");

  public class Checksum {
    public final MessageDigest messageDigest;
    public final String expected;

    public Checksum(MessageDigest messageDigest, String expected) {
      this.messageDigest = messageDigest;
      this.expected = expected;
    }

    public boolean isValid() {
      String actual = ChecksumUtils.toHexString(messageDigest.digest());
      boolean matches = expected.equals(actual);

      if (!matches) {
        log.warn("Local artifact '{}' {} checksum mismatch, expected '{}', was '{}'", localFile, messageDigest.getAlgorithm(), expected, actual);
      }

      return matches;
    }
  }

  private List<Checksum> checksums = new ArrayList<>(2);
  private Path localFile;

  public ChecksumValidator(Path file, ChecksumProvider checksumProvider) {
    this.localFile = file;

    for (String algorithm : Arrays.asList("SHA-1", "MD5")) {
      String extension = algorithm.replace("-", "").toLowerCase(Locale.ENGLISH);
      File checksumFile = new File(file.toString() + "." + extension);
      try {
        if (checksumFile.isFile()) {
          try {
            String expected = ChecksumUtils.read(checksumFile);
            checksums.add(new Checksum(MessageDigest.getInstance(algorithm), expected));
            //TODO: Only checking one checksum file for now, otherwise all md5 checksums get downloaded and every build takes a long time
            return;
          }
          catch (IOException e) {
            log.warn("Failed to read local checksum file, skipping validation", e);
          }
        }
        else {
          log.info("Downloading remote checksum file for artifact '{}'", file);
          try {
            String expected = checksumProvider.getChecksum(algorithm);
            checksums.add(new Checksum(MessageDigest.getInstance(algorithm), expected));
            return;
          }
          catch (IOException e) {
            log.warn("Failed to download checksum file, skipping validation", e);
          }
        }
      }
      catch (NoSuchAlgorithmException e) {
        log.warn("Can't find checksum algorithm, skipping local artifact validation", e);
      }
    }
  }

  protected void calculateChecksum() {
    try {
      ByteBuffer buffer = ByteBuffer.allocate(8192);
      FileChannel fc = FileChannel.open(localFile);

      int read = fc.read(buffer);
      while (read != -1) {
        buffer.flip();
        buffer.mark();
        for (Checksum checksum : checksums) {
          buffer.reset();
          checksum.messageDigest.update(buffer);
        }
        buffer.clear();
        read = fc.read(buffer);
      }
    }
    catch (IOException e) {
      log.warn("Failed to read artifact for checksum validation", e);
    }
  }

  public boolean isValid() {
    calculateChecksum();

    for (Checksum checksum : checksums) {
      if (!checksum.isValid()) return false;
    }

    return true;
  }
}
