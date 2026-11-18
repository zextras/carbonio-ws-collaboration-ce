// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

/** A timeline page with what is needed to render it for one room member. */
public record TimelineView(TimelinePage page, MessageContext context, ReadMarkersView markers) {}
