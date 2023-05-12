/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.icons.DefaultFontImageIcon;
import is.codion.swing.common.ui.icons.FontImageIcon;
import is.codion.swing.common.ui.icons.Icons;

import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * A default FrameworkIcons implementation.
 */
public final class DefaultFrameworkIcons implements FrameworkIcons, Logos {

  private static FrameworkIcons instance;

  private final Icons icons = Icons.icons();
  private final Map<Integer, FontImageIcon> logos = new HashMap<>();
  private final ImageIcon refreshRequired = FontImageIcon.of(FrameworkIkons.REFRESH, ICON_SIZE.get(), Color.RED.darker()).imageIcon();

  public DefaultFrameworkIcons() {
    add(FrameworkIkons.LOGO, FrameworkIkons.FILTER, FrameworkIkons.SEARCH, FrameworkIkons.ADD,
            FrameworkIkons.DELETE, FrameworkIkons.UPDATE, FrameworkIkons.COPY, FrameworkIkons.REFRESH,
            FrameworkIkons.CLEAR, FrameworkIkons.UP, FrameworkIkons.DOWN, FrameworkIkons.DETAIL,
            FrameworkIkons.PRINT, FrameworkIkons.EDIT, FrameworkIkons.SUMMARY, FrameworkIkons.EDIT_PANEL,
            FrameworkIkons.DEPENDENCIES, FrameworkIkons.SETTINGS, FrameworkIkons.CALENDAR, FrameworkIkons.EDIT_TEXT);
  }

  @Override
  public void add(Ikon... ikons) {
    icons.add(ikons);
  }

  @Override
  public ImageIcon icon(Ikon ikon) {
    return icons.icon(ikon);
  }

  @Override
  public ImageIcon filter() {
    return icon(FrameworkIkons.FILTER);
  }

  @Override
  public ImageIcon search() {
    return icon(FrameworkIkons.SEARCH);
  }

  @Override
  public ImageIcon add() {
    return icon(FrameworkIkons.ADD);
  }

  @Override
  public ImageIcon delete() {
    return icon(FrameworkIkons.DELETE);
  }

  @Override
  public ImageIcon update() {
    return icon(FrameworkIkons.UPDATE);
  }

  @Override
  public ImageIcon copy() {
    return icon(FrameworkIkons.COPY);
  }

  @Override
  public ImageIcon refresh() {
    return icon(FrameworkIkons.REFRESH);
  }

  @Override
  public ImageIcon refreshRequired() {
    return refreshRequired;
  }

  @Override
  public ImageIcon clear() {
    return icon(FrameworkIkons.CLEAR);
  }

  @Override
  public ImageIcon up() {
    return icon(FrameworkIkons.UP);
  }

  @Override
  public ImageIcon down() {
    return icon(FrameworkIkons.DOWN);
  }

  @Override
  public ImageIcon detail() {
    return icon(FrameworkIkons.DETAIL);
  }

  @Override
  public ImageIcon print() {
    return icon(FrameworkIkons.PRINT);
  }

  @Override
  public ImageIcon clearSelection() {
    return icon(FrameworkIkons.CLEAR);
  }

  @Override
  public ImageIcon edit() {
    return icon(FrameworkIkons.EDIT);
  }

  @Override
  public ImageIcon summary() {
    return icon(FrameworkIkons.SUMMARY);
  }

  @Override
  public ImageIcon editPanel() {
    return icon(FrameworkIkons.EDIT_PANEL);
  }

  @Override
  public ImageIcon dependencies() {
    return icon(FrameworkIkons.DEPENDENCIES);
  }

  @Override
  public ImageIcon settings() {
    return icon(FrameworkIkons.SETTINGS);
  }

  @Override
  public ImageIcon calendar() {
    return icon(FrameworkIkons.CALENDAR);
  }

  @Override
  public ImageIcon editText() {
    return icon(FrameworkIkons.EDIT_TEXT);
  }

  @Override
  public ImageIcon logo() {
    return icon(FrameworkIkons.LOGO);
  }

  @Override
  public ImageIcon logo(int size) {
    return logos.computeIfAbsent(size, k -> new LogoImageIcon(size)).imageIcon();
  }

  static FrameworkIcons instance() {
    if (instance == null) {
      instance = createInstance();
    }

    return instance;
  }

  private static FrameworkIcons createInstance() {
    String iconsClassName = FRAMEWORK_ICONS_CLASSNAME.get();
    ServiceLoader<FrameworkIcons> loader = ServiceLoader.load(FrameworkIcons.class);
    for (FrameworkIcons icons : loader) {
      if (Objects.equals(icons.getClass().getName(), iconsClassName)) {
        return icons;
      }
    }

    throw new IllegalArgumentException("FrameworkIcons implementation " + iconsClassName + " not found");
  }

  private static final class LogoImageIcon extends DefaultFontImageIcon {

    private LogoImageIcon(int size) {
      super(FrameworkIkons.LOGO, size);
    }

    @Override
    protected void paintIcon() {
      //center on y-axis
      int yOffset = (fontIcon().getIconHeight() - fontIcon().getIconWidth()) / 2;

      fontIcon().paintIcon(null, imageIcon().getImage().getGraphics(), 0, -yOffset);
    }
  }
}
