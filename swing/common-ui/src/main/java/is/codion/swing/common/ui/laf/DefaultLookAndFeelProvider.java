/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.laf;

import javax.swing.UIManager;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class DefaultLookAndFeelProvider implements LookAndFeelProvider {

  static final Map<String, LookAndFeelProvider> LOOK_AND_FEEL_PROVIDERS = new HashMap<>();

  static {
    final LookAndFeelProvider systemProvider = LookAndFeelProvider.create(LookAndFeelProvider.getSystemLookAndFeelClassName());
    LOOK_AND_FEEL_PROVIDERS.put(systemProvider.getName(), systemProvider);
    final LookAndFeelProvider crossPlatformProvider = LookAndFeelProvider.create(UIManager.getCrossPlatformLookAndFeelClassName());
    if (!LOOK_AND_FEEL_PROVIDERS.containsKey(crossPlatformProvider.getName())) {
      LOOK_AND_FEEL_PROVIDERS.put(crossPlatformProvider.getName(), crossPlatformProvider);
    }
  }

  private final String classname;
  private final String name;
  private final Runnable enabler;

  DefaultLookAndFeelProvider(final String classname, final String name, final Runnable enabler) {
    this.classname = requireNonNull(classname);
    this.name = requireNonNull(name);
    this.enabler = requireNonNull(enabler);
  }

  @Override
  public String getClassName() {
    return classname;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void enable() {
    this.enabler.run();
  }

  @Override
  public String toString() {
    return getName();
  }
}
