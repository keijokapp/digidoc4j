/* DigiDoc4J library
*
* This software is released under either the GNU Library General Public
* License (see LICENSE.LGPL).
*
* Note that the only valid version of the LGPL license as far as this
* project is concerned is the original GNU Library General Public License
* Version 2.1, February 1999
*/

package prototype;

import eu.europa.ec.markt.dss.signature.DSSDocument;
import eu.europa.ec.markt.dss.signature.FileDocument;
import eu.europa.ec.markt.dss.validation102853.CommonCertificateVerifier;
import eu.europa.ec.markt.dss.validation102853.SignedDocumentValidator;
import eu.europa.ec.markt.dss.validation102853.https.FileCacheDataLoader;
import eu.europa.ec.markt.dss.validation102853.ocsp.OnlineOCSPSource;
import eu.europa.ec.markt.dss.validation102853.report.Reports;
import eu.europa.ec.markt.dss.validation102853.tsl.TrustedListsCertificateSource;
import org.digidoc4j.*;
import org.digidoc4j.SignatureProfile;
import org.digidoc4j.signers.PKCS12SignatureToken;

import java.io.File;

import static java.util.Arrays.asList;

public class HowTo {
  public static void main(String[] args) {
    test();
  }

  private static void test() {
    Configuration configuration = new DefaultConfiguration(DefaultConfiguration.Mode.TEST);
    Container container = ContainerBuilder.
        aContainer().
        withConfiguration(configuration).
        withDataFile("testFiles/test.txt", "text/plain").
        build();

    Signature signature = SignatureBuilder.
        aSignature(container).
        withCity("Nömme").
        withRoles("manakeri").
        withSignatureProfile(SignatureProfile.LT_TM).
        withSignatureToken(new PKCS12SignatureToken("testFiles/signout.p12", "test".toCharArray())).
        invokeSigning();
    container.addSignature(signature);

    container.saveAsFile("prototype.bdoc");
    ValidationResult result = container.validate();
    System.out.println(result.getReport());
  }

  public static void validate() {
    DSSDocument toValidateDocument = new FileDocument("util/plugtest_asice/Signature-A-EE_CBTS-1.asice");
    SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(toValidateDocument);
    CommonCertificateVerifier verifier = new CommonCertificateVerifier();

    OnlineOCSPSource ocspSource = new OnlineOCSPSource();
    verifier.setOcspSource(ocspSource);

    final FileCacheDataLoader fileCacheDataLoader = new FileCacheDataLoader();
    File cacheFolder = new File("/tmp");

    fileCacheDataLoader.setFileCacheDirectory(cacheFolder);
    final TrustedListsCertificateSource certificateSource = new TrustedListsCertificateSource();
    certificateSource.setLotlUrl("https://ec.europa.eu/information_society/policy/esignature/trusted-list/tl-mp.xml");
    certificateSource.setCheckSignature(false);
    certificateSource.setDataLoader(fileCacheDataLoader);

    certificateSource.init();

    verifier.setTrustedCertSource(certificateSource);
    verifier.setDataLoader(fileCacheDataLoader);
    validator.setCertificateVerifier(verifier);
    final Reports reports = validator.validateDocument(new File("conf/test_constraint.xml"));
    reports.print();
  }
}
