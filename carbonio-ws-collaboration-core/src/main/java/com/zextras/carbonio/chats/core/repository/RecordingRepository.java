// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Recording;

public interface RecordingRepository {

  /**
   * Inserts a new recording
   *
   * @param recording recording to insert
   * @return recording inserted {@link Recording}
   */
  Recording insert(Recording recording);

  /**
   * Updates a recording
   *
   * @param recording recording to update
   * @return recording updated {@link Recording}
   */
  Recording update(Recording recording);

  /**
   * Deletes a recording
   *
   * @param recording recording to delete
   */
  void delete(Recording recording);
}
