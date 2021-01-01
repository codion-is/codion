/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.plugin.ikonli.foundation;

import is.codion.swing.framework.ui.icons.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import java.awt.Color;

/**
 * {@link FrameworkIcons} implementation based on ikonli-foundation.
 */
public final class IkonliFoundationFrameworkIcons extends IkonliFoundationIcons implements FrameworkIcons {

  @Override
  public ImageIcon add() {
    return imageIcon(FontIcon.of(Foundation.PAGE_ADD, ICON_SIZE));
  }

  @Override
  public ImageIcon delete() {
    return imageIcon(FontIcon.of(Foundation.TRASH, ICON_SIZE));
  }

  @Override
  public ImageIcon update() {
    return imageIcon(FontIcon.of(Foundation.SAVE, ICON_SIZE));
  }

  @Override
  public ImageIcon copy() {
    return imageIcon(FontIcon.of(Foundation.PAGE_COPY, ICON_SIZE));
  }

  @Override
  public ImageIcon refresh() {
    return imageIcon(FontIcon.of(Foundation.REFRESH, ICON_SIZE));
  }

  @Override
  public ImageIcon refreshRequired() {
    final FontIcon refresh = FontIcon.of(Foundation.REFRESH, ICON_SIZE);
    refresh.setIconColor(Color.RED.darker());

    return imageIcon(refresh);
  }

  @Override
  public ImageIcon clear() {
    return imageIcon(FontIcon.of(Foundation.PAGE, ICON_SIZE));
  }

  @Override
  public ImageIcon up() {
    return imageIcon(FontIcon.of(Foundation.ARROW_UP, ICON_SIZE));
  }

  @Override
  public ImageIcon down() {
    return imageIcon(FontIcon.of(Foundation.ARROW_DOWN, ICON_SIZE));
  }

  @Override
  public ImageIcon detail() {
    return imageIcon(FontIcon.of(Foundation.PAGE_MULTIPLE, ICON_SIZE));
  }

  @Override
  public ImageIcon print() {
    return imageIcon(FontIcon.of(Foundation.PRINT, ICON_SIZE));
  }

  @Override
  public ImageIcon clearSelection() {
    return imageIcon(FontIcon.of(Foundation.PAGE, ICON_SIZE));
  }

  @Override
  public ImageIcon edit() {
    return imageIcon(FontIcon.of(Foundation.PAGE_EDIT, ICON_SIZE));
  }

  @Override
  public ImageIcon summary() {
    return imageIcon(FontIcon.of(Foundation.DOWNLOAD, ICON_SIZE));
  }

  @Override
  public ImageIcon editPanel() {
    return imageIcon(FontIcon.of(Foundation.LIST_THUMBNAILS, ICON_SIZE));
  }

  @Override
  public ImageIcon dependencies() {
    return imageIcon(FontIcon.of(Foundation.SHARE, ICON_SIZE));
  }
}
