package com.github.murkaje;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.aether.util.ChecksumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DigestChecksumValidator implements ChecksumValidator {

  private static final Logger log = LoggerFactory.getLogger("Robust-Local-Cache");

  public static ChecksumValidator get(Path file, ChecksumProvider checksumProvider) {
    for (String algorithm : Arrays.asList("SHA-1", "MD5")) {
      Optional<ChecksumValidator> validator = getLocal(file, algorithm);
      if (validator.isPresent()) return validator.get();

      validator = getRemote(file, algorithm, checksumProvider);
      if (validator.isPresent()) return validator.get();
    }
    return new NoOpChecksumValidator();
  }

  private static Optional<ChecksumValidator> getLocal(Path file, String algorithm) {
    String extension = algorithm.replace("-", "").toLowerCase(Locale.ENGLISH);
    File checksumFile = new File(file.toString() + "." + extension);
    if (!checksumFile.isFile()) return Optional.empty();

    try {
      String expected = ChecksumUtils.read(checksumFile);
      return Optional.of(new DigestChecksumValidator(file, MessageDigest.getInstance(algorithm), expected));
    }
    catch (IOException e) {
      log.warn("Failed to read local checksum file, skipping validation", e);
    }
    catch (NoSuchAlgorithmException e) {
      log.warn("Can't find checksum algorithm, skipping local artifact validation", e);
    }

    return Optional.empty();
  }

  private static Optional<ChecksumValidator> getRemote(Path file, String algorithm, ChecksumProvider checksumProvider) {
    try {
      String expected = checksumProvider.getChecksum(algorithm);
      if (expected == null) return Optional.empty();

      return Optional.of(new DigestChecksumValidator(file, MessageDigest.getInstance(algorithm), expected));
    }
    catch (IOException e) {
      log.warn("Failed to read remote checksum file, skipping validation", e);
    }
    catch (NoSuchAlgorithmException e) {
      log.warn("Can't find checksum algorithm, skipping local artifact validation", e);
    }

    return Optional.empty();
  }

  private final Path localFile;
  private final MessageDigest messageDigest;
  private final String expected;

  private DigestChecksumValidator(Path file, MessageDigest digest, String expected) {
    this.localFile = file;
    this.messageDigest = digest;
    this.expected = expected;
  }

  @Override
  public boolean isValid() {
    ByteBuffer buffer = ByteBuffer.allocate(8192);

    try (FileChannel fc = FileChannel.open(localFile)) {
      while (fc.read(buffer) != -1) {
        buffer.flip();
        messageDigest.update(buffer);
        buffer.clear();
      }
    }
    catch (IOException e) {
      log.warn("Failed to read artifact '{}' for checksum validation", localFile, e);
      return false;
    }

    String actual = ChecksumUtils.toHexString(messageDigest.digest());
    boolean matches = expected.equals(actual);

    if (!matches) {
      log.warn("Local artifact '{}' {} checksum mismatch, expected '{}', was '{}'", localFile, messageDigest.getAlgorithm(), expected, actual);
    }

    return matches;
  }
}
