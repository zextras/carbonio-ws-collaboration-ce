package com.zextras.team.boot;


import com.zextras.team.core.utils.EnvConfig;
import javax.inject.Inject;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

public class Boot {

  private final EnvConfig                                    envConfig;
  private final GuiceResteasyBootstrapServletContextListener resteasyListener;

  @Inject
  public Boot(EnvConfig envConfig, GuiceResteasyBootstrapServletContextListener resteasyListener) {
    this.envConfig = envConfig;
    this.resteasyListener = resteasyListener;
  }

  public void boot() throws Exception {
    Server server = new Server(envConfig.getInt("server.port"));
    ServletContextHandler servletHandler = new ServletContextHandler(server, envConfig.get("servlet.context-root"));
    servletHandler.addEventListener(resteasyListener);

    ServletHolder sh = new ServletHolder(HttpServletDispatcher.class);

    servletHandler.addServlet(sh, "/*");
    server.setHandler(servletHandler);

    server.start();
    server.join();
  }

}
