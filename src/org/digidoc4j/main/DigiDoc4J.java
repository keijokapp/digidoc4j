/* DigiDoc4J library
*
* This software is released under either the GNU Library General Public
* License (see LICENSE.LGPL).
*
* Note that the only valid version of the LGPL license as far as this
* project is concerned is the original GNU Library General Public License
* Version 2.1, February 1999
*/

package org.digidoc4j.main;

import ee.sk.digidoc.CertValue;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.factory.DigiDocGenFactory;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.digidoc4j.*;
import org.digidoc4j.exceptions.DigiDoc4JException;
import org.digidoc4j.exceptions.SignatureNotFoundException;
import org.digidoc4j.impl.ddoc.DDocContainer;
import org.digidoc4j.impl.ddoc.DDocFacade;
import org.digidoc4j.impl.ddoc.DDocSignature;
import org.digidoc4j.impl.ddoc.ValidationResultForDDoc;
import org.digidoc4j.signers.PKCS12SignatureToken;

import java.io.File;
import java.util.List;

import static org.apache.commons.cli.OptionBuilder.withArgName;
import static org.digidoc4j.Container.DocumentType;
import static org.digidoc4j.Container.DocumentType.BDOC;
import static org.digidoc4j.Container.DocumentType.DDOC;

import org.digidoc4j.SignatureProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client commandline tool for DigiDoc4J library.
 */
public final class DigiDoc4J {

  private final static Logger logger = LoggerFactory.getLogger(DigiDoc4J.class);
  private static boolean verboseMode;
  private static boolean warnings;
  private static final String ANSI_RED = "[31m";
  private static final String ANSI_RESET = "[0m";
  private static final String EXTRACT_CMD = "extract";

  private DigiDoc4J() {
  }

