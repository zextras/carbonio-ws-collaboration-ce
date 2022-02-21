package com.zextras.carbonio.chats.it.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MockedFiles {

  private static final List<FileMock> mockedFiles = List.of(
    FileMock.create().id(UUID.randomUUID()).size(33786L).mimeType("image/jpg").name("peanuts.jpg"),
    FileMock.create().id(UUID.randomUUID()).size(13885L).mimeType("image/jpg").name("snoopy.jpg"),
    FileMock.create().id(UUID.randomUUID()).size(4831L).mimeType("image/jpg").name("charlie-brown.jpg"),
    FileMock.create().id(UUID.randomUUID()).size(4119L).mimeType("image/jpg").name("lucy-van-pelt.jpg"),
    FileMock.create().id(UUID.randomUUID()).size(7053L).mimeType("image/jpg").name("linus-van-pelt.jpg"),
    FileMock.create().id(UUID.randomUUID()).size(6874L).mimeType("image/jpg").name("peperita-patty.jpg"),
    FileMock.create().id(UUID.randomUUID()).size(4838L).mimeType("image/jpg").name("marcie-johnson.jpg"),
    FileMock.create().id(UUID.randomUUID()).size(6699L).mimeType("image/jpg").name("schroeder.jpg")
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

  public static List<FileMock> getRandomFileList(int listSize) {
    if (listSize < 0 || listSize > mockedFiles.size()) {
      throw new IndexOutOfBoundsException();
    }
    int index = new Random().nextInt(Integer.MAX_VALUE);
    List<FileMock> list = new ArrayList<>(listSize);
    IntStream.range(0, listSize).forEach(i -> list.add(mockedFiles.get((index + i) % mockedFiles.size())));
    return list;
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
