// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot;


import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.net.InetSocketAddress;
import javax.inject.Inject;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.flywaydb.core.Flyway;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

public class Boot {

  private final String CONTEXT_PATH = "/chats";

  private final AppConfig                                    appConfig;
  private final GuiceResteasyBootstrapServletContextListener resteasyListener;
  private final Flyway                                       flyway;

  @Inject
  public Boot(
    AppConfig appConfig,
    GuiceResteasyBootstrapServletContextListener resteasyListener,
    Flyway flyway
  ) {
    this.appConfig = appConfig;
    this.resteasyListener = resteasyListener;
    this.flyway = flyway;
  }

  public void boot() throws Exception {
    flyway.migrate();

    if(appConfig.getEnvType().equals(EnvironmentType.DEVELOPMENT)) {
      ChatsLogger.warn("****** RUNNING IN DEVELOPMENT MODE! DO NOT USE IN PRODUCTION ENVIRONMENTS ******");
    }
    Server server = new Server(new InetSocketAddress(ChatsConstant.SERVER_HOST, ChatsConstant.SERVER_PORT));
    ServletContextHandler servletHandler = new ServletContextHandler(server, CONTEXT_PATH);
    servletHandler.addEventListener(resteasyListener);

    ServletHolder sh = new ServletHolder(HttpServletDispatcher.class);

    servletHandler.addServlet(sh, "/*");
    server.setHandler(servletHandler);

    server.start();
    server.join();
  }

}
