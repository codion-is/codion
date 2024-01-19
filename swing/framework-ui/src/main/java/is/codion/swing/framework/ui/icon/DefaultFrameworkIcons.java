/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icon;

import is.codion.swing.common.ui.icon.FontImageIcon;
import is.codion.swing.common.ui.icon.FontImageIcon.IconPainter;
import is.codion.swing.common.ui.icon.FontImageIcon.ImageIconFactory;
import is.codion.swing.common.ui.icon.Icons;
import is.codion.swing.common.ui.icon.Logos;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import static is.codion.swing.framework.ui.icon.FrameworkIkon.*;

/**
 * A default FrameworkIcons implementation.
 */
public final class DefaultFrameworkIcons implements FrameworkIcons, Logos {

  private static final IconPainter LOGO_ICON_PAINTER = new IconPainter() {

    @Override
    public void paintIcon(FontIcon fontIcon, ImageIcon imageIcon) {
      //center on y-axis
      int yOffset = (fontIcon.getIconHeight() - fontIcon.getIconWidth()) / 2;

      fontIcon.paintIcon(null, imageIcon.getImage().getGraphics(), 0, -yOffset);
    }
  };

  private static final ImageIconFactory LOGO_ICON_FACTORY = new ImageIconFactory() {
    @Override
    public ImageIcon createImageIcon(FontIcon fontIcon) {
      int yCorrection = (fontIcon.getIconHeight() - fontIcon.getIconWidth());

      return new ImageIcon(new BufferedImage(fontIcon.getIconWidth(), fontIcon.getIconHeight() - yCorrection, BufferedImage.TYPE_INT_ARGB));
    }
  };

  private static FrameworkIcons instance;

  private final Icons icons = Icons.icons();
  private final Map<Integer, FontImageIcon> logos = new HashMap<>();
  private final ImageIcon refreshRequired = FontImageIcon.builder(REFRESH)
          .color(Color.RED.darker())
          .build().imageIcon();

  public DefaultFrameworkIcons() {
    add(LOGO, FILTER, SEARCH, ADD, DELETE, UPDATE, COPY, REFRESH, CLEAR, UP, DOWN, DETAIL,
            PRINT, EDIT, SUMMARY, EDIT_PANEL, DEPENDENCIES, SETTINGS, CALENDAR, EDIT_TEXT);
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
    return icon(FILTER);
  }

  @Override
  public ImageIcon search() {
    return icon(SEARCH);
  }

  @Override
  public ImageIcon add() {
    return icon(ADD);
  }

  @Override
  public ImageIcon delete() {
    return icon(DELETE);
  }

  @Override
  public ImageIcon update() {
    return icon(UPDATE);
  }

  @Override
  public ImageIcon copy() {
    return icon(COPY);
  }

  @Override
  public ImageIcon refresh() {
    return icon(REFRESH);
  }

  @Override
  public ImageIcon refreshRequired() {
    return refreshRequired;
  }

  @Override
  public ImageIcon clear() {
    return icon(CLEAR);
  }

  @Override
  public ImageIcon up() {
    return icon(UP);
  }

  @Override
  public ImageIcon down() {
    return icon(DOWN);
  }

  @Override
  public ImageIcon detail() {
    return icon(DETAIL);
  }

  @Override
  public ImageIcon print() {
    return icon(PRINT);
  }

  @Override
  public ImageIcon clearSelection() {
    return icon(CLEAR);
  }

  @Override
  public ImageIcon edit() {
    return icon(EDIT);
  }

  @Override
  public ImageIcon summary() {
    return icon(SUMMARY);
  }

  @Override
  public ImageIcon editPanel() {
    return icon(EDIT_PANEL);
  }

  @Override
  public ImageIcon dependencies() {
    return icon(DEPENDENCIES);
  }

  @Override
  public ImageIcon settings() {
    return icon(SETTINGS);
  }

  @Override
  public ImageIcon calendar() {
    return icon(CALENDAR);
  }

  @Override
  public ImageIcon editText() {
    return icon(EDIT_TEXT);
  }

  @Override
  public ImageIcon logo() {
    return icon(LOGO);
  }

  @Override
  public ImageIcon logo(int size) {
    return logos.computeIfAbsent(size, k -> FontImageIcon.builder(LOGO)
            .size(size)
            .iconPainter(LOGO_ICON_PAINTER)
            .imageIconFactory(LOGO_ICON_FACTORY)
            .build()).imageIcon();
  }

  @Override
  public void iconColor(Color color) {
    icons.iconColor(color);
  }

  @Override
  public FrameworkIcons enableIconColorListener() {
    icons.enableIconColorListener();
    return this;
  }

  @Override
  public FrameworkIcons disableIconColorListener() {
    icons.disableIconColorListener();
    return this;
  }

  static FrameworkIcons instance() {
    if (instance == null) {
      instance = (FrameworkIcons) createInstance().enableIconColorListener();
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
}
