package com.zextras.carbonio.chats.core.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.impl.AppConfigType;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
public class AppConfigProviderTest {

  @Test
  @DisplayName("Correctly sets all configurations resolvers")
  public void setsAllConfigurationsResolvers() throws Exception {
    String folder = Files.createTempDirectory("temp").toString();
    String environmentFilePath = createEnvironmentFile(folder + "/.env", Map.of(
      "CONSUL_HOST", "localhost",
      "CONSUL_PORT", "0",
      "CONSUL_HTTP_TOKEN", "TOKEN"
    ));
    String propertiesFilePath = createPropertiesFile(folder + "/config.properties", Map.of(
      "carbonio.storages.host", "localhost",
      "carbonio.storages.port", "8081"
    ));

    ConsulClient consulClient = mock(ConsulClient.class);
    when(consulClient.getKVValues("carbonio-chats/", "TOKEN"))
      .thenReturn(new Response<>(getConsulConfigValues(Map.of(
        "carbonio-chats/db-username", Base64.getEncoder().encodeToString("username".getBytes(StandardCharsets.UTF_8)))
      ), 0L, true, 0L));

    AppConfigProvider appConfigProvider = new AppConfigProvider(environmentFilePath, propertiesFilePath, consulClient);

    AppConfig appConfig = appConfigProvider.get();
    assertNotNull(appConfig);

    assertTrue(appConfig.getFromTypes(AppConfigType.ENVIRONMENT).isPresent());
    assertTrue(appConfig.getFromTypes(AppConfigType.PROPERTY).isPresent());
    assertTrue(appConfig.getFromTypes(AppConfigType.CONSUL).isPresent());

    removeFolder(folder);
  }

  @Test
  @DisplayName("Correctly sets configurations resolvers without properties")
  public void setsConfigurationsResolversWithoutProperties() throws Exception {
    String folder = Files.createTempDirectory("temp").toString();
    String environmentFilePath = createEnvironmentFile(folder + "/.env", Map.of(
      "CONSUL_HOST", "localhost",
      "CONSUL_PORT", "0",
      "CONSUL_HTTP_TOKEN", "TOKEN"
    ));
    ConsulClient consulClient = mock(ConsulClient.class);
    when(consulClient.getKVValues("carbonio-chats/", "TOKEN"))
      .thenReturn(new Response<>(getConsulConfigValues(Map.of(
        "carbonio-chats/db-username", Base64.getEncoder().encodeToString("username".getBytes(StandardCharsets.UTF_8)))
      ), 0L, true, 0L));

    AppConfigProvider appConfigProvider = new AppConfigProvider(environmentFilePath, "/fake/config.properties",
      consulClient);

    AppConfig appConfig = appConfigProvider.get();
    assertNotNull(appConfig);

    assertTrue(appConfig.getFromTypes(AppConfigType.ENVIRONMENT).isPresent());
    assertFalse(appConfig.getFromTypes(AppConfigType.PROPERTY).isPresent());
    assertTrue(appConfig.getFromTypes(AppConfigType.CONSUL).isPresent());

    removeFolder(folder);
  }

  @Test
  @DisplayName("Correctly sets configurations resolvers without consul")
  public void setsConfigurationsResolversWithoutConsul() throws Exception {
    String folder = Files.createTempDirectory("temp").toString();
    String environmentFilePath = createEnvironmentFile(folder + "/.env", Map.of(
      "CONSUL_HOST", "localhost",
      "CONSUL_PORT", "0",
      "CONSUL_HTTP_TOKEN", "TOKEN"
    ));

    String propertiesFilePath = createPropertiesFile(folder + "/config.properties", Map.of(
      "carbonio.storages.host", "localhost",
      "carbonio.storages.port", "8081"
    ));

    AppConfigProvider appConfigProvider = new AppConfigProvider(environmentFilePath, propertiesFilePath);

    AppConfig appConfig = appConfigProvider.get();
    assertNotNull(appConfig);

    assertTrue(appConfig.getFromTypes(AppConfigType.ENVIRONMENT).isPresent());
    assertTrue(appConfig.getFromTypes(AppConfigType.PROPERTY).isPresent());
    assertFalse(appConfig.getFromTypes(AppConfigType.CONSUL).isPresent());

    removeFolder(folder);
  }

  private String createEnvironmentFile(String path, Map<String, String> configs) throws Exception {
    StringBuilder sb = new StringBuilder();
    configs.forEach((key, value) -> sb.append(String.join("=", key, value)).append("\n"));
    createTempFile(path, sb.toString());
    return path;
  }

  private String createPropertiesFile(String path, Map<String, String> configs) throws Exception {
    StringBuilder sb = new StringBuilder();
    configs.forEach((key, value) -> sb.append(String.join("=", key, value)).append("\n"));
    return createTempFile(path, sb.toString());
  }

  private List<GetValue> getConsulConfigValues(Map<String, String> configs) {
    List<GetValue> result = new ArrayList<>();
    configs.forEach((k, v) -> {
      GetValue value = new GetValue();
      value.setKey(k);
      value.setValue(v);
      result.add(value);
    });
    return result;
  }

  private String createTempFile(String path, String content) throws Exception {
    File file = new File(path);
    FileWriter writer = new FileWriter(file);
    writer.write(content);
    writer.close();
    return file.getAbsolutePath();
  }

  private void removeFolder(String path) throws Exception {
    Files.walk(Paths.get(path))
      .sorted(Comparator.reverseOrder())
      .map(Path::toFile)
      .forEach(File::delete);
  }

}
