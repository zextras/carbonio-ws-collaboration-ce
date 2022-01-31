// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot;


import com.zextras.carbonio.chats.core.config.AppConfig;
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

    Server server = new Server(appConfig.get(Integer.class, "SERVER_PORT").orElse(8081));
    ServletContextHandler servletHandler = new ServletContextHandler(server,
      CONTEXT_PATH);
    servletHandler.addEventListener(resteasyListener);

    ServletHolder sh = new ServletHolder(HttpServletDispatcher.class);

    servletHandler.addServlet(sh, "/*");
    server.setHandler(servletHandler);

    server.start();
    server.join();
  }

}
