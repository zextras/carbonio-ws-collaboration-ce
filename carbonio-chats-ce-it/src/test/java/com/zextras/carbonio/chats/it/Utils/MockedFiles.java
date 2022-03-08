package com.zextras.carbonio.chats.it.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MockedFiles {

  private static final UUID                          snoopyId          = UUID.randomUUID();
  private static final Map<MockedFileType, FileMock> mapMockedFile     = Map.of(
    MockedFileType.PEANUTS_IMAGE,
    FileMock.create().id(UUID.randomUUID()).size(33786L).mimeType("image/jpg").name("peanuts.jpg"),
    MockedFileType.PEANUTS_LARGE_IMAGE,
    FileMock.create().id(UUID.randomUUID()).size(2664054L).mimeType("image/bmp").name("peanuts.bmp"),
    MockedFileType.PEANUTS_PDF,
    FileMock.create().id(UUID.randomUUID()).size(81694L).mimeType("application/pdf").name("peanuts.pdf"),
    MockedFileType.SNOOPY_IMAGE, FileMock.create().id(snoopyId).size(13705).mimeType("image/jpg").name("snoopy.jpg")
  );
  private static final Map<MockedFileType, FileMock> mapMockedPreviews = Map.of(
    MockedFileType.SNOOPY_PREVIEW,
    FileMock.create().id(snoopyId).size(4408).mimeType("image/jpg").name("snoopy-preview.jpg")
  );

  public static List<FileMock> getMockedFiles() {
    return new ArrayList<>(mapMockedFile.values());
  }

  public static List<FileMock> getMockedPreviews() {
    return new ArrayList<>(mapMockedPreviews.values());
  }

  public static FileMock get(MockedFileType type) {
    return mapMockedFile.get(type);
  }

  public static FileMock getPreview(MockedFileType type) {
    return mapMockedPreviews.get(type);
  }

  public enum MockedFileType {
    PEANUTS_IMAGE,
    PEANUTS_LARGE_IMAGE,
    PEANUTS_PDF,
    SNOOPY_IMAGE,
    SNOOPY_PREVIEW
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
