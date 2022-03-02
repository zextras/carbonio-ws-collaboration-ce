package com.zextras.carbonio.chats.it.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MockedFiles {

  private static final List<FileMock> mockedFiles = List.of(
    FileMock.create().id(UUID.randomUUID()).size(33786L).mimeType("image/jpg").name("peanuts.jpg"),
    FileMock.create().id(UUID.randomUUID()).size(13705).mimeType("image/jpg").name("snoopy.jpg"),
    FileMock.create().id(UUID.randomUUID()).size(2664054L).mimeType("image/bmp").name("peanuts.bmp"),
    FileMock.create().id(UUID.randomUUID()).size(81694L).mimeType("application/pdf").name("peanuts.pdf")
  );

  public static List<FileMock> getMockedFiles() {
    return mockedFiles;
  }

  public static FileMock getFile() {
    return mockedFiles.get(0);
  }

  public static FileMock getImage() {
    return mockedFiles.get(0);
  }

  public static FileMock getLargeImage() {
    return mockedFiles.get(2);
  }

  public static FileMock getPdf() {
    return mockedFiles.get(3);
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
