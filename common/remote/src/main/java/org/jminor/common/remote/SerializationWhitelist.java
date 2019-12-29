/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * Implements a serialization whitelist for Java 8
 */
public final class SerializationWhitelist {

  private static final Logger LOG = LoggerFactory.getLogger(SerializationWhitelist.class);

  private SerializationWhitelist() {}

  /**
   * Configures a serialization whitelist, does nothing if {@code whitelist} is null or empty.
   * @param whitelistFile the path to the file containing the whitelisted class names, or the
   * file to write to in case of a dry-run
   * @param dryRun if true then a dry-run is assumed, with the whitelist written to
   * {@code whitelist} on a call to {@link #writeDryRunWhitelist()}
   */
  public static void configureSerializationWhitelist(final String whitelistFile, final boolean dryRun) {
    if (!nullOrEmpty(whitelistFile)) {
      sun.misc.ObjectInputFilter.Config.setSerialFilter(dryRun ?
              new SerializationFilterDryRun(whitelistFile) : new SerializationFilter(whitelistFile));
      LOG.debug("Serialization filter whitelist set: " + whitelistFile + " (dry run: " + dryRun + ")");
    }
  }

  /**
   * Returns true if a serialization dry-run is active.
   * @return true if a dry-run is active.
   */
  public static boolean isSerializationDryRunActive() {
    return sun.misc.ObjectInputFilter.Config.getSerialFilter() instanceof SerializationFilterDryRun;
  }

  /**
   * Writes the class names collected during a dry-run to file.
   * If dry-run was not active this method has no effect.
   */
  public static void writeDryRunWhitelist() {
    final sun.misc.ObjectInputFilter serialFilter = sun.misc.ObjectInputFilter.Config.getSerialFilter();
    if (serialFilter instanceof SerializationFilterDryRun) {
      ((SerializationFilterDryRun) serialFilter).writeToFile();
    }
  }

  private static final class SerializationFilterDryRun implements sun.misc.ObjectInputFilter {

    private final String whitelistFile;
    private final Set<Class> deserializedClasses = new HashSet<>();

    public SerializationFilterDryRun(final String whitelistFile) {
      this.whitelistFile = requireNonNull(whitelistFile, "whitelistFile");
    }

    @Override
    public Status checkInput(final FilterInfo filterInfo) {
      final Class clazz = filterInfo.serialClass();
      if (clazz != null) {
        deserializedClasses.add(clazz);
      }

      return Status.ALLOWED;
    }

    private void writeToFile() {
      try {
        final File file = new File(whitelistFile);
        file.createNewFile();
        Files.write(file.toPath(), deserializedClasses.stream()
                .map(Class::getName).sorted().collect(toList()));
        LOG.debug("Serialization whitelist written: " + whitelistFile);
      }
      catch (final Exception e) {
        LOG.error("Error while writing serialization filter dry run results: " + whitelistFile, e);
      }
    }
  }

  static final class SerializationFilter implements sun.misc.ObjectInputFilter {

    private static final String WILDCARD = "*";

    private final Set<String> allowedClassnames = new HashSet<>();
    private final List<String> allowedWildcardClassnames = new ArrayList<>();

    SerializationFilter(final String whitelistFile) {
      try (final Stream<String> stream = Files.lines(Paths.get(whitelistFile))) {
        stream.collect(toSet()).forEach(whitelistItem -> {
          if (whitelistItem.endsWith(WILDCARD)) {
            allowedWildcardClassnames.add(whitelistItem.substring(0, whitelistItem.length() - 1));
          }
          else {
            allowedClassnames.add(whitelistItem);
          }
        });
      }
      catch (final IOException e) {
        LOG.error("Unable to read serialization whitelist: " + whitelistFile);
        throw new RuntimeException(e);
      }
    }

    @Override
    public Status checkInput(final FilterInfo filterInfo) {
      final Class clazz = filterInfo.serialClass();
      if (clazz == null) {
        return Status.ALLOWED;
      }

      return checkInput(clazz.getName());
    }

    Status checkInput(final String classname) {
      if (allowedClassnames.contains(classname) || allowWildcard(classname)) {
        return Status.ALLOWED;
      }
      LOG.debug("Serialization rejected: " + classname);

      return Status.REJECTED;
    }

    private boolean allowWildcard(final String classname) {
      if (allowedWildcardClassnames.isEmpty()) {
        return true;
      }

      for (int i = 0; i < allowedWildcardClassnames.size(); i++) {
        if (classname.startsWith(allowedWildcardClassnames.get(i))) {
          return true;
        }
      }

      return false;
    }
  }
}
