// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

/**
 * This enum represents all possible values for the audio codec usable on the VideoServer.
 */
public enum AudioCodec {
  OPUS,
  G722,
  PCMU,
  PCMA,
  ISAC32,
  ISAC16
}
