package com.zextras.carbonio.chats.it.tools;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class CloseablePostgreSQLContainer<SELF extends PostgreSQLContainer<SELF>> extends
  PostgreSQLContainer<SELF> implements CloseableResource {

  public CloseablePostgreSQLContainer(String dockerImageName) {
    super(dockerImageName);
  }

  public CloseablePostgreSQLContainer(DockerImageName dockerImageName) {
    super(dockerImageName);
  }

  @Override
  public void close() {
    ChatsLogger.debug("Closing test db...");
    super.stop();
  }
}
