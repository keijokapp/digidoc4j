package org.digidoc4j;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;

import org.apache.commons.io.IOUtils;
import org.digidoc4j.exceptions.DigiDoc4JException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;


public abstract class AbstractConfiguration implements Serializable {
  private static Logger logger = LoggerFactory.getLogger(AbstractConfiguration.class);
  public static final long ONE_MB_IN_BYTES = 1048576;
  public static final long CACHE_ALL_DATA_FILES = -1;
  public static final long CACHE_NO_DATA_FILES = 0;
  protected static final int ONE_SECOND = 1000;

  public static final String TEST_OCSP_URL = "http://demo.sk.ee/ocsp";
  public static final String PROD_OCSP_URL = "http://ocsp.sk.ee/";

  /**
   * Get availability of OCSP signing certificate
   * @return value indicating if requirements are met
   */
  public boolean isOCSPSigningConfigurationAvailable() {
    boolean available = isNotEmpty(getOCSPAccessCertificateFileName()) && getOCSPAccessCertificatePassword().length != 0;
    logger.debug("Is OCSP signing configuration available: " + available);
    return available;
  }

  /**
   * Get OCSP access certificate filename
   * @return filename for the OCSP access certificate
   */
  public String getOCSPAccessCertificateFileName() {
    return "";
  }

  /**
   * Get OSCP access certificate password
   * @return password
   */
  public char[] getOCSPAccessCertificatePassword() {
    return new char[] { };
  }

  /**
   * Return configuration needed for JDigiDoc library
   * @return configuration values
   */
  public Hashtable<String, String> getJDigiDocConfiguration() {
    return new Hashtable<>();
  }

  /**
   * Return whether big file support is enabled or not
   * @return is big file support enabled
   */
  public boolean isBigFilesSupportEnabled() {
    return getMaxDataFileCachedInMB() >= 0;
  }

  /**
   * Return whether OCSP request signing is needed or not
   * @return is OCSP request signing needed
   */
  public abstract boolean hasToBeOCSPRequestSigned();

  /**
   * Get the maximum size of data files to be cached. Used by DigiDoc4J and by JDigiDoc.
   * @return Size in MB. if size < 0 no caching is used
   */
  public abstract long getMaxDataFileCachedInMB();

  /**
   * Get the maximum size of data files to be cached. Used by DigiDoc4J and by JDigiDoc.
   * @return Size in MB. if size < 0 no caching is used
   */
  public long getMaxDataFileCachedInBytes() {
    long maxDataFileCachedInMB = getMaxDataFileCachedInMB();
    if (maxDataFileCachedInMB == CACHE_ALL_DATA_FILES) {
      return CACHE_ALL_DATA_FILES;
    } else {
      return (maxDataFileCachedInMB * ONE_MB_IN_BYTES);
    }
  }

  /**
   * Loads TSL certificates
   * @return TSL source
   */
  public abstract TSLCertificateSource getTSL();

  /**
   * Get the TSP Source
   * @return TSP Source
   */
  public abstract String getTspSource();

  /**
   * Get HTTP connection timeout
   * @return connection timeout in milliseconds
   */
  public int getConnectionTimeout() {
    return ONE_SECOND;
  }

  /**
   * Get the OCSP Source
   * @return OCSP Source
   */
  public abstract String getOcspSource();

  /**
   * Get the Location to Keystore that holds potential TSL Signing certificates
   * @return KeyStore Location
   */
  public abstract String getTslKeyStoreLocation();

  /**
   * Get the password for Keystore that holds potential TSL Signing certificates
   * @return Tsl Keystore password
   */
  public abstract String getTslKeyStorePassword();

  /**
   * Get the validation policy
   *
   * @return Validation policy
   */
  public abstract String getValidationPolicy();

  /**
   * Get the PKCS11 Module path
   * @return path
   */
  public String getPKCS11ModulePath() {
    String path = "/usr/lib/x86_64-linux-gnu/opensc-pkcs11.so";
    logger.debug("PKCS11 module path: " + path);
    return path;
  }

  /**
   * Clone configuration
   * @return new configuration object
   */
  public AbstractConfiguration copy() {
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    AbstractConfiguration copyConfiguration = null;
    // deep copy
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      oos = new ObjectOutputStream(bos);
      oos.writeObject(this);
      oos.flush();
      ByteArrayInputStream bin =
          new ByteArrayInputStream(bos.toByteArray());
      ois = new ObjectInputStream(bin);
      copyConfiguration = (AbstractConfiguration) ois.readObject();
    } catch (Exception e) {
      throw new DigiDoc4JException(e);
    } finally {
      IOUtils.closeQuietly(oos);
      IOUtils.closeQuietly(ois);
      IOUtils.closeQuietly(bos);
    }
    return copyConfiguration;
  }
}
