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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.digidoc4j.exceptions.ConfigurationException;
import org.digidoc4j.exceptions.DigiDoc4JException;
import org.digidoc4j.exceptions.TslKeyStoreNotFoundException;
import org.digidoc4j.impl.bdoc.TslLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

import eu.europa.esig.dss.client.http.Protocol;

/**
 * Possibility to create custom configurations for {@link Container} implementations.
 * <p/>
 * You can specify the configuration mode, either {@link Configuration.Mode#TEST} or {@link Configuration.Mode#PROD}
 * configuration. Default is {@link Configuration.Mode#PROD}.
 * <p/>
 * It is also possible to set the mode using the System property. Setting the property "digidoc4j.mode" to "TEST" forces
 * the default mode to {@link Configuration.Mode#TEST} mode
 * <p/>
 * Configurations will be loaded from a file. The file must be in yaml format.<p/>
 * <p/>
 * <H3>Required entries of the configuration file:</H3>
 * The configuration file must contain one or more Certificate Authorities under the heading DIGIDOC_CAS
 * similar to following format (values are examples only):<br>
 * <p>
 * <pre>
 * DIGIDOC_CAS:
 * - DIGIDOC_CA:
 *     NAME: CA name
 *     TRADENAME: Tradename
 *     CERTS:
 *       - jar://certs/cert1.crt
 *       - jar://certs/cert2.crt
 *     OCSPS:
 * </pre>
 * <p/>
 * Each DIGIDOC_CA entry must contain one or more OCSP certificates under the heading "OCSPS"
 * similar to following format (values are examples only):<br>
 * <p>
 * <pre>
 *       - OCSP:
 *         CA_CN: your certificate authority common name
 *         CA_CERT: jar://your ca_cn.crt
 *         CN: your common name
 *         CERTS:
 *         - jar://certs/Your first OCSP Certifications file.crt
 *         - jar://certs/Your second OCSP Certifications file.crt
 *         URL: http://ocsp.test.test
 * </pre>
 * <p>All entries must exist and be valid. Under CERTS must be at least one entry.</p>
 * <p/>
 * <p/>
 * <H3>Optional entries of the configuration file:</H3>
 * <ul>
 * <li>CANONICALIZATION_FACTORY_IMPL: Canonicalization factory implementation.<br>
 * Default value: {@value #DEFAULT_FACTORY_IMPLEMENTATION}</li>
 * <li>CONNECTION_TIMEOUT: TSL HTTP Connection timeout (milliseconds).<br>
 * Default value: 1000  </li>
 * <li>DIGIDOC_FACTORY_IMPL: Factory implementation.<br>
 * Default value: {@value #DEFAULT_FACTORY_IMPLEMENTATION}</li>
 * <li>DATAFILE_HASHCODE_MODE: Is the datafile containing only a hash (not the actual file)?
 * Allowed values: true, false.<br>
 * Default value: {@value #DEFAULT_DATAFILE_HASHCODE_MODE}</li>
 * <li>DIGIDOC_DF_CACHE_DIR: Temporary directory to use. Default: uses system's default temporary directory</li>
 * <li>DIGIDOC_LOG4J_CONFIG: File containing Log4J configuration parameters.<br>
 * Default value: {@value #DEFAULT_LOG4J_CONFIGURATION}</li>
 * <li>DIGIDOC_MAX_DATAFILE_CACHED: Maximum datafile size that will be cached in MB.
 * Must be numeric. Set to -1 to cache all files. Set to 0 to prevent caching for all files<br>
 * Default value: {@value #DEFAULT_MAX_DATAFILE_CACHED}</li>
 * <li>DIGIDOC_NOTARY_IMPL: Notary implementation.<br>
 * Default value: {@value #DEFAULT_NOTARY_IMPLEMENTATION}</li>
 * <li>DIGIDOC_OCSP_SIGN_CERT_SERIAL: OCSP Signing certificate serial number</li>
 * <li>DIGIDOC_SECURITY_PROVIDER: Security provider.<br>
 * Default value: {@value #DEFAULT_SECURITY_PROVIDER}</li>
 * <li>DIGIDOC_SECURITY_PROVIDER_NAME: Name of the security provider.<br>
 * Default value: {@value #DEFAULT_SECURITY_PROVIDER_NAME}</li>
 * <li>DIGIDOC_TSLFAC_IMPL: TSL Factory implementation.<br>
 * Default value: {@value #DEFAULT_TSL_FACTORY_IMPLEMENTATION}</li>
 * <li>DIGIDOC_USE_LOCAL_TSL: Use local TSL? Allowed values: true, false<br>
 * Default value: {@value #DEFAULT_USE_LOCAL_TSL}</li>
 * <li>KEY_USAGE_CHECK: Should key usage be checked? Allowed values: true, false.<br>
 * Default value: {@value #DEFAULT_KEY_USAGE_CHECK}</li>
 * <li>DIGIDOC_PKCS12_CONTAINER: OCSP access certificate file</li>
 * <li>DIGIDOC_PKCS12_PASSWD: OCSP access certificate password</li>
 * <li>OCSP_SOURCE: Online Certificate Service Protocol source</li>
 * <li>PKCS11_MODULE: PKCS11 Module file</li>
 * <li>SIGN_OCSP_REQUESTS: Should OCSP requests be signed? Allowed values: true, false</li>
 * <li>TSL_LOCATION: TSL Location</li>
 * <li>TSP_SOURCE: Time Stamp Protocol source address</li>
 * <li>VALIDATION_POLICY: Validation policy source file</li>
 * <li>TSL_KEYSTORE_LOCATION: keystore location for tsl signing certificates</li>
 * <li>TSL_KEYSTORE_PASSWORD: keystore password for the keystore in TSL_KEYSTORE_LOCATION</li>
 * </ul>
 */
