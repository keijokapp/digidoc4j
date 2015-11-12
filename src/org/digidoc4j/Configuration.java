/* DigiDoc4J library
*
* This software is released under either the GNU Library General Public
* License (see LICENSE.LGPL).
*
* Note that the only valid version of the LGPL license as far as this
* project is concerned is the original GNU Library General Public License
* Version 2.1, February 1999
*/

package org.digidoc4j;

import java.util.Hashtable;
import java.io.Serializable;


public interface Configuration extends Serializable {
  public static final String TEST_OCSP_URL = "http://demo.sk.ee/ocsp";
  public static final String PROD_OCSP_URL = "http://ocsp.sk.ee/";


  /**
   * Are requirements met for signing OCSP certificate?
   * @return value indicating if requirements are met
   */
  public boolean isOCSPSigningConfigurationAvailable();

  /**
   * Get OCSP access certificate filename
   * @return filename for the OCSP access certificate
   */
  public String getOCSPAccessCertificateFileName();

  /**
   * Get OSCP access certificate password
   * @return password
   */
  public char[] getOCSPAccessCertificatePassword();

  /**
   * Returns configuration needed for JDigiDoc library
   * @return configuration values
   */
  public Hashtable<String, String> getJDigiDocConfiguration();

  /**
   * @return is big file support enabled
   */
  public boolean isBigFilesSupportEnabled();

  /**
   * Returns configuration item must be OCSP request signed.
   * @return must be OCSP request signed
   */
  public boolean hasToBeOCSPRequestSigned();

  /**
   * Get the maximum size of data files to be cached. Used by DigiDoc4J and by JDigiDoc.
   * @return Size in MB. if size < 0 no caching is used
   */
  public long getMaxDataFileCachedInMB();

  /**
   * Get the maximum size of data files to be cached. Used by DigiDoc4J and by JDigiDoc.
   * @return Size in MB. if size < 0 no caching is used
   */
  public long getMaxDataFileCachedInBytes();

  /**
   * Get TSL location
   * @returns location of TSL data
   */
  public String getTslLocation();

  /**
   * Loads TSL certificates
   * If configuration mode is TEST then TSL signature is not checked.
   * @return TSL source
   */
  public TSLCertificateSource getTSL();

  /**
   * Get the TSP Source
   * @return TSP Source
   */
  public String getTspSource();

  /**
   * Get HTTP connection timeout
   * @return connection timeout in milliseconds
   */
  public int getConnectionTimeout();

  /**
   * Get the OCSP Source
   * @return OCSP Source
   */
  public String getOcspSource();

  /**
   * Get the validation policy
   * @return Validation policy
   */
  public String getValidationPolicy();

  /**
   * Get the PKCS11 Module path
   * @return path
   */
  public String getPKCS11ModulePath();

  /**
   * Clone configuration
   * @return new configuration object
   */
  public Configuration copy();
  
  /**
   * Get the Location to Keystore that holds potential TSL Signing certificates
   * @return KeyStore Location
   */
  public String getTslKeyStoreLocation();

  /**
   * Get the password for Keystore that holds potential TSL Signing certificates
   * @return Tsl Keystore password
   */
  public String getTslKeyStorePassword();

}

