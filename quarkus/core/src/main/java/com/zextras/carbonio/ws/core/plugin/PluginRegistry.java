// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.plugin;

import com.zextras.carbonio.ws.plugin.api.Plugin;
import com.zextras.carbonio.ws.plugin.api.PluginApi;
import com.zextras.carbonio.ws.plugin.api.PluginDescriptor;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PluginRegistry {

  private static final Logger LOG = Logger.getLogger(PluginRegistry.class);

  @Inject Instance<Plugin> plugins;

  private volatile List<PluginDescriptor> loaded = List.of();

  void onStart(@Observes @Priority(10) StartupEvent event) {
    loaded = validate(plugins.stream().map(Plugin::descriptor).toList());
    loaded.forEach(d -> LOG.infof("Loaded plugin %s %s (API %s)", d.id(), d.version(), d.requiredApiVersion()));
  }

  public List<PluginDescriptor> loaded() {
    return loaded;
  }

  static List<PluginDescriptor> validate(Collection<PluginDescriptor> descriptors) {
    Set<String> ids = new HashSet<>();
    for (PluginDescriptor d : descriptors) {
      if (!ids.add(d.id())) {
        throw new IllegalStateException(String.format("Duplicate plugin id '%s'", d.id()));
      }
      if (!PluginApi.isCompatible(d.requiredApiVersion(), PluginApi.VERSION)) {
        throw new IllegalStateException(String.format(
            "Plugin '%s' %s requires plugin API %s but core provides %s",
            d.id(), d.version(), d.requiredApiVersion(), PluginApi.VERSION));
      }
    }
    return descriptors.stream().sorted(Comparator.comparing(PluginDescriptor::id)).toList();
  }
}
