// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.type;

import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * High-level classification of an attachment by its MIME type, backing the attachments UI tabs.
 *
 * <p>{@link #IMAGES} and {@link #VIDEOS} each match a specific MIME type prefix (image/, video/);
 * {@link #DOCUMENTS} is their complement, i.e. every other MIME type (including audio). The prefix
 * that defines each category lives here so the classification has a single source of truth, shared
 * with the persistence layer that builds the filter predicate.
 */
public enum MimeTypeCategory {
  IMAGES("image/"),
  VIDEOS("video/"),
  DOCUMENTS(null);

  @Nullable private final String mimeTypePrefix;

  MimeTypeCategory(@Nullable String mimeTypePrefix) {
    this.mimeTypePrefix = mimeTypePrefix;
  }

  /**
   * MIME type prefix that defines this category, or {@code null} when the category is the
   * complement of all prefixed categories (i.e. {@link #DOCUMENTS}).
   *
   * @return the MIME type prefix, or {@code null} for the complement category
   */
  @Nullable
  public String mimeTypePrefix() {
    return mimeTypePrefix;
  }

  /**
   * Prefixes of every category that matches a specific MIME type prefix. A MIME type belongs to the
   * complement category ({@link #DOCUMENTS}) when it does not start with any of these prefixes.
   *
   * @return immutable list of the prefixes of all prefixed categories
   */
  public static List<String> prefixedCategoryPrefixes() {
    return Arrays.stream(values())
        .map(category -> category.mimeTypePrefix)
        .filter(Objects::nonNull)
        .toList();
  }
}
