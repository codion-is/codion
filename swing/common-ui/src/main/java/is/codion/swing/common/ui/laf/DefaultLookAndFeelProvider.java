/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.laf;

import is.codion.swing.common.ui.Utilities;

import javax.swing.UIManager;
import java.util.HashMap;
import java.util.Map;

import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeelProvider;
import static java.util.Objects.requireNonNull;

final class DefaultLookAndFeelProvider implements LookAndFeelProvider {

  static final Map<String, LookAndFeelProvider> LOOK_AND_FEEL_PROVIDERS = new HashMap<>();

  static {
    LookAndFeelProvider systemProvider = lookAndFeelProvider(Utilities.getSystemLookAndFeelClassName());
    LOOK_AND_FEEL_PROVIDERS.put(systemProvider.name(), systemProvider);
    LookAndFeelProvider crossPlatformProvider = lookAndFeelProvider(UIManager.getCrossPlatformLookAndFeelClassName());
    if (!LOOK_AND_FEEL_PROVIDERS.containsKey(crossPlatformProvider.name())) {
      LOOK_AND_FEEL_PROVIDERS.put(crossPlatformProvider.name(), crossPlatformProvider);
    }
  }

  private final String classname;
  private final String name;
  private final Runnable enabler;

  DefaultLookAndFeelProvider(String classname, String name, Runnable enabler) {
    this.classname = requireNonNull(classname);
    this.name = requireNonNull(name);
    this.enabler = requireNonNull(enabler);
  }

  @Override
  public String className() {
    return classname;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public void enable() {
    this.enabler.run();
  }

  @Override
  public String toString() {
    return name();
  }
}
