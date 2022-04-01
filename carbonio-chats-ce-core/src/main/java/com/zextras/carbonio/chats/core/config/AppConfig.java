package com.zextras.carbonio.chats.core.config;

import java.util.Optional;

@SuppressWarnings("unchecked")
public abstract class AppConfig {

  private AppConfig next;

  /**
   * Retrieves the specified config or returns an empty {@link  Optional}
   *
   * @param clazz      the configuration parameter class (es. {@link Integer}, {@link Boolean} or {@link String})
   * @param configName configName   the configuration name
   * @param <T>        the configuration parameter type
   * @return an {@link Optional} which contains the configuration, if found
   */
  public <T> Optional<T> get(Class<T> clazz, ConfigValue configName) {
    return getConfigByImplementation(clazz, configName).or(() -> {
      if (next != null) {
        return next.getConfigByImplementation(clazz, configName);
      } else {
        return Optional.empty();
      }
    });
  }

  /**
   * Retrieves the environment type the application is running in. The default is {@link EnvironmentType#PRODUCTION}
   *
   * @return the current {@link EnvironmentType}
   */
  public EnvironmentType getEnvType() {
    return getEnvTypeByImplementation().orElseGet(
      () -> Optional.ofNullable(next).flatMap(AppConfig::getEnvTypeByImplementation)
        .orElse(EnvironmentType.PRODUCTION)
    );
  }

  /**
   * Returns the configured value type or an empty optional if it was not found
   *
   * @param clazz      the configuration parameter class (es. {@link Integer}, {@link Boolean} or {@link String})
   * @param configName the configuration name
   * @param <T>        the configuration parameter type
   * @return an {@link Optional} which contains the configuration, if found
   */
  protected abstract <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigValue configName);

  /**
   * Returns the configured environment type or an empty optional if it was not found. N.B. The returned {@link
   * Optional} should always be empty if the configuration was not found.
   *
   * @return an {@link  Optional} which contains the environment type if it was found
   */
  protected abstract Optional<EnvironmentType> getEnvTypeByImplementation();

  /**
   * Adds an alternative {@link AppConfig} to use to read the attribute from if it was not found in this one. This is
   * used to compose a chain-of-responsibility for configurations.
   *
   * @param nextConfigResolver the alternative {@link AppConfig} to use
   * @return the given {@link AppConfig} parameter to allow for method chaining
   */
  public AppConfig or(AppConfig nextConfigResolver) {
    this.next = nextConfigResolver;
    return this.next;
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


}
