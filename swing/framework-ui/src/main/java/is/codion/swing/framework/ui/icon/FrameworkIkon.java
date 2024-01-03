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

import org.kordamp.ikonli.Ikon;

enum FrameworkIkon implements Ikon {

  FILTER("fr-filter", '\uf14b'),
  SEARCH("fr-search", '\uf16c'),
  ADD("fr-add", '\uf17f'),
  DELETE("fr-delete", '\uf204'),
  UPDATE("fr-update", '\uf1ac'),
  COPY("fr-copy", '\uf180'),
  REFRESH("fr-refresh", '\uf1a5'),
  CLEAR("fr-clear", '\uf18e'),
  UP("fr-up", '\uf10c'),
  DOWN("fr-down", '\uf109'),
  DETAIL("fr-detail", '\uf18a'),
  PRINT("fr-print", '\uf19f'),
  EDIT("fr-edit", '\uf184'),
  SUMMARY("fr-summary", 'Σ'),
  EDIT_PANEL("fr-edit-panel", '\uf168'),
  DEPENDENCIES("fr-dependencies", '\uf1ad'),
  SETTINGS("fr-settings", '\uf214'),
  CALENDAR("fr-calendar", '\uf124'),
  EDIT_TEXT("fr-edit-text", '\uf129'),
  LOGO("fr-logo", '\uf208');

  private final String description;
  private final char code;

  FrameworkIkon(String description, char code) {
    this.description = description;
    this.code = code;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public int getCode() {
    return code;
  }
}
