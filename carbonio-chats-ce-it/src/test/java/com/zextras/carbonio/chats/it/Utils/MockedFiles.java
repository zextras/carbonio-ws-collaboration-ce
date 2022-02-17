package com.zextras.carbonio.chats.it.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class MockedFiles {

  private static final List<FileMock> mockedFiles = List.of(
    FileMock.create().id(UUID.randomUUID()).size(33786L).mimeType("image/jpg").name("peanuts.jpg"),
    FileMock.create().id(UUID.randomUUID()).size(13885L).mimeType("image/jpg").name("snoopy.jpg")
  );

  public static List<FileMock> getMockedFiles() {
    return mockedFiles;
  }

  public static FileMock getRandomFile() {
    return mockedFiles.get(new Random().nextInt(Integer.MAX_VALUE) % mockedFiles.size());
  }

  public static FileMock getRandomImage() {
    List<FileMock> fileMocks = mockedFiles.stream().filter(file -> file.mimeType.startsWith("image/"))
      .collect(Collectors.toList());
    return fileMocks.get(new Random().nextInt(Integer.MAX_VALUE) % fileMocks.size());
  }

  public static class FileMock {

    private UUID   id;
    private long   size;
    private String mimeType;
    private String name;

    public static FileMock create() {
      return new FileMock();
    }

    public String getId() {
      return id.toString();
    }

    public UUID getUUID() {
      return id;
    }

    public FileMock id(UUID id) {
      this.id = id;
      return this;
    }

    public long getSize() {
      return size;
    }

    public FileMock size(long size) {
      this.size = size;
      return this;
    }

    public String getMimeType() {
      return mimeType;
    }

    public FileMock mimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    public String getName() {
      return name;
    }

    public FileMock name(String name) {
      this.name = name;
      return this;
    }

    public byte[] getFileBytes() throws IOException {
      return Objects.requireNonNull(getClass().getResourceAsStream(String.format("/files/%s", name))).readAllBytes();
    }
  }


}
