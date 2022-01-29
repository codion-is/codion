/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import is.codion.swing.common.ui.icons.DefaultIcons;

import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import java.awt.Color;

public final class DefaultFrameworkIcons extends DefaultIcons implements FrameworkIcons {

@Override
  public ImageIcon add() {
    return imageIcon(FontIcon.of(FrameworkIkons.ADD, ICON_SIZE));
  }

  @Override
  public ImageIcon delete() {
    return imageIcon(FontIcon.of(FrameworkIkons.DELETE, ICON_SIZE));
  }

  @Override
  public ImageIcon update() {
    return imageIcon(FontIcon.of(FrameworkIkons.UPDATE, ICON_SIZE));
  }

  @Override
  public ImageIcon copy() {
    return imageIcon(FontIcon.of(FrameworkIkons.COPY, ICON_SIZE));
  }

  @Override
  public ImageIcon refresh() {
    return imageIcon(FontIcon.of(FrameworkIkons.REFRESH, ICON_SIZE));
  }

  @Override
  public ImageIcon refreshRequired() {
    final FontIcon refresh = FontIcon.of(FrameworkIkons.REFRESH, ICON_SIZE);
    refresh.setIconColor(Color.RED.darker());

    return imageIcon(refresh);
  }

  @Override
  public ImageIcon clear() {
    return imageIcon(FontIcon.of(FrameworkIkons.CLEAR, ICON_SIZE));
  }

  @Override
  public ImageIcon up() {
    return imageIcon(FontIcon.of(FrameworkIkons.UP, ICON_SIZE));
  }

  @Override
  public ImageIcon down() {
    return imageIcon(FontIcon.of(FrameworkIkons.DOWN, ICON_SIZE));
  }

  @Override
  public ImageIcon detail() {
    return imageIcon(FontIcon.of(FrameworkIkons.DETAIL, ICON_SIZE));
  }

  @Override
  public ImageIcon print() {
    return imageIcon(FontIcon.of(FrameworkIkons.PRINT, ICON_SIZE));
  }

  @Override
  public ImageIcon clearSelection() {
    return imageIcon(FontIcon.of(FrameworkIkons.CLEAR, ICON_SIZE));
  }

  @Override
  public ImageIcon edit() {
    return imageIcon(FontIcon.of(FrameworkIkons.EDIT, ICON_SIZE));
  }

  @Override
  public ImageIcon summary() {
    return imageIcon(FontIcon.of(FrameworkIkons.SUMMARY, ICON_SIZE));
  }

  @Override
  public ImageIcon editPanel() {
    return imageIcon(FontIcon.of(FrameworkIkons.EDIT_PANEL, ICON_SIZE));
  }

  @Override
  public ImageIcon dependencies() {
    return imageIcon(FontIcon.of(FrameworkIkons.DEPENDENCIES, ICON_SIZE));
  }
}
