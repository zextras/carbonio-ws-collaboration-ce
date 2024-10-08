// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.model.FileResponse;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.ImageQualityEnumDto;
import com.zextras.carbonio.chats.model.ImageShapeEnumDto;
import com.zextras.carbonio.chats.model.ImageTypeEnumDto;
import com.zextras.carbonio.preview.queries.enums.Format;
import com.zextras.carbonio.preview.queries.enums.Quality;
import com.zextras.carbonio.preview.queries.enums.Shape;
import io.vavr.control.Option;
import java.util.UUID;

/**
 * This service is used to retrieve the preview of an attachment
 */
public interface PreviewService extends HealthIndicator {

  /**
   * Get the preview of an image
   *
   * @param user the user trying to access the preview attachment
   * @param fileId identifier of attachment file to preview {@link UUID}
   * @param area area ot preview in format widthXheight
   * @param quality the quality of the preview {@link Quality}
   * @param outputFormat the format of the preview {@link Format}
   * @param crop if true will crop borders, otherwise will fill them
   * @return the preview requested with necessary data {@link FileResponse}
   */
  FileResponse getImage(UserPrincipal user, UUID fileId, String area, Option<ImageQualityEnumDto> quality, Option<ImageTypeEnumDto> outputFormat, Option<Boolean> crop);

  /**
   * Get the thumbnail of an image
   *
   * @param user the user trying to access the preview attachment
   * @param fileId identifier of attachment file to preview {@link UUID}
   * @param area area ot preview in format widthXheight
   * @param quality the quality of the preview {@link Quality}
   * @param outputFormat the format of the preview {@link Format}
   * @param shape rounded or rectangular are supported {@link Shape}
   * @return the preview requested with necessary data {@link FileResponse}
   */
  FileResponse getImageThumbnail(UserPrincipal user, UUID fileId, String area, Option<ImageQualityEnumDto> quality, Option<ImageTypeEnumDto> outputFormat, Option<ImageShapeEnumDto> shape);

  /**
   * Get the preview of a pdf
   * @param user the user trying to access the preview attachment
   * @param fileId identifier of attachment file to preview {@link UUID}
   * @param firstPage the first page of the pdf to use for the preview
   * @param lastPage the last page of the pdf to use for the preview, 0 means all remaining pages
   * @return the preview requested with necessary data {@link FileResponse}
   */
  FileResponse getPDF(UserPrincipal user, UUID fileId, Integer firstPage, Integer lastPage);

  /**
   * Get the thumbnail of a pdf
   *
   * @param user the user trying to access the preview attachment
   * @param fileId identifier of attachment file to preview {@link UUID}
   * @param area area ot preview in format widthXheight
   * @param quality the quality of the preview {@link Quality}
   * @param outputFormat the format of the preview {@link Format}
   * @param shape rounded or rectangular are supported {@link Shape}
   * @return the preview requested with necessary data {@link FileResponse}
   */
  FileResponse getPDFThumbnail(UserPrincipal user, UUID fileId, String area, Option<ImageQualityEnumDto> quality, Option<ImageTypeEnumDto> outputFormat, Option<ImageShapeEnumDto> shape);
}
