/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

public final class DefaultFrameworkIcons implements FrameworkIcons {

  private static final String LOOK_AND_FEEL_PROPERTY = "lookAndFeel";
  private static final String BUTTON_FOREGROUND_PROPERTY = "Button.foreground";

  private static FrameworkIcons instance;

  private final Map<Ikon, FontImageIcon> icons = new HashMap<>();
  private final Map<Integer, FontImageIcon> logos = new HashMap<>();
  private final ImageIcon refreshRequired = FontImageIcon.of(FrameworkIkons.REFRESH, ICON_SIZE.get(), Color.RED.darker()).imageIcon;

  public DefaultFrameworkIcons() {
    add(FrameworkIkons.LOGO, FrameworkIkons.FILTER, FrameworkIkons.SEARCH, FrameworkIkons.ADD,
            FrameworkIkons.DELETE, FrameworkIkons.UPDATE, FrameworkIkons.COPY, FrameworkIkons.REFRESH,
            FrameworkIkons.CLEAR, FrameworkIkons.UP, FrameworkIkons.DOWN, FrameworkIkons.DETAIL,
            FrameworkIkons.PRINT, FrameworkIkons.EDIT, FrameworkIkons.SUMMARY, FrameworkIkons.EDIT_PANEL,
            FrameworkIkons.DEPENDENCIES, FrameworkIkons.SETTINGS, FrameworkIkons.CALENDAR, FrameworkIkons.EDIT_TEXT);
    ICON_COLOR.addDataListener(color -> {
      if (color != null) {
        icons.values().forEach(icon -> icon.setColor(color));
        logos.values().forEach(logo -> logo.setColor(color));
      }
    });
    ICON_SIZE.addListener(() -> icons.keySet().forEach(ikon -> icons.put(ikon, FontImageIcon.of(ikon))));
    UIManager.addPropertyChangeListener(propertyChangeEvent -> {
      if (propertyChangeEvent.getPropertyName().equals(LOOK_AND_FEEL_PROPERTY)) {
        ICON_COLOR.set(UIManager.getColor(BUTTON_FOREGROUND_PROPERTY));
      }
    });
  }

  @Override
  public ImageIcon filter() {
    return icons.get(FrameworkIkons.FILTER).imageIcon;
  }

  @Override
  public ImageIcon search() {
    return icons.get(FrameworkIkons.SEARCH).imageIcon;
  }

  @Override
  public ImageIcon add() {
    return icons.get(FrameworkIkons.ADD).imageIcon;
  }

  @Override
  public ImageIcon delete() {
    return icons.get(FrameworkIkons.DELETE).imageIcon;
  }

  @Override
  public ImageIcon update() {
    return icons.get(FrameworkIkons.UPDATE).imageIcon;
  }

  @Override
  public ImageIcon copy() {
    return icons.get(FrameworkIkons.COPY).imageIcon;
  }

  @Override
  public ImageIcon refresh() {
    return icons.get(FrameworkIkons.REFRESH).imageIcon;
  }

  @Override
  public ImageIcon refreshRequired() {
    return refreshRequired;
  }

  @Override
  public ImageIcon clear() {
    return icons.get(FrameworkIkons.CLEAR).imageIcon;
  }

  @Override
  public ImageIcon up() {
    return icons.get(FrameworkIkons.UP).imageIcon;
  }

  @Override
  public ImageIcon down() {
    return icons.get(FrameworkIkons.DOWN).imageIcon;
  }

  @Override
  public ImageIcon detail() {
    return icons.get(FrameworkIkons.DETAIL).imageIcon;
  }

  @Override
  public ImageIcon print() {
    return icons.get(FrameworkIkons.PRINT).imageIcon;
  }

  @Override
  public ImageIcon clearSelection() {
    return icons.get(FrameworkIkons.CLEAR).imageIcon;
  }

  @Override
  public ImageIcon edit() {
    return icons.get(FrameworkIkons.EDIT).imageIcon;
  }

  @Override
  public ImageIcon summary() {
    return icons.get(FrameworkIkons.SUMMARY).imageIcon;
  }

  @Override
  public ImageIcon editPanel() {
    return icons.get(FrameworkIkons.EDIT_PANEL).imageIcon;
  }

  @Override
  public ImageIcon dependencies() {
    return icons.get(FrameworkIkons.DEPENDENCIES).imageIcon;
  }

  @Override
  public ImageIcon settings() {
    return icons.get(FrameworkIkons.SETTINGS).imageIcon;
  }

  @Override
  public ImageIcon calendar() {
    return icons.get(FrameworkIkons.CALENDAR).imageIcon;
  }

  @Override
  public ImageIcon editText() {
    return icons.get(FrameworkIkons.EDIT_TEXT).imageIcon;
  }

  @Override
  public ImageIcon logo() {
    return icons.get(FrameworkIkons.LOGO).imageIcon;
  }

  @Override
  public ImageIcon logo(int size) {
    return logos.computeIfAbsent(size, k -> new LogoImageIcon(size)).imageIcon;
  }

  @Override
  public void add(Ikon... ikons) {
    for (Ikon ikon : requireNonNull(ikons)) {
      if (icons.containsKey(requireNonNull(ikon))) {
        throw new IllegalArgumentException("Icon has already been added: " + ikon);
      }
    }
    for (Ikon ikon : ikons) {
      icons.put(ikon, FontImageIcon.of(ikon));
    }
  }

  @Override
  public ImageIcon icon(Ikon ikon) {
    if (!icons.containsKey(requireNonNull(ikon))) {
      throw new IllegalArgumentException("No icon has been added for key: " + ikon);
    }

    return icons.get(ikon).imageIcon;
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

  private static class FontImageIcon {

    protected final FontIcon fontIcon;
    protected final ImageIcon imageIcon;

    private FontImageIcon(Ikon ikon, int size, Color color) {
      fontIcon = FontIcon.of(ikon, size, color);
      imageIcon = createImageIcon();
      paintIcon();
    }

    protected ImageIcon createImageIcon() {
      return new ImageIcon(new BufferedImage(fontIcon.getIconWidth(), fontIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB));
    }

    protected void paintIcon() {
      fontIcon.paintIcon(null, imageIcon.getImage().getGraphics(), 0, 0);
    }

    private void setColor(Color color) {
      fontIcon.setIconColor(color);
      paintIcon();
    }

    private static FontImageIcon of(Ikon ikon) {
      return of(ikon, ICON_SIZE.get(), ICON_COLOR.get());
    }

    private static FontImageIcon of(Ikon ikon, int size, Color color) {
      return new FontImageIcon(ikon, size, color);
    }
  }

  private static final class LogoImageIcon extends FontImageIcon {

    private LogoImageIcon(int size) {
      super(FrameworkIkons.LOGO, size, ICON_COLOR.get());
    }

    @Override
    protected ImageIcon createImageIcon() {
      return new ImageIcon(new BufferedImage(fontIcon.getIconWidth(), fontIcon.getIconWidth(), BufferedImage.TYPE_INT_ARGB));
    }

    @Override
    protected void paintIcon() {
      //center on y-axis
      int yOffset = (fontIcon.getIconHeight() - fontIcon.getIconWidth()) / 2;

      fontIcon.paintIcon(null, imageIcon.getImage().getGraphics(), 0, -yOffset);
    }
  }
}
