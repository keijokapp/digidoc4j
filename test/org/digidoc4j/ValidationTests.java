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

import static org.digidoc4j.testutils.TestHelpers.containsErrorMessage;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.digidoc4j.exceptions.ContainerWithoutSignaturesException;
import org.digidoc4j.exceptions.InvalidTimestampException;
import org.digidoc4j.exceptions.TimestampAfterOCSPResponseTimeException;
import org.digidoc4j.impl.DigiDoc4JTestHelper;
import org.digidoc4j.testutils.TSLHelper;
import org.junit.Test;

public class ValidationTests extends DigiDoc4JTestHelper {

  @Test
  public void asicValidationShouldFail_ifTimeStampHashDoesntMatchSignature() throws Exception {
    ValidationResult result = validateContainer("testFiles/TS-02_23634_TS_wrong_SignatureValue.asice");
    assertFalse(result.isValid());
    assertTrue(containsErrorMessage(result.getErrors(), InvalidTimestampException.MESSAGE));
  }

  @Test(expected = ContainerWithoutSignaturesException.class)
  public void asicContainerWithoutSignatures_isNotValid() throws Exception {
    ValidationResult result = validateContainer("testFiles/asics_without_signatures.bdoc");
    assertFalse(result.isValid());
  }

  @Test
  public void asicOcspTimeShouldBeAfterTimestamp() throws Exception {
    ValidationResult result = validateContainer("testFiles/TS-08_23634_TS_OCSP_before_TS.asice");
    assertFalse(result.isValid());
    assertTrue(result.getErrors().size() >= 1);
    assertTrue(containsErrorMessage(result.getErrors(), TimestampAfterOCSPResponseTimeException.MESSAGE));
  }

  @Test
  public void containerWithTMProfile_SignedWithExpiredCertificate_shouldBeInvalid() throws Exception {
    assertFalse(validateContainer("testFiles/invalid_bdoc_tm_old-sig-sigat-NOK-prodat-NOK.bdoc").isValid());
    assertFalse(validateContainer("testFiles/invalid_bdoc_tm_old-sig-sigat-OK-prodat-NOK.bdoc").isValid());
  }

  @Test
  public void containerWithTSProfile_SignedWithExpiredCertificate_shouldBeInvalid() throws Exception {
    ValidationResult result = validateContainer("testFiles/invalid_bdoc21-TS-old-cert.bdoc");
    assertFalse(result.isValid());
  }

  @Test
  public void bdocTM_signedWithValidCert_isExpiredByNow_shouldBeValid() throws Exception {
    String containerPath = "testFiles/valid_bdoc_tm_signed_with_valid_cert_expired_by_now.bdoc";
    Configuration configuration = new DefaultConfiguration(DefaultConfiguration.Mode.TEST);
    TSLHelper.addCertificateFromFileToTsl(configuration, "testFiles/certs/ESTEID-SK_2007_prod.pem.crt");
    Container container = ContainerBuilder.
        aContainer("BDOC").
        fromExistingFile(containerPath).
        withConfiguration(configuration).
        build();
    ValidationResult result = container.validate();
    assertTrue(result.isValid());
  }

  private ValidationResult validateContainer(String containerPath) {
    Container container = ContainerBuilder.
        aContainer("BDOC").
        fromExistingFile(containerPath).
        build();
    return container.validate();
  }




}
