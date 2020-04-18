/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.plugin.ikonli;

import org.jminor.swing.framework.ui.icons.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import java.awt.Color;

/**
 * {@link FrameworkIcons} implementation based on ikonli-foundation.
 */
public final class IkonliFrameworkFoundationIcons extends IkonliFoundationIcons implements FrameworkIcons {

  @Override
  public ImageIcon add() {
    return imageIcon(FontIcon.of(Foundation.PAGE_ADD));
  }

  @Override
  public ImageIcon delete() {
    return imageIcon(FontIcon.of(Foundation.TRASH));
  }

  @Override
  public ImageIcon update() {
    return imageIcon(FontIcon.of(Foundation.SAVE));
  }

  @Override
  public ImageIcon refresh() {
    return imageIcon(FontIcon.of(Foundation.REFRESH));
  }

  @Override
  public ImageIcon refreshRequired() {
    final FontIcon refresh = FontIcon.of(Foundation.REFRESH);
    refresh.setIconColor(Color.RED.darker());

    return imageIcon(refresh);
  }

  @Override
  public ImageIcon clear() {
    return imageIcon(FontIcon.of(Foundation.PAGE));
  }

  @Override
  public ImageIcon up() {
    return imageIcon(FontIcon.of(Foundation.ARROW_UP));
  }

  @Override
  public ImageIcon down() {
    return imageIcon(FontIcon.of(Foundation.ARROW_DOWN));
  }

  @Override
  public ImageIcon detail() {
    return imageIcon(FontIcon.of(Foundation.PAGE_MULTIPLE));
  }

  @Override
  public ImageIcon print() {
    return imageIcon(FontIcon.of(Foundation.PRINT));
  }

  @Override
  public ImageIcon clearSelection() {
    return imageIcon(FontIcon.of(Foundation.ARROW_DOWN));
  }

  @Override
  public ImageIcon edit() {
    return imageIcon(FontIcon.of(Foundation.PAGE_EDIT));
  }

  @Override
  public ImageIcon summary() {
    return imageIcon(FontIcon.of(Foundation.DOWNLOAD));
  }

  @Override
  public ImageIcon editPanel() {
    return imageIcon(FontIcon.of(Foundation.LIST_THUMBNAILS));
  }
}
