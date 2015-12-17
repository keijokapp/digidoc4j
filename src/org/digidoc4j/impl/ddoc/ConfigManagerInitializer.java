/* DigiDoc4J library
*
* This software is released under either the GNU Library General Public
* License (see LICENSE.LGPL).
*
* Note that the only valid version of the LGPL license as far as this
* project is concerned is the original GNU Library General Public License
* Version 2.1, February 1999
*/

package org.digidoc4j.impl.ddoc;

import java.io.Serializable;

import org.digidoc4j.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ee.sk.utils.ConfigManager;

public class ConfigManagerInitializer implements Serializable{

  private static final Logger logger = LoggerFactory.getLogger(ConfigManagerInitializer.class);
  static boolean configManagerInitialized = false;

  public void initConfigManager(AbstractConfiguration configuration) {
    if(!configManagerInitialized) {
      initializeJDigidocConfigManager(configuration);
    } else {
      logger.debug("Skipping DDoc configuration manager initialization");
    }
  }

  public static synchronized void forceInitConfigManager(AbstractConfiguration configuration) {
    logger.info("Initializing DDoc configuration manager");
    ConfigManager.init(configuration.getJDigiDocConfiguration());
    ConfigManager.addProvider();
    configManagerInitialized = true;
  }

  public static boolean isConfigManagerInitialized() {
    return configManagerInitialized;
  }

  void initializeJDigidocConfigManager(AbstractConfiguration configuration) {
    forceInitConfigManager(configuration);
  }
}
