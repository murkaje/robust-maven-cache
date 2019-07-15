package com.github.murkaje;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.ValidatingLocalRepositoryManager;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ValidatingLocalRepositoryManagerFactory implements LocalRepositoryManagerFactory {

  private static final Logger log = LoggerFactory.getLogger("Robust-Local-Cache");

  @Inject
  private RepositoryLayoutFactory repositoryLayoutFactory;

  @Inject
  private TransporterProvider transporterProvider;

  @Override
  public LocalRepositoryManager newInstance(RepositorySystemSession repositorySystemSession, LocalRepository localRepository) throws NoLocalRepositoryManagerException {
    log.info("Enabling local artifact checksum validation");
    if ("".equals(localRepository.getContentType()) || "default".equals(localRepository.getContentType())) {
      return new ValidatingLocalRepositoryManager(localRepository.getBasedir(), repositorySystemSession, repositoryLayoutFactory, transporterProvider);
    }
    else {
      throw new NoLocalRepositoryManagerException(localRepository);
    }
  }

  @Override
  public float getPriority() {
    return 21;
  }
}
