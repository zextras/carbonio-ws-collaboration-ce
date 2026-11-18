// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.ReadMarker;
import java.util.List;

/** The read markers of a room with the visibility floor of the requester they are clamped to. */
public record ReadMarkersView(List<ReadMarker> markers, VisibilityFloor floor) {}
