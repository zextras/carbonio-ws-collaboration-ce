package com.zextras.team.boot;

import com.google.inject.Guice;
import com.zextras.team.boot.config.BootModule;
import java.util.Properties;

public class Team {

  public static void main(String[] args) throws Exception {
    Properties properties = new Properties();
    properties.load(Team.class.getClassLoader().getResourceAsStream("team.properties"));
    Guice.createInjector(new BootModule(properties)).getInstance(Boot.class).boot();
  }

}
