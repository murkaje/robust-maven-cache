package com.github.murkaje;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;

@Named
public class ConnectorConfigurationHook extends AbstractMavenLifecycleParticipant {

  private static final String RESUME_DOWNLOADS = "aether.connector.resumeDownloads";

  @Inject
  private Logger logger;

  @Override
  public void afterSessionStart(MavenSession session) {
    RepositorySystemSession repo = session.getRepositorySession();
    if (repo instanceof DefaultRepositorySystemSession) {
      DefaultRepositorySystemSession defaultRepo = (DefaultRepositorySystemSession) repo;
      if (!defaultRepo.getConfigProperties().containsKey(RESUME_DOWNLOADS)) {
        logger.debug("Setting aether.connector.resumeDownloads = false");
        defaultRepo.setConfigProperty(RESUME_DOWNLOADS, "false");
      }
    }
  }
}
