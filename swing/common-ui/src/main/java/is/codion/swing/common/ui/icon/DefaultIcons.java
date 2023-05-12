/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icon;

import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class DefaultIcons implements Icons {

  private static final String LOOK_AND_FEEL_PROPERTY = "lookAndFeel";
  private static final String BUTTON_FOREGROUND_PROPERTY = "Button.foreground";

  private final Map<Ikon, FontImageIcon> icons = new HashMap<>();
  private final Map<Integer, FontImageIcon> logos = new HashMap<>();

  DefaultIcons() {
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

    return icons.get(ikon).imageIcon();
  }
}