  /**
   * Utility main method
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    try {
      if (System.getProperty("digidoc4j.mode") == null)
        System.setProperty("digidoc4j.mode", "PROD");
      run(args);
    } catch (DigiDoc4JUtilityException e) {
      logger.error("Errors occurred when running utility method: " + e.getMessage());
      System.err.print(e.getMessage());
      System.exit(e.getErrorCode());
    }
    logger.info("Finished running utility method");
    System.exit(0);
  }

  private static void run(String[] args) {
    Options options = createParameters();

    try {
      CommandLine commandLine = new BasicParser().parse(options, args);
      if(commandLine.hasOption("version")) {
        showVersion();
      }
      if(commandLine.hasOption("in")) {
        execute(commandLine);
      }
      if(!commandLine.hasOption("version") && !commandLine.hasOption("in")) {
        showUsage(options);
      }
    } catch (ParseException e) {
      logger.error(e.getMessage());
      showUsage(options);
    }
  }

  private static void showUsage(Options options) {
    new HelpFormatter().printHelp("digidoc4j/" + Version.VERSION, options);
    throw new DigiDoc4JUtilityException(2, "wrong parameters given");
  }

  private static void execute(CommandLine commandLine) {
    boolean fileHasChanged = false;

    verboseMode = commandLine.hasOption("verbose");
    warnings = commandLine.hasOption("warnings");
    String inputFile = commandLine.getOptionValue("in");
    DocumentType type = getContainerType(commandLine);

    checkSupportedFunctionality(commandLine);

    try {
      Container container;

      if (new File(inputFile).exists() || commandLine.hasOption("verify") || commandLine.hasOption("remove")) {
        verboseMessage("Opening container " + inputFile);
        container = ContainerOpener.open(inputFile);
      } else {
        verboseMessage("Creating new " + type + "container " + inputFile);
        container = ContainerBuilder.aContainer(type.name()).build();
      }

      if (commandLine.hasOption("add")) {
        String[] optionValues = commandLine.getOptionValues("add");
        container.addDataFile(optionValues[0], optionValues[1]);
        fileHasChanged = true;
      }

      if (commandLine.hasOption("remove")) {
        container.removeDataFile(commandLine.getOptionValue("remove"));
        fileHasChanged = true;
      }

      if(commandLine.hasOption(EXTRACT_CMD)) {
        logger.debug("Extracting data file");
        extractDataFile(container, commandLine);
      }

      if (commandLine.hasOption("profile")) {
        String profile = commandLine.getOptionValue("profile");
        try {
          SignatureProfile signatureProfile = SignatureProfile.valueOf(profile);
          container.setSignatureProfile(signatureProfile);
        } catch (IllegalArgumentException e) {
          System.out.println("Signature profile \"" + profile + "\" is unknown and will be ignored");
        }
      }

      if (commandLine.hasOption("encryption")) {
        String encryption = commandLine.getOptionValue("encryption");
        SignatureParameters signatureParameters = new SignatureParameters();
        signatureParameters.setEncryptionAlgorithm(EncryptionAlgorithm.valueOf(encryption));
        container.setSignatureParameters(signatureParameters);
      }

      if (commandLine.hasOption("pkcs12")) {
        pkcs12Sign(commandLine, container);
        fileHasChanged = true;
      }

      if (fileHasChanged) {
        container.saveAsFile(inputFile);
        if(new File(inputFile).exists()) {
          logger.debug("Container has been successfully saved to " + inputFile);
        }
        else {
          logger.warn("Container was NOT saved to " + inputFile);
        }
      }

      if (commandLine.hasOption("verify"))
        verify(container);
    } catch (DigiDoc4JUtilityException e) {
      throw e;
    } catch (DigiDoc4JException e) {
      throw new DigiDoc4JUtilityException(1, e.getMessage());
    }
  }

  private static void checkSupportedFunctionality(CommandLine commandLine) {
    if (commandLine.hasOption("add")) {
      String[] optionValues = commandLine.getOptionValues("add");
      if (optionValues.length != 2) {
        throw new DigiDoc4JUtilityException(2, "Incorrect add command");
      }
    }
    if (commandLine.hasOption(EXTRACT_CMD)) {
      String[] optionValues = commandLine.getOptionValues(EXTRACT_CMD);
      if (optionValues.length != 2) {
        throw new DigiDoc4JUtilityException(3, "Incorrect extract command");
      }
    }
  }

  private static DocumentType getContainerType(CommandLine commandLine) {
    if ("BDOC".equals(commandLine.getOptionValue("type"))) return BDOC;
    if ("DDOC".equals(commandLine.getOptionValue("type"))) return DDOC;

    String fileName = commandLine.getOptionValue("in");
    if (fileName != null) {
      if (fileName.toLowerCase().endsWith(".bdoc")) return BDOC;
      if (fileName.toLowerCase().endsWith(".ddoc")) return DDOC;
    }
    return BDOC;
  }

  private static void pkcs12Sign(CommandLine commandLine, Container container) {
    String[] optionValues = commandLine.getOptionValues("pkcs12");
    SignatureToken pkcs12Signer = new PKCS12SignatureToken(optionValues[0], optionValues[1].toCharArray());
    container.sign(pkcs12Signer);
  }

  private static void verify(Container container) {
    ValidationResult validationResult = container.validate();

    List<DigiDoc4JException> exceptions = validationResult.getContainerErrors();
    boolean isDDoc = StringUtils.equalsIgnoreCase("DDOC", container.getType());
    for (DigiDoc4JException exception : exceptions) {
      if (isDDoc && isWarning(((DDocContainer) container).getFormat(), exception))
        System.out.println("	Warning: " + exception.toString());
      else
        System.out.println((isDDoc ? "	" : "	Error: ") + exception.toString());
    }

    if (isDDoc && (((ValidationResultForDDoc) validationResult).hasFatalErrors())) {
      throw new DigiDoc4JException("DDoc container has fatal errors");
    }

    List<Signature> signatures = container.getSignatures();
    if (signatures == null) {
      throw new SignatureNotFoundException();
    }

    for (Signature signature : signatures) {
      List<DigiDoc4JException> signatureValidationResult = signature.validate();
      if (signatureValidationResult.size() == 0) {
        System.out.println("Signature " + signature.getId() + " is valid");
      } else {
        System.out.println(ANSI_RED + "Signature " + signature.getId() + " is not valid" + ANSI_RESET);
        for (DigiDoc4JException exception : signatureValidationResult) {
          System.out.println((isDDoc ? "	" : "	Error: ")
              + exception.toString());
        }
      }
      if (isDDoc && isDDocTestSignature(signature)) {
        System.out.println("Signature " + signature.getId() + " is a test signature");
      }
    }

    showWarnings(validationResult);
    verboseMessage(validationResult.getReport());

    if(validationResult.isValid()) {
      logger.info("Validation was successful. Container is valid");
    } else {
      logger.info("Validation finished. Container is NOT valid!");
      throw new DigiDoc4JException("Container is NOT valid");
    }
  }

  private static void showWarnings(ValidationResult validationResult) {
    if (warnings) {
      for (DigiDoc4JException warning : validationResult.getWarnings()) {
        System.out.println("Warning: " + warning.toString());
      }
    }
  }

  /**
   * Checks is DigiDoc4JException predefined as warning for DDOC
   *
   * @param documentFormat format SignedDoc
   * @param exception      error to check
   * @return is this exception warning for DDOC utility program
   * @see SignedDoc
   */
  public static boolean isWarning(String documentFormat, DigiDoc4JException exception) {
    int errorCode = exception.getErrorCode();
    return (errorCode == DigiDocException.ERR_DF_INV_HASH_GOOD_ALT_HASH
        || errorCode == DigiDocException.ERR_OLD_VER
        || errorCode == DigiDocException.ERR_TEST_SIGNATURE
        || errorCode == DigiDocException.WARN_WEAK_DIGEST
        || (errorCode == DigiDocException.ERR_ISSUER_XMLNS && !documentFormat.equals(SignedDoc.FORMAT_SK_XML)));
  }

