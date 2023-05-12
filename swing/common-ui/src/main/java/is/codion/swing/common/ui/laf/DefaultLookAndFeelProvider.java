/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.laf;

import is.codion.swing.common.ui.Utilities;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.HashMap;
import java.util.Map;

import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeelProvider;
import static java.util.Objects.requireNonNull;

final class DefaultLookAndFeelProvider implements LookAndFeelProvider {

  static final Map<String, LookAndFeelProvider> LOOK_AND_FEEL_PROVIDERS = new HashMap<>();

  static {
    LookAndFeelProvider systemProvider = lookAndFeelProvider(new LookAndFeelInfo("System", Utilities.systemLookAndFeelClassName()));
    LOOK_AND_FEEL_PROVIDERS.put(systemProvider.lookAndFeelInfo().getClassName(), systemProvider);
    LookAndFeelProvider crossPlatformProvider = lookAndFeelProvider(new LookAndFeelInfo("Cross Platform", UIManager.getCrossPlatformLookAndFeelClassName()));
    if (!LOOK_AND_FEEL_PROVIDERS.containsKey(crossPlatformProvider.lookAndFeelInfo().getClassName())) {
      LOOK_AND_FEEL_PROVIDERS.put(crossPlatformProvider.lookAndFeelInfo().getClassName(), crossPlatformProvider);
    }
  }

  private final LookAndFeelInfo lookAndFeelInfo;
  private final Runnable enabler;

  DefaultLookAndFeelProvider(LookAndFeelInfo lookAndFeelInfo, Runnable enabler) {
    this.lookAndFeelInfo = requireNonNull(lookAndFeelInfo);
    this.enabler = requireNonNull(enabler);
  }

  @Override
  public LookAndFeelInfo lookAndFeelInfo() {
    return lookAndFeelInfo;
  }

  @Override
  public void enable() {
    this.enabler.run();
  }

  @Override
  public String toString() {
    return lookAndFeelInfo.getName();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DefaultLookAndFeelProvider)) {
      return false;
    }

    DefaultLookAndFeelProvider that = (DefaultLookAndFeelProvider) obj;

    return lookAndFeelInfo.getClassName().equals(that.lookAndFeelInfo.getClassName());
  }

  @Override
  public int hashCode() {
    return lookAndFeelInfo().getClassName().hashCode();
  }
}
