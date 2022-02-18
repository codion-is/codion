/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class DefaultFrameworkIcons implements FrameworkIcons {

  private static final String LOOK_AND_FEEL_PROPERTY = "lookAndFeel";
  private static final String BUTTON_FOREGROUND_PROPERTY = "Button.foreground";

  private static final Map<Ikon, FontImageIcon> ICONS = new HashMap<>();
  private static final Map<Integer, FontImageIcon> LOGOS = new HashMap<>();
  private static final ImageIcon REFRESH_REQUIRED = FontImageIcon.imageIcon(FontIcon.of(FrameworkIkons.REFRESH, ICON_SIZE.get(), Color.RED.darker()));

  static {
    UIManager.addPropertyChangeListener(evt -> {
      if (evt.getPropertyName().equals(LOOK_AND_FEEL_PROPERTY)) {
        ICON_COLOR.set(UIManager.getColor(BUTTON_FOREGROUND_PROPERTY));
      }
    });
    ICONS.put(FrameworkIkons.LOGO, FontImageIcon.of(FrameworkIkons.LOGO));
    ICONS.put(FrameworkIkons.FILTER, FontImageIcon.of(FrameworkIkons.FILTER));
    ICONS.put(FrameworkIkons.ADD, FontImageIcon.of(FrameworkIkons.ADD));
    ICONS.put(FrameworkIkons.DELETE, FontImageIcon.of(FrameworkIkons.DELETE));
    ICONS.put(FrameworkIkons.UPDATE, FontImageIcon.of(FrameworkIkons.UPDATE));
    ICONS.put(FrameworkIkons.COPY, FontImageIcon.of(FrameworkIkons.COPY));
    ICONS.put(FrameworkIkons.REFRESH, FontImageIcon.of(FrameworkIkons.REFRESH));
    ICONS.put(FrameworkIkons.CLEAR, FontImageIcon.of(FrameworkIkons.CLEAR));
    ICONS.put(FrameworkIkons.UP, FontImageIcon.of(FrameworkIkons.UP));
    ICONS.put(FrameworkIkons.DOWN, FontImageIcon.of(FrameworkIkons.DOWN));
    ICONS.put(FrameworkIkons.DETAIL, FontImageIcon.of(FrameworkIkons.DETAIL));
    ICONS.put(FrameworkIkons.PRINT, FontImageIcon.of(FrameworkIkons.PRINT));
    ICONS.put(FrameworkIkons.EDIT, FontImageIcon.of(FrameworkIkons.EDIT));
    ICONS.put(FrameworkIkons.SUMMARY, FontImageIcon.of(FrameworkIkons.SUMMARY));
    ICONS.put(FrameworkIkons.EDIT_PANEL, FontImageIcon.of(FrameworkIkons.EDIT_PANEL));
    ICONS.put(FrameworkIkons.DEPENDENCIES, FontImageIcon.of(FrameworkIkons.DEPENDENCIES));
    ICON_COLOR.addDataListener(color -> {
      if (color != null) {
        ICONS.values().forEach(icon -> icon.setColor(color));
        LOGOS.values().forEach(logo -> logo.setColor(color));
      }
    });
  }

  @Override
  public ImageIcon filter() {
    return ICONS.get(FrameworkIkons.FILTER).imageIcon;
  }

  @Override
  public ImageIcon add() {
    return ICONS.get(FrameworkIkons.ADD).imageIcon;
  }

  @Override
  public ImageIcon delete() {
    return ICONS.get(FrameworkIkons.DELETE).imageIcon;
  }

  @Override
  public ImageIcon update() {
    return ICONS.get(FrameworkIkons.UPDATE).imageIcon;
  }

  @Override
  public ImageIcon copy() {
    return ICONS.get(FrameworkIkons.COPY).imageIcon;
  }

  @Override
  public ImageIcon refresh() {
    return ICONS.get(FrameworkIkons.REFRESH).imageIcon;
  }

  @Override
  public ImageIcon refreshRequired() {
    return REFRESH_REQUIRED;
  }

  @Override
  public ImageIcon clear() {
    return ICONS.get(FrameworkIkons.CLEAR).imageIcon;
  }

  @Override
  public ImageIcon up() {
    return ICONS.get(FrameworkIkons.UP).imageIcon;
  }

  @Override
  public ImageIcon down() {
    return ICONS.get(FrameworkIkons.DOWN).imageIcon;
  }

  @Override
  public ImageIcon detail() {
    return ICONS.get(FrameworkIkons.DETAIL).imageIcon;
  }

  @Override
  public ImageIcon print() {
    return ICONS.get(FrameworkIkons.PRINT).imageIcon;
  }

  @Override
  public ImageIcon clearSelection() {
    return ICONS.get(FrameworkIkons.CLEAR).imageIcon;
  }

  @Override
  public ImageIcon edit() {
    return ICONS.get(FrameworkIkons.EDIT).imageIcon;
  }

  @Override
  public ImageIcon summary() {
    return ICONS.get(FrameworkIkons.SUMMARY).imageIcon;
  }

  @Override
  public ImageIcon editPanel() {
    return ICONS.get(FrameworkIkons.EDIT_PANEL).imageIcon;
  }

  @Override
  public ImageIcon dependencies() {
    return ICONS.get(FrameworkIkons.DEPENDENCIES).imageIcon;
  }

  @Override
  public ImageIcon logo() {
    return ICONS.get(FrameworkIkons.LOGO).imageIcon;
  }

  @Override
  public ImageIcon logo(final int size) {
    return LOGOS.computeIfAbsent(size, k -> new FontImageIcon(FrameworkIkons.LOGO, size,
            fontIcon -> {
              //hack to get the logo centered on the image
              final int yOffset = (fontIcon.getIconHeight() - fontIcon.getIconWidth()) / 2;
              final BufferedImage image = new BufferedImage(fontIcon.getIconWidth(), fontIcon.getIconWidth(), BufferedImage.TYPE_INT_ARGB);
              fontIcon.paintIcon(null, image.getGraphics(), 0, -yOffset);

              return new ImageIcon(image);
            })).imageIcon;
  }

  private static final class FontImageIcon {

    private final FontIcon fontIcon;
    private final ImageIcon imageIcon;

    private FontImageIcon(final Ikon ikon) {
      this(ikon, ICON_SIZE.get(), FontImageIcon::imageIcon);
    }

    private FontImageIcon(final Ikon ikon, final int size,
                          final Function<FontIcon, ImageIcon> toImageIcon) {
      this.fontIcon = FontIcon.of(ikon, size, ICON_COLOR.get());
      this.imageIcon = toImageIcon.apply(fontIcon);
    }

    private void setColor(final Color color) {
      fontIcon.setIconColor(color);
      fontIcon.paintIcon(null, imageIcon.getImage().getGraphics(), 0, 0);
    }

    private static FontImageIcon of(final Ikon ikon) {
      return new FontImageIcon(ikon);
    }

    private static ImageIcon imageIcon(final Icon icon) {
      requireNonNull(icon, "icon");
      final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
      icon.paintIcon(null, image.getGraphics(), 0, 0);

      return new ImageIcon(image);
    }
  }
}
