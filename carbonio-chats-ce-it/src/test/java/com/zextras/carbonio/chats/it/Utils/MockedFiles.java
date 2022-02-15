package com.zextras.carbonio.chats.it.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class MockedFiles {

  private static final List<FileMock> mockedFiles = List.of(
    FileMock.create().id("a6b89d72-dd02-438e-8fcd-4a6639c26269").size(37863L).mimeType("image/jpg").name("test-1.jpg")
  );

  public static List<FileMock> getMockedFiles() {
    return mockedFiles;
  }

  public static FileMock getImages() {
    return mockedFiles.stream().filter(file -> file.mimeType.startsWith("image/")).findFirst().orElse(null);
  }


  public static class FileMock {

    private String id;
    private long   size;
    private String mimeType;
    private String name;

    public static FileMock create() {
      return new FileMock();
    }

    public String getId() {
      return id;
    }

    public FileMock id(String id) {
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