public class Configuration extends AbstractConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

  private static final int ONE_SECOND = 1000;
  private static final int ONE_DAY_IN_MINUTES = 24 * 60;
  public static final long ONE_MB_IN_BYTES = 1048576;

  public static final String DEFAULT_CANONICALIZATION_FACTORY_IMPLEMENTATION
      = "ee.sk.digidoc.c14n.TinyXMLCanonicalizer";
  public static final String DEFAULT_SECURITY_PROVIDER = "org.bouncycastle.jce.provider.BouncyCastleProvider";
  public static final String DEFAULT_SECURITY_PROVIDER_NAME = "BC";
  public static final String DEFAULT_LOG4J_CONFIGURATION = "./log4j.properties";
  public static final String DEFAULT_NOTARY_IMPLEMENTATION = "ee.sk.digidoc.factory.BouncyCastleNotaryFactory";
  public static final String DEFAULT_TSL_FACTORY_IMPLEMENTATION = "ee.sk.digidoc.tsl.DigiDocTrustServiceFactory";
  public static final String DEFAULT_FACTORY_IMPLEMENTATION = "ee.sk.digidoc.factory.SAXDigiDocFactory";
  public static final String DEFAULT_KEY_USAGE_CHECK = "false";
  public static final String DEFAULT_DATAFILE_HASHCODE_MODE = "false";
  public static final String DEFAULT_USE_LOCAL_TSL = "true";
  public static final String DEFAULT_MAX_DATAFILE_CACHED = "-1";
  public static final String DEFAULT_TSL_KEYSTORE_LOCATION = "keystore/keystore.jks";

  private static final String SIGN_OCSP_REQUESTS = "SIGN_OCSP_REQUESTS";
  private static final String OCSP_PKCS_12_CONTAINER = "DIGIDOC_PKCS12_CONTAINER";
  private static final String OCSP_PKCS_12_PASSWD = "DIGIDOC_PKCS12_PASSWD";

  private final Mode mode;
  private LinkedHashMap configurationFromFile;
  private String configurationInputSourceName;
  private Hashtable<String, String> jDigiDocConfiguration = new Hashtable<>();
  private ArrayList<String> inputSourceParseErrors = new ArrayList<>();
  private TSLCertificateSource tslCertificateSource;
  Map<String, String> configuration = new HashMap<>();

  /**
   * Application mode
   */
  public enum Mode {
    TEST,
    PROD
  }

  private void initDefaultValues() {
    logger.debug("");

    configuration.put("pkcs11Module", "/usr/lib/x86_64-linux-gnu/opensc-pkcs11.so");
    configuration.put("connectionTimeout", String.valueOf(ONE_SECOND));
    configuration.put("tslKeyStorePassword", "digidoc4j-password");
    configuration.put("revocationAndTimestampDeltaInMinutes", String.valueOf(ONE_DAY_IN_MINUTES));

    if (mode == Mode.TEST) {
      configuration.put("tspSource", "http://demo.sk.ee/tsa");
      configuration.put("tslLocation", "http://10.0.25.57/tsl/trusted-test-mp.xml");
      configuration.put("tslKeyStoreLocation", "keystore/test-keystore.jks");
      configuration.put("validationPolicy", "conf/test_constraint.xml");
      configuration.put("ocspSource", TEST_OCSP_URL);
      configuration.put(SIGN_OCSP_REQUESTS, "false");
      jDigiDocConfiguration.put(SIGN_OCSP_REQUESTS, "false");
    } else {
      configuration.put("tspSource", "http://tsa.sk.ee");
      configuration.put("tslLocation",
          "https://ec.europa.eu/information_society/policy/esignature/trusted-list/tl-mp.xml");
      configuration.put("tslKeyStoreLocation", DEFAULT_TSL_KEYSTORE_LOCATION);
      configuration.put("validationPolicy", "conf/constraint.xml");
      configuration.put("ocspSource", PROD_OCSP_URL);
      configuration.put(SIGN_OCSP_REQUESTS, "true");
      jDigiDocConfiguration.put(SIGN_OCSP_REQUESTS, "true");
    }
    logger.debug(mode + "configuration:\n" + configuration);

    loadInitialConfigurationValues();
  }

  /**
   * Get OCSP access certificate filename
   *
   * @return filename for the OCSP access certificate
   */
  @Override
  public String getOCSPAccessCertificateFileName() {
    logger.debug("Loading OCSPAccessCertificateFile");
    String ocspAccessCertificateFile = getConfigurationParameter("OCSPAccessCertificateFile");
    logger.debug("OCSPAccessCertificateFile " + ocspAccessCertificateFile + " loaded");
    return ocspAccessCertificateFile;
  }

  /**
   * Get OSCP access certificate password
   *
   * @return password
   */
  @Override
  public char[] getOCSPAccessCertificatePassword() {
    logger.debug("Loading OCSPAccessCertificatePassword");
    char[] result = {};
    String password = getConfigurationParameter("OCSPAccessCertificatePassword");
    if (isNotEmpty(password)) {
      result = password.toCharArray();
    }
    logger.debug("OCSPAccessCertificatePassword loaded");
    return result;
  }

  /**
   * Set OCSP access certificate filename
   *
   * @param fileName filename for the OCSP access certficate
   */
  public void setOCSPAccessCertificateFileName(String fileName) {
    logger.debug("Setting OCSPAccessCertificateFileName: " + fileName);
    setConfigurationParameter("OCSPAccessCertificateFile", fileName);
    jDigiDocConfiguration.put(OCSP_PKCS_12_CONTAINER, fileName);
    logger.debug("OCSPAccessCertificateFile is set");
  }

  /**
   * Set OCSP access certificate password
   *
   * @param password password to set
   */
  public void setOCSPAccessCertificatePassword(char[] password) {
    logger.debug("Setting OCSPAccessCertificatePassword: ");
    String value = String.valueOf(password);
    setConfigurationParameter("OCSPAccessCertificatePassword", value);
    jDigiDocConfiguration.put(OCSP_PKCS_12_PASSWD, value);
    logger.debug("OCSPAccessCertificatePassword is set");
  }

  public void setSignOCSPRequests(boolean shouldSignOcspRequests) {
    logger.debug("Should sign OCSP requests: " + shouldSignOcspRequests);
    String valueToSet = String.valueOf(shouldSignOcspRequests);
    setConfigurationParameter(SIGN_OCSP_REQUESTS, valueToSet);
    jDigiDocConfiguration.put(SIGN_OCSP_REQUESTS, valueToSet);
  }

  /**
   * Create new configuration
   */
  public Configuration() {
    mode = ("TEST".equalsIgnoreCase(System.getProperty("digidoc4j.mode")) ? Mode.TEST : Mode.PROD);
    loadConfiguration("digidoc4j.yaml");

    initDefaultValues();

    logger.info("Configuration loaded for " + mode + " mode");
  }

  /**
   * Create new configuration for application mode specified
   *
   * @param mode Application mode
   */
  public Configuration(Mode mode) {
    logger.debug("Mode: " + mode);
    this.mode = mode;
    loadConfiguration("digidoc4j.yaml");

    initDefaultValues();

    logger.info("Configuration loaded for " + mode + " mode");
  }

  /**
   * Add configuration settings from a stream. After loading closes stream.
   *
   * @param stream Input stream
   * @return configuration hashtable
   */
  public Hashtable<String, String> loadConfiguration(InputStream stream) {
    configurationInputSourceName = "stream";

    return loadConfigurationSettings(stream);
  }

  /**
   * Add configuration settings from a file
   *
   * @param file File name
   * @return configuration hashtable
   */
  public Hashtable<String, String> loadConfiguration(String file) {
    logger.info("Loading configuration from file " + file);
    configurationInputSourceName = file;
    InputStream resourceAsStream = null;

    try {
      resourceAsStream = new FileInputStream(file);
    } catch (FileNotFoundException e) {
      logger.info("Configuration file " + file + " not found. Trying to search from jar file.");
    }

    if (resourceAsStream == null) {
      resourceAsStream = getResourceAsStream(file);
    }
    return loadConfigurationSettings(resourceAsStream);
  }

  private Hashtable<String, String> loadConfigurationSettings(InputStream stream) {
    configurationFromFile = new LinkedHashMap();
    Yaml yaml = new Yaml();

    try {
      configurationFromFile = (LinkedHashMap) yaml.load(stream);
    } catch (Exception e) {
      ConfigurationException exception = new ConfigurationException("Configuration from "
          + configurationInputSourceName + " is not correctly formatted");
      logger.error(exception.getMessage());
      throw exception;
    }

    IOUtils.closeQuietly(stream);

    return mapToJDigiDocConfiguration();
  }

  private InputStream getResourceAsStream(String certFile) {
    InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(certFile);
    if (resourceAsStream == null) {
      String message = "File " + certFile + " not found in classpath.";
      logger.error(message);
      throw new ConfigurationException(message);
    }

    return resourceAsStream;
  }

  /**
   * Returns configuration needed for JDigiDoc library
   *
   * @return configuration values
   */
  @Override
  public Hashtable<String, String> getJDigiDocConfiguration() {
    return jDigiDocConfiguration;
  }

  /**
   * Gives back all configuration parameters needed for jDigiDoc
   *
   * @return Hashtable containing jDigiDoc configuration parameters
   */

  private Hashtable<String, String> mapToJDigiDocConfiguration() {
    logger.debug("loading JDigiDoc configuration");

    inputSourceParseErrors = new ArrayList<>();

    loadInitialConfigurationValues();
    loadCertificateAuthoritiesAndCertificates();
    reportFileParseErrors();

    return jDigiDocConfiguration;
  }

  private void loadCertificateAuthoritiesAndCertificates() {
    logger.debug("");
    @SuppressWarnings("unchecked")
    ArrayList<LinkedHashMap> digiDocCAs = (ArrayList<LinkedHashMap>) configurationFromFile.get("DIGIDOC_CAS");
    if (digiDocCAs == null) {
      String errorMessage = "Empty or no DIGIDOC_CAS entry";
      logError(errorMessage);
      return;
    }

    int numberOfDigiDocCAs = digiDocCAs.size();
    jDigiDocConfiguration.put("DIGIDOC_CAS", String.valueOf(numberOfDigiDocCAs));
    for (int i = 0; i < numberOfDigiDocCAs; i++) {
      String caPrefix = "DIGIDOC_CA_" + (i + 1);
      LinkedHashMap digiDocCA = (LinkedHashMap) digiDocCAs.get(i).get("DIGIDOC_CA");
      if (digiDocCA == null) {
        String errorMessage = "Empty or no DIGIDOC_CA for entry " + (i + 1);
        logError(errorMessage);
      } else {
        loadCertificateAuthorityCerts(digiDocCA, caPrefix);
        loadOCSPCertificates(digiDocCA, caPrefix);
      }
    }
  }

  private void logError(String errorMessage) {
    logger.error(errorMessage);
    inputSourceParseErrors.add(errorMessage);
  }

  private void reportFileParseErrors() {
    logger.debug("");
    if (inputSourceParseErrors.size() > 0) {
      StringBuilder errorMessage = new StringBuilder();
      errorMessage.append("Configuration from ");
      errorMessage.append(configurationInputSourceName);
      errorMessage.append(" contains error(s):\n");
      for (String message : inputSourceParseErrors) {
        errorMessage.append(message);
      }
      throw new ConfigurationException(errorMessage.toString());
    }
  }

  private void loadInitialConfigurationValues() {
    logger.debug("");
    setJDigiDocConfigurationValue("DIGIDOC_LOG4J_CONFIG", DEFAULT_LOG4J_CONFIGURATION);
    setJDigiDocConfigurationValue("DIGIDOC_SECURITY_PROVIDER", DEFAULT_SECURITY_PROVIDER);
    setJDigiDocConfigurationValue("DIGIDOC_SECURITY_PROVIDER_NAME", DEFAULT_SECURITY_PROVIDER_NAME);
    setJDigiDocConfigurationValue("KEY_USAGE_CHECK", DEFAULT_KEY_USAGE_CHECK);
    setJDigiDocConfigurationValue("DIGIDOC_OCSP_SIGN_CERT_SERIAL", "");
    setJDigiDocConfigurationValue("DATAFILE_HASHCODE_MODE", DEFAULT_DATAFILE_HASHCODE_MODE);
    setJDigiDocConfigurationValue("CANONICALIZATION_FACTORY_IMPL", DEFAULT_CANONICALIZATION_FACTORY_IMPLEMENTATION);
    setJDigiDocConfigurationValue("DIGIDOC_MAX_DATAFILE_CACHED", DEFAULT_MAX_DATAFILE_CACHED);
    setJDigiDocConfigurationValue("DIGIDOC_USE_LOCAL_TSL", DEFAULT_USE_LOCAL_TSL);
    setJDigiDocConfigurationValue("DIGIDOC_NOTARY_IMPL", DEFAULT_NOTARY_IMPLEMENTATION);
    setJDigiDocConfigurationValue("DIGIDOC_TSLFAC_IMPL", DEFAULT_TSL_FACTORY_IMPLEMENTATION);
    setJDigiDocConfigurationValue("DIGIDOC_OCSP_RESPONDER_URL", getOcspSource());
    setJDigiDocConfigurationValue("DIGIDOC_FACTORY_IMPL", DEFAULT_FACTORY_IMPLEMENTATION);
    setJDigiDocConfigurationValue("DIGIDOC_DF_CACHE_DIR", null);

    setConfigurationValue("TSL_LOCATION", "tslLocation");
    setConfigurationValue("TSP_SOURCE", "tspSource");
    setConfigurationValue("VALIDATION_POLICY", "validationPolicy");
    setConfigurationValue("PKCS11_MODULE", "pkcs11Module");
    setConfigurationValue("OCSP_SOURCE", "ocspSource");
    setConfigurationValue(OCSP_PKCS_12_CONTAINER, "OCSPAccessCertificateFile");
    setConfigurationValue(OCSP_PKCS_12_PASSWD, "OCSPAccessCertificatePassword");
    setConfigurationValue("CONNECTION_TIMEOUT", "connectionTimeout");
    setConfigurationValue(SIGN_OCSP_REQUESTS, SIGN_OCSP_REQUESTS);
    setConfigurationValue("TSL_KEYSTORE_LOCATION", "tslKeyStoreLocation");
    setConfigurationValue("TSL_KEYSTORE_PASSWORD", "tslKeyStorePassword");
    setConfigurationValue("REVOCATION_AND_TIMESTAMP_DELTA_IN_MINUTES", "revocationAndTimestampDeltaInMinutes");

    setJDigiDocConfigurationValue(SIGN_OCSP_REQUESTS, Boolean.toString(hasToBeOCSPRequestSigned()));
    setJDigiDocConfigurationValue(OCSP_PKCS_12_CONTAINER, getOCSPAccessCertificateFileName());

    initOcspAccessCertPasswordForJDigidoc();
  }

  private void setConfigurationValue(String fileKey, String configurationKey) {
    if (configurationFromFile == null) return;
    Object fileValue = configurationFromFile.get(fileKey);
    if (fileValue != null) {
      configuration.put(configurationKey, fileValue.toString());
    }
  }

  private void setJDigiDocConfigurationValue(String key, String defaultValue) {
    String value = defaultIfNull(key, defaultValue);
    if (value != null) {
      jDigiDocConfiguration.put(key, value);
    }
  }

  /**
   * Enables big files support. Sets limit in MB when handling files are creating temporary file for streaming in
   * container creation and adding data files.
   * <p/>
   * Used by DigiDoc4J and by JDigiDoc.
   *
   * @param maxFileSizeCachedInMB Maximum size in MB
   */
  public void enableBigFilesSupport(long maxFileSizeCachedInMB) {
    logger.debug("Set maximum datafile cached to: " + maxFileSizeCachedInMB);
    String value = Long.toString(maxFileSizeCachedInMB);
    if (isValidIntegerParameter("DIGIDOC_MAX_DATAFILE_CACHED", value)) {
      jDigiDocConfiguration.put("DIGIDOC_MAX_DATAFILE_CACHED", value);
    }
  }

  /**
   * Returns configuration item must be OCSP request signed. Reads it from configuration parameter SIGN_OCSP_REQUESTS.
   * Default value is true for {@link Configuration.Mode#PROD} and false for {@link Configuration.Mode#TEST}
   *
   * @return must be OCSP request signed
   */
  public boolean hasToBeOCSPRequestSigned() {
    String signOcspRequests = getConfigurationParameter(SIGN_OCSP_REQUESTS);
    return StringUtils.equalsIgnoreCase("true", signOcspRequests);
  }

  /**
   * Get the maximum size of data files to be cached. Used by DigiDoc4J and by JDigiDoc.
   *
   * @return Size in MB. if size < 0 no caching is used
   */
  @Override
  public long getMaxDataFileCachedInMB() {
    String maxDataFileCached = jDigiDocConfiguration.get("DIGIDOC_MAX_DATAFILE_CACHED");
    logger.debug("Maximum datafile cached in MB: " + maxDataFileCached);

    if (maxDataFileCached == null) return CACHE_ALL_DATA_FILES;
    return Long.parseLong(maxDataFileCached);
  }

  private String defaultIfNull(String configParameter, String defaultValue) {
    logger.debug("Parameter: " + configParameter);
    if (configurationFromFile == null) return defaultValue;
    Object value = configurationFromFile.get(configParameter);
    if (value != null) {
      return valueIsAllowed(configParameter, value.toString()) ? value.toString() : "";
    }
    String configuredValue = jDigiDocConfiguration.get(configParameter);
    return configuredValue != null ? configuredValue : defaultValue;
  }

  private boolean valueIsAllowed(String configParameter, String value) {
    logger.debug("Parameter: " + configParameter + ", value: " + value);

    List<String> mustBeBooleans =
        asList(SIGN_OCSP_REQUESTS, "KEY_USAGE_CHECK", "DATAFILE_HASHCODE_MODE", "DIGIDOC_USE_LOCAL_TSL");
    List<String> mustBeIntegers =
        asList("DIGIDOC_MAX_DATAFILE_CACHED");

    boolean errorFound = false;
    if (mustBeBooleans.contains(configParameter)) {
      errorFound = !(isValidBooleanParameter(configParameter, value));
    }

    if (mustBeIntegers.contains(configParameter)) {
      errorFound = !(isValidIntegerParameter(configParameter, value)) || errorFound;
    }
    return (!errorFound);
  }

  private boolean isValidBooleanParameter(String configParameter, String value) {
    if (!("true".equals(value.toLowerCase()) || "false".equals(value.toLowerCase()))) {
      String errorMessage = "Configuration parameter " + configParameter + " should be set to true or false"
          + " but the actual value is: " + value + ".";
      logError(errorMessage);
      return false;
    }
    return true;
  }

  private boolean isValidIntegerParameter(String configParameter, String value) {
    Integer parameterValue;

    try {
      parameterValue = Integer.parseInt(value);
    } catch (Exception e) {
      String errorMessage = "Configuration parameter " + configParameter + " should have an integer value"
          + " but the actual value is: " + value + ".";
      logError(errorMessage);
      return false;
    }

    if (configParameter.equals("DIGIDOC_MAX_DATAFILE_CACHED") && parameterValue < -1) {
      String errorMessage = "Configuration parameter " + configParameter + " should be greater or equal -1"
          + " but the actual value is: " + value + ".";
      logError(errorMessage);
      return false;
    }

    return true;
  }

  private void loadOCSPCertificates(LinkedHashMap digiDocCA, String caPrefix) {
    logger.debug("");
    String errorMessage;

    @SuppressWarnings("unchecked")
    ArrayList<LinkedHashMap> ocsps = (ArrayList<LinkedHashMap>) digiDocCA.get("OCSPS");
    if (ocsps == null) {
      errorMessage = "No OCSPS entry found or OCSPS entry is empty. Configuration from: "
          + configurationInputSourceName;
      logError(errorMessage);
      return;
    }

    int numberOfOCSPCertificates = ocsps.size();
    jDigiDocConfiguration.put(caPrefix + "_OCSPS", String.valueOf(numberOfOCSPCertificates));

    for (int i = 1; i <= numberOfOCSPCertificates; i++) {
      String prefix = caPrefix + "_OCSP" + i;
      LinkedHashMap ocsp = ocsps.get(i - 1);

      List<String> entries = asList("CA_CN", "CA_CERT", "CN", "URL");
      for (String entry : entries) {
        if (!loadOCSPCertificateEntry(entry, ocsp, prefix)) {
          errorMessage = "OCSPS list entry " + i + " does not have an entry for " + entry
              + " or the entry is empty\n";
          logError(errorMessage);
        }
      }

      if (!getOCSPCertificates(prefix, ocsp)) {
        errorMessage = "OCSPS list entry " + i + " does not have an entry for CERTS or the entry is empty\n";
        logError(errorMessage);
      }
    }
  }

  private boolean loadOCSPCertificateEntry(String ocspsEntryName, LinkedHashMap ocsp, String prefix) {
    Object ocspEntry = ocsp.get(ocspsEntryName);
    if (ocspEntry == null) return false;
    jDigiDocConfiguration.put(prefix + "_" + ocspsEntryName, ocspEntry.toString());
    return true;
  }

  @SuppressWarnings("unchecked")
  private boolean getOCSPCertificates(String prefix, LinkedHashMap ocsp) {
    ArrayList<String> certificates = (ArrayList<String>) ocsp.get("CERTS");
    if (certificates == null) return false;
    for (int j = 0; j < certificates.size(); j++) {
      if (j == 0) {
        jDigiDocConfiguration.put(prefix + "_CERT", certificates.get(0));
      } else {
        jDigiDocConfiguration.put(prefix + "_CERT_" + j, certificates.get(j));
      }
    }
    return true;
  }

  private void loadCertificateAuthorityCerts(LinkedHashMap digiDocCA, String caPrefix) {
    logger.debug("");
    ArrayList<String> certificateAuthorityCerts = getCACertsAsArray(digiDocCA);

    jDigiDocConfiguration.put(caPrefix + "_NAME", digiDocCA.get("NAME").toString());
    jDigiDocConfiguration.put(caPrefix + "_TRADENAME", digiDocCA.get("TRADENAME").toString());
    int numberOfCACertificates = certificateAuthorityCerts.size();
    jDigiDocConfiguration.put(caPrefix + "_CERTS", String.valueOf(numberOfCACertificates));

    for (int i = 0; i < numberOfCACertificates; i++) {
      String certFile = certificateAuthorityCerts.get(i);
      jDigiDocConfiguration.put(caPrefix + "_CERT" + (i + 1), certFile);
    }
  }

  @SuppressWarnings("unchecked")
  private ArrayList<String> getCACertsAsArray(LinkedHashMap digiDocCa) {
    return (ArrayList<String>) digiDocCa.get("CERTS");
  }

  String getTslLocation() {
    String urlString = getConfigurationParameter("tslLocation");
    if (!Protocol.isFileUrl(urlString)) return urlString;
    try {
      String filePath = new URL(urlString).getPath();
      if (!new File(filePath).exists()) {
        URL resource = getClass().getClassLoader().getResource(filePath);
        if (resource != null)
          urlString = resource.toString();
      }
    } catch (MalformedURLException e) {
      logger.warn(e.getMessage());
    }
    return urlString;
  }

  /**
   * Set the TSL certificate source.
   *
   * @param certificateSource TSL certificate source
   *                          When certificateSource equals null then getTSL() will load the TSL according to the TSL
   *                          location specified .
   */

  public void setTSL(TSLCertificateSource certificateSource) {
    this.tslCertificateSource = certificateSource;
  }

  /**
   * Loads TSL certificates
   * If configuration mode is TEST then TSL signature is not checked.
   *
   * @return TSL source
   */
  @Override
  public TSLCertificateSource getTSL() {
    if (tslCertificateSource != null) {
      logger.debug("Using TSL cached copy");
      return tslCertificateSource;
    }
    String tslLocation = getTslLocation();
    File tslKeystoreFile = getTslKeystoreFile();
    String tslKeyStorePassword = getTslKeyStorePassword();
    boolean checkSignature = mode == Mode.TEST ? false : true;

    TslLoader tslLoader = new TslLoader(tslLocation, tslKeystoreFile, tslKeyStorePassword);
    tslLoader.setCheckSignature(checkSignature);
    tslLoader.setConnectionTimeout(getConnectionTimeout());
    tslCertificateSource = tslLoader.createTSL();
    return tslCertificateSource;
  }

  private File getTslKeystoreFile() throws TslKeyStoreNotFoundException{
    try {
      String keystoreLocation = getTslKeyStoreLocation();
      if (Files.exists(Paths.get(keystoreLocation))) {
        return new File(keystoreLocation);
      }
      File tempFile = File.createTempFile("temp-tsl-keystore", ".jks");
      InputStream in = getClass().getClassLoader().getResourceAsStream(keystoreLocation);
      if (in == null) {
        logger.error("keystore not found in location " + keystoreLocation);
        throw new TslKeyStoreNotFoundException("keystore not found in location " + keystoreLocation);
      }
      FileUtils.copyInputStreamToFile(in, tempFile);
      return tempFile;
    } catch (IOException e) {
      logger.error(e.getMessage());
      throw new TslKeyStoreNotFoundException(e.getMessage());
    }
  }

  /**
   * Set the TSL location.
   * TSL can be loaded from file (file://) or from web (http://). If file protocol is used then
   * first try is to locate file from this location if file does not exist then it tries to load
   * relatively from classpath.
   * <p/>
   * Setting new location clears old values
   * <p/>
   * Windows wants it in file:DRIVE:/directories/tsl-file.xml format
   *
   * @param tslLocation TSL Location to be used
   */
  public void setTslLocation(String tslLocation) {
    logger.debug("Set TSL location: " + tslLocation);
    setConfigurationParameter("tslLocation", tslLocation);
    tslCertificateSource = null;
  }

  /**
   * Get the TSP Source
   *
   * @return TSP Source
   */
  @Override
  public String getTspSource() {
    String tspSource = getConfigurationParameter("tspSource");
    logger.debug("TSP Source: " + tspSource);
    return tspSource;
  }

  /**
   * Set HTTP connection timeout
   * @param connectionTimeout connection timeout in milliseconds
   */
  public void setConnectionTimeout(int connectionTimeout) {
    logger.debug("Set connection timeout to " + connectionTimeout + " ms");
    setConfigurationParameter("connectionTimeout", String.valueOf(connectionTimeout));
  }

  /**
   * Get HTTP connection timeout
   *
   * @return connection timeout in milliseconds
   */
  @Override
  public int getConnectionTimeout() {
    return Integer.parseInt(getConfigurationParameter("connectionTimeout"));
  }

  /**
   * Set the TSP Source
   *
   * @param tspSource TSPSource to be used
   */
  public void setTspSource(String tspSource) {
    logger.debug("Set TSP source: " + tspSource);
    setConfigurationParameter("tspSource", tspSource);
  }

  /**
   * Get the OCSP Source
   *
   * @return OCSP Source
   */
  @Override
  public String getOcspSource() {
    String ocspSource = getConfigurationParameter("ocspSource");
    logger.debug("OCSP source: " + ocspSource);
    return ocspSource;
  }

  /**
   * Set the KeyStore Location that holds potential TSL Signing certificates
   * @param tslKeyStoreLocation KeyStore location to use
   */
  public void setTslKeyStoreLocation(String tslKeyStoreLocation) {
    logger.debug("Set tsl KeyStore Location: " + tslKeyStoreLocation);
    setConfigurationParameter("tslKeyStoreLocation", tslKeyStoreLocation);
  }

  /**
   * Get the Location to Keystore that holds potential TSL Signing certificates
   * @return KeyStore Location
   */
  @Override
  public String getTslKeyStoreLocation() {
    String keystoreLocation = getConfigurationParameter("tslKeyStoreLocation");
    logger.debug("tsl KeyStore Location: " + keystoreLocation);
    return keystoreLocation;
  }

  /**
   * Set the password for Keystore that holds potential TSL Signing certificates
   * @param tslKeyStorePassword Keystore password
   */
  public void setTslKeyStorePassword(String tslKeyStorePassword) {
    logger.debug("Set tsl KeyStore Password: " + tslKeyStorePassword);
    setConfigurationParameter("tslKeyStorePassword", tslKeyStorePassword);
  }

  /**
   * Get the password for Keystore that holds potential TSL Signing certificates
   * @return Tsl Keystore password
   */
  @Override
  public String getTslKeyStorePassword() {
    String keystorePassword = getConfigurationParameter("tslKeyStorePassword");
    logger.debug("tsl KeyStore Password: " + keystorePassword);
    return keystorePassword;
  }

  /**
   * Set the OCSP source
   *
   * @param ocspSource OCSP Source to be used
   */
  public void setOcspSource(String ocspSource) {
    logger.debug("Set OCSP source: " + ocspSource);
    setConfigurationParameter("ocspSource", ocspSource);
  }

  /**
   * Get the validation policy
   *
   * @return Validation policy
   */
  @Override
  public String getValidationPolicy() {
    String validationPolicy = getConfigurationParameter("validationPolicy");
    logger.debug("Validation policy: " + validationPolicy);
    return validationPolicy;
  }

  /**
   * Set the validation policy
   *
   * @param validationPolicy Policy to be used
   */
  public void setValidationPolicy(String validationPolicy) {
    logger.debug("Set validation policy: " + validationPolicy);
    setConfigurationParameter("validationPolicy", validationPolicy);
  }

  /**
   * Get the PKCS11 Module path
   *
   * @return path
   */
  @Override
  public String getPKCS11ModulePath() {
    String path = getConfigurationParameter("pkcs11Module");
    logger.debug("PKCS11 module path: " + path);
    return path;
  }

  public int getRevocationAndTimestampDeltaInMinutes() {
    String timeDelta = getConfigurationParameter("revocationAndTimestampDeltaInMinutes");
    logger.debug("Revocation and timestamp delta in minutes: " + timeDelta);
    return Integer.parseInt(timeDelta);
  }
  public void setRevocationAndTimestampDeltaInMinutes(int timeInMinutes) {
    logger.debug("Set revocation and timestamp delta in minutes: " + timeInMinutes);
    setConfigurationParameter("revocationAndTimestampDeltaInMinutes", String.valueOf(timeInMinutes));
  }

  private void setConfigurationParameter(String key, String value) {
    logger.debug("Key: " + key + ", value: " + value);
    configuration.put(key, value);
  }

  private String getConfigurationParameter(String key) {
    logger.debug("Key: " + key);
    String value = configuration.get(key);
    logger.debug("Value: " + value);
    return value;
  }

  /**
   * @return true when configuration is Configuration.Mode.TEST
   * @see Configuration.Mode#TEST
   */
  public boolean isTest() {
    boolean isTest = mode == Mode.TEST;
    logger.debug("Is test: " + isTest);
    return isTest;
  }

  /**
   * Clones configuration
   *
   * @return new configuration object
   */
  public Configuration copy() {
    return (Configuration) super.copy();
  }

  private void initOcspAccessCertPasswordForJDigidoc() {
    char[] ocspAccessCertificatePassword = getOCSPAccessCertificatePassword();
    if(ocspAccessCertificatePassword != null && ocspAccessCertificatePassword.length > 0) {
      setJDigiDocConfigurationValue(OCSP_PKCS_12_PASSWD, String.valueOf(ocspAccessCertificatePassword));
    }
  }
}

