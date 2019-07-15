package org.eclipse.aether.internal.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.layout.RepositoryLayout;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.transfer.NoRepositoryLayoutException;
import org.eclipse.aether.util.ChecksumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.murkaje.ChecksumProvider;
import com.github.murkaje.ChecksumValidator;

/* In special package to extend package-private class
 * Otherwise would need to copy the whole thing to specialize it
 * No other injection points either as org.eclipse.aether.internal.impl.DefaultArtifactResolver.isLocallyInstalled is private
 */
public class ValidatingLocalRepositoryManager extends EnhancedLocalRepositoryManager {

  private static final Logger log = LoggerFactory.getLogger("Robust-Local-Cache");

  private RepositoryLayoutFactory repositoryLayoutFactory;

  private TransporterProvider transporterProvider;

  public ValidatingLocalRepositoryManager(File basedir, RepositorySystemSession session, RepositoryLayoutFactory repositoryLayoutFactory,
                                          TransporterProvider transporterProvider) {
    super(basedir, session);
    this.repositoryLayoutFactory = repositoryLayoutFactory;
    this.transporterProvider = transporterProvider;
  }

  @Override
  public LocalArtifactResult find(RepositorySystemSession session, LocalArtifactRequest request) {
    LocalArtifactResult result = super.find(session, request);

    File localFile = result.getFile();
    if (result.isAvailable() && localFile != null && localFile.isFile()) {

      URI artifactUri = getArtifactUri(session, request, result);
      if (artifactUri == null) return result;

      ChecksumProvider checksumProvider = (String algorithm) -> {
        String extension = algorithm.replace("-", "").toLowerCase(Locale.ENGLISH);
        URI checksumLocation = URI.create(artifactUri.toString() + "." + extension);
        File tmp = File.createTempFile("checksum-" + request.getArtifact().getArtifactId(), "." + extension);
        try {
          //TODO: Add regular maven transport listener? some logging is in validator if remote checksum downloaded
          transporterProvider.newTransporter(session, result.getRepository()).get(new GetTask(checksumLocation).setDataFile(tmp));
          return ChecksumUtils.read(tmp);
        }
        catch (Exception e) {
          if (e instanceof IOException) {
            throw (IOException) e;
          }
          else {
            throw new IOException(e);
          }
        }
        finally {
          tmp.delete();
        }
      };

      ChecksumValidator validator = new ChecksumValidator(localFile.toPath(), checksumProvider);
      if (!validator.isValid()) {
        // Both need to be set, see org.eclipse.aether.internal.impl.DefaultArtifactResolver.isLocallyInstalled
        result.setAvailable(false);
        result.setFile(null);
      }
    }

    return result;
  }

  private URI getArtifactUri(RepositorySystemSession session, LocalArtifactRequest request, LocalArtifactResult result) {
    Artifact artifact = request.getArtifact();
    try {
      RemoteRepository remoteRepository = result.getRepository();
      if (remoteRepository == null) return null;
      RepositoryLayout repositoryLayout = repositoryLayoutFactory.newInstance(session, remoteRepository);
      return repositoryLayout.getLocation(artifact, false);
    }
    catch (NoRepositoryLayoutException e) {
      log.warn("Can't determine artifact URI, skipping checksum validation", e);
      return null;
    }
  }
}
