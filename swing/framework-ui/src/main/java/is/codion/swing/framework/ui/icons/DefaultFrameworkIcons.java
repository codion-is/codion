/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.image.BufferedImage;

import static java.util.Objects.requireNonNull;

public final class DefaultFrameworkIcons implements FrameworkIcons {

  @Override
  public ImageIcon filter() {
    return imageIcon(FontIcon.of(FrameworkIkons.FILTER, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon add() {
    return imageIcon(FontIcon.of(FrameworkIkons.ADD, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon delete() {
    return imageIcon(FontIcon.of(FrameworkIkons.DELETE, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon update() {
    return imageIcon(FontIcon.of(FrameworkIkons.UPDATE, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon copy() {
    return imageIcon(FontIcon.of(FrameworkIkons.COPY, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon refresh() {
    return imageIcon(FontIcon.of(FrameworkIkons.REFRESH, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon refreshRequired() {
    return imageIcon(FontIcon.of(FrameworkIkons.REFRESH, ICON_SIZE.get(), Color.RED.darker()));
  }

  @Override
  public ImageIcon clear() {
    return imageIcon(FontIcon.of(FrameworkIkons.CLEAR, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon up() {
    return imageIcon(FontIcon.of(FrameworkIkons.UP, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon down() {
    return imageIcon(FontIcon.of(FrameworkIkons.DOWN, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon detail() {
    return imageIcon(FontIcon.of(FrameworkIkons.DETAIL, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon print() {
    return imageIcon(FontIcon.of(FrameworkIkons.PRINT, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon clearSelection() {
    return imageIcon(FontIcon.of(FrameworkIkons.CLEAR, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon edit() {
    return imageIcon(FontIcon.of(FrameworkIkons.EDIT, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon summary() {
    return imageIcon(FontIcon.of(FrameworkIkons.SUMMARY, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon editPanel() {
    return imageIcon(FontIcon.of(FrameworkIkons.EDIT_PANEL, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  @Override
  public ImageIcon dependencies() {
    return imageIcon(FontIcon.of(FrameworkIkons.DEPENDENCIES, ICON_SIZE.get(), ICON_COLOR.get()));
  }

  /**
   * Creates a {@link ImageIcon} from the given icon.
   * @param icon the icon
   * @return a ImageIcon based on the given icon
   */
  private static ImageIcon imageIcon(final Icon icon) {
    requireNonNull(icon, "icon");
    final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    icon.paintIcon(null, image.getGraphics(), 0, 0);

    return new ImageIcon(image);
  }
}
