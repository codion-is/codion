/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.icon;

import org.kordamp.ikonli.AbstractIkonHandler;
import org.kordamp.ikonli.Ikon;

import java.io.InputStream;
import java.net.URL;

public final class FrameworkIkonHandler extends AbstractIkonHandler {

  private static final String FONT_RESOURCE = "is/codion/swing/framework/ui/icon/framework-icons.ttf";
  private static final String FONT_FAMILY = "codion-framework-icons";
  private static final String DESCRIPTION_PREFIX = "fr-";

  @Override
  public boolean supports(String description) {
    return description != null && description.startsWith(DESCRIPTION_PREFIX);
  }

  @Override
  public Ikon resolve(String description) {
    for (FrameworkIkon font : FrameworkIkon.values()) {
      if (font.getDescription().equals(description)) {
        return font;
      }
    }

    throw new IllegalArgumentException("Uknown icon description '" + description + "'");
  }

  @Override
  public URL getFontResource() {
    return FrameworkIkonHandler.class.getClassLoader().getResource(FONT_RESOURCE);
  }

  @Override
  public InputStream getFontResourceAsStream() {
    return FrameworkIkonHandler.class.getClassLoader().getResourceAsStream(FONT_RESOURCE);
  }

  @Override
  public String getFontFamily() {
    return FONT_FAMILY;
  }
}