  private static boolean isDDocTestSignature(Signature signature) {
    CertValue certValue = ((DDocSignature) signature).getCertValueOfType(CertValue.CERTVAL_TYPE_SIGNER);
    if (certValue != null) {
      if (DigiDocGenFactory.isTestCard(certValue.getCert())) return true;
    }
    return false;
  }

  private static Options createParameters() {
    Options options = new Options();
    options.addOption("v", "verify", false, "verify input file");
    options.addOption("verbose", "verbose", false, "verbose output");
    options.addOption("w", "warnings", false, "show warnings");
    options.addOption("version", "version", false, "show version");

    options.addOption(type());
    options.addOption(inputFile());
    options.addOption(addFile());
    options.addOption(removeFile());
    options.addOption(pkcs12Sign());
    options.addOption(signatureProfile());
    options.addOption(encryptionAlgorithm());
    options.addOption(extractDataFile());

    return options;
  }

  @SuppressWarnings("AccessStaticViaInstance")
  private static Option signatureProfile() {
    return withArgName("signatureProfile").hasArg()
        .withDescription("sets signature profile. Profile can be B_BES, LT, LT_TM or LTA")
        .withLongOpt("profile").create("p");
  }

  @SuppressWarnings("AccessStaticViaInstance")
  private static Option encryptionAlgorithm() {
    return withArgName("encryptionAlgorithm").hasArg()
        .withDescription("sets the encryption algorithm (RSA/ECDSA).").withLongOpt("encryption").create("e");
  }

  private static void verboseMessage(String message) {
    if (verboseMode)
      System.out.println(message);
  }

  @SuppressWarnings("AccessStaticViaInstance")
  private static Option pkcs12Sign() {
    return withArgName("pkcs12Keystore password").hasArgs(2).withValueSeparator(' ')
        .withDescription("sets pkcs12 keystore and keystore password").create("pkcs12");
  }

  @SuppressWarnings("AccessStaticViaInstance")
  private static Option removeFile() {
    return withArgName("file").hasArg()
        .withDescription("removes file from container").create("remove");
  }

  @SuppressWarnings("AccessStaticViaInstance")
  private static Option addFile() {
    return withArgName("file mime-type").hasArgs(2)
        .withDescription("adds file specified with mime type to container").create("add");
  }

  @SuppressWarnings("AccessStaticViaInstance")
  private static Option inputFile() {
    return withArgName("file").hasArg()
        .withDescription("opens or creates container").create("in");
  }

  @SuppressWarnings("AccessStaticViaInstance")
  private static Option type() {
    return withArgName("type").hasArg()
        .withDescription("sets container type. types can be DDOC or BDOC").withLongOpt("type").create("t");
  }

  @SuppressWarnings("AccessStaticViaInstance")
  private static Option extractDataFile() {
    return withArgName("fileName destination").hasArgs(2)
        .withDescription("extracts the file from the container to the specified destination").create(EXTRACT_CMD);
  }

  private static void showVersion() {
    System.out.println("DigiDoc4j version " + Version.VERSION);
  }

  private static void extractDataFile(Container container, CommandLine commandLine) {
    String[] optionValues = commandLine.getOptionValues(EXTRACT_CMD);
    String fileNameToExtract = optionValues[0];
    String extractPath = optionValues[1];
    boolean fileFound = false;
    for (DataFile dataFile : container.getDataFiles()) {
      if(StringUtils.equalsIgnoreCase(fileNameToExtract, dataFile.getName())) {
        logger.info("Extracting " + dataFile.getName() + " to " + extractPath);
        dataFile.saveAs(extractPath);
        fileFound = true;
      }
    }
    if(!fileFound) {
      throw new DigiDoc4JUtilityException(4, "Data file " + fileNameToExtract + " was not found in the container");
    }
  }
}
