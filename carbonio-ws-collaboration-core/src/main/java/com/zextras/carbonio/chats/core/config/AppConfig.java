// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import com.zextras.carbonio.chats.core.config.impl.AppConfigType;
import java.util.Optional;
import javax.annotation.Nullable;

@SuppressWarnings("unchecked")
public abstract class AppConfig {

  private AppConfig next;

  /**
   * Loads all configurations and saves them in cache
   */
  public abstract AppConfig load();

  /**
   * Retrieve a boolean value which indicates if the configuration is loaded
   *
   * @return true if the configuration is loaded, false otherwise
   */
  public abstract boolean isLoaded();

  /**
   * Retrieves the specified config or returns an empty {@link  Optional}
   *
   * @param clazz      the configuration parameter class (es. {@link Integer}, {@link Boolean} or {@link String})
   * @param configName configName   the configuration name
   * @param <T>        the configuration parameter type
   * @return an {@link Optional} which contains the configuration, if found
   */
  public <T> Optional<T> get(Class<T> clazz, ConfigName configName) {
    return getConfigByImplementation(clazz, configName).or(() -> {
      if (next != null) {
        return next.get(clazz, configName);
      } else {
        return Optional.empty();
      }
    });
  }

  /**
   * Returns the configured value type or an empty optional if it was not found
   *
   * @param clazz      the configuration parameter class (es. {@link Integer}, {@link Boolean} or {@link String})
   * @param configName the configuration name
   * @param <T>        the configuration parameter type
   * @return an {@link Optional} which contains the configuration, if found
   */
  protected abstract <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigName configName);

  /**
   * Sets the configuration in the first chain node
   *
   * @param configName configuration key {@link ConfigName}
   * @param value      configuration value
   */
  public AppConfig set(ConfigName configName, String value) {
    if (!setConfigByImplementation(configName, value) && next != null) {
      next.set(configName, value);
    }
    return this;
  }

  /**
   * Sets the configuration and returns a boolean value which indicates if is set
   *
   * @param configName configuration key {@link ConfigName}
   * @param value      configuration value
   * @return true if the configuration is set, false otherwise
   */
  protected abstract boolean setConfigByImplementation(ConfigName configName, String value);

  /**
   * Return the configuration type {@link AppConfigType}
   *
   * @return configuration type {@link AppConfigType}
   */
  public abstract AppConfigType getType();

  /**
   * Adds a new configuration resolver to the last position in the chain
   *
   * @param newConfigResolver the new configuration resolver {@link AppConfig}
   * @return itself
   */
  public AppConfig add(@Nullable AppConfig newConfigResolver) {
    if (newConfigResolver != null && newConfigResolver.isLoaded()) {
      if (next == null) {
        next = newConfigResolver;
      } else {
        next.add(newConfigResolver);
      }
    }
    return this;
  }

  protected <T> T castToGeneric(Class<T> clazz, String stringValue) {
    if (clazz.equals(String.class)) {
      return (T) stringValue;
    } else if (clazz.equals(Boolean.class)) {
      return (T) Boolean.valueOf(stringValue);
    } else if (clazz.equals(Integer.class)) {
      return (T) Integer.valueOf(stringValue);
    } else if (clazz.equals(Double.class)) {
      return (T) Double.valueOf(stringValue);
    } else {
      throw new RuntimeException("Missing support for " + clazz.getSimpleName());
    }
  }

  /**
   * Gets the configuration resolver by type
   *
   * @param type configuration resolver type to search {@link AppConfigType}
   * @return an {@link Optional} which contains the configuration resolver, if found
   */
  public Optional<AppConfig> getFromTypes(AppConfigType type) {
    if (type.equals(getType())) {
      return Optional.of(this);
    }
    if (next != null) {
      return next.getFromTypes(type);
    }
    return Optional.empty();
  }
}