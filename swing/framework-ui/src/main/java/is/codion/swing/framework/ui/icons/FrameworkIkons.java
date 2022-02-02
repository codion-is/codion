/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import org.kordamp.ikonli.Ikon;

public enum FrameworkIkons implements Ikon {

  FILTER("fr-filter", '\uf14b'),
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
  SUMMARY("fr-summary", '\u03a3'),
  EDIT_PANEL("fr-edit-panel", '\uf168'),
  DEPENDENCIES("fr-dependencies", '\uf1ad');

  private final String description;
  private final char code;

  FrameworkIkons(final String description, final char code) {
    this.description = description;
    this.code = code;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public char getCode() {
    return code;
  }
}