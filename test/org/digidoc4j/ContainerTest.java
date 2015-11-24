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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.digidoc4j.exceptions.DigiDoc4JException;
import org.digidoc4j.impl.bdoc.BDocContainer;
import org.digidoc4j.impl.ddoc.DDocContainer;
import org.digidoc4j.impl.DigiDoc4JTestHelper;
import org.digidoc4j.signers.PKCS12SignatureToken;
import org.digidoc4j.utils.Helper;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.digidoc4j.DefaultConfiguration.Mode.TEST;
import static org.digidoc4j.Container.DocumentType.DDOC;
import static org.digidoc4j.ContainerBuilder.BDOC_CONTAINER_TYPE;
import static org.digidoc4j.ContainerBuilder.DDOC_CONTAINER_TYPE;
import static org.digidoc4j.SignatureProfile.B_BES;
import static org.digidoc4j.SignatureProfile.LT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class ContainerTest extends DigiDoc4JTestHelper {
  public static final String TEXT_MIME_TYPE = "text/plain";

  public static final String CERTIFICATE =
      "MIIFEzCCA/ugAwIBAgIQSXxaK/qTYahTT77Z9I56EjANBgkqhkiG9w0BAQUFADBsMQswCQYDVQQGEwJFRTEiMCAGA1UECgwZQVMgU2VydGlmaX" +
          "RzZWVyaW1pc2tlc2t1czEfMB0GA1UEAwwWVEVTVCBvZiBFU1RFSUQtU0sgMjAxMTEYMBYGCSqGSIb3DQEJARYJcGtpQHNrLmVlMB4XDTE0" +
          "MDQxNzExNDUyOVoXDTE2MDQxMjIwNTk1OVowgbQxCzAJBgNVBAYTAkVFMQ8wDQYDVQQKDAZFU1RFSUQxGjAYBgNVBAsMEWRpZ2l0YWwgc2" +
          "lnbmF0dXJlMTEwLwYDVQQDDCjFvcOVUklOw5xXxaBLWSxNw4RSw5wtTMOWw5ZaLDExNDA0MTc2ODY1MRcwFQYDVQQEDA7FvcOVUklOw5xX" +
          "xaBLWTEWMBQGA1UEKgwNTcOEUsOcLUzDlsOWWjEUMBIGA1UEBRMLMTE0MDQxNzY4NjUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAo" +
          "IBAQChn9qVaA+x3RkDBrD5ujwfnreK5/Nb+Nvo9Vg5OLMn3JKUoUhFX6A/q5lBUylK/CU/lNRTv/kicqnu1aCyAiW0XVYk8jrOI1wRbHey" +
          "BMq/5gVm/vbbRtMi/XGLkgMZ5UDxY0QZfmu8wlRJ8164zRNocuUJLLXWOB6vda2RRXC3Cix4TDvQwGmPrQQJ8dzDIJEkLS7NCLBTcndm7b" +
          "uQegRc043gKMjUmRhGZEzF4oJa4pMfXqeSa+PUtrNyNNNQaOwTH29R8aFfGU2xorVvxoUieNipyWMEz8BTUGwwIceapWi77loBV/VQfStX" +
          "nQNu/s6BC04ss43O6sK70MB1qlRZAgMBAAGjggFmMIIBYjAJBgNVHRMEAjAAMA4GA1UdDwEB/wQEAwIGQDCBmQYDVR0gBIGRMIGOMIGLBg" +
          "orBgEEAc4fAwEBMH0wWAYIKwYBBQUHAgIwTB5KAEEAaQBuAHUAbAB0ACAAdABlAHMAdABpAG0AaQBzAGUAawBzAC4AIABPAG4AbAB5ACAA" +
          "ZgBvAHIAIAB0AGUAcwB0AGkAbgBnAC4wIQYIKwYBBQUHAgEWFWh0dHA6Ly93d3cuc2suZWUvY3BzLzAdBgNVHQ4EFgQUEjVsOkaNOGG0Gl" +
          "cF4icqxL0u4YcwIgYIKwYBBQUHAQMEFjAUMAgGBgQAjkYBATAIBgYEAI5GAQQwHwYDVR0jBBgwFoAUQbb+xbGxtFMTjPr6YtA0bW0iNAow" +
          "RQYDVR0fBD4wPDA6oDigNoY0aHR0cDovL3d3dy5zay5lZS9yZXBvc2l0b3J5L2NybHMvdGVzdF9lc3RlaWQyMDExLmNybDANBgkqhkiG9w" +
          "0BAQUFAAOCAQEAYTJLbScA3+Xh/s29Qoc0cLjXW3SVkFP/U71/CCIBQ0ygmCAXiQIp/7X7JonY4aDz5uTmq742zZgq5FA3c3b4NtRzoiJX" +
          "FUWQWZOPE6Ep4Y07Lpbn04sypRKbVEN9TZwDy3elVq84BcX/7oQYliTgj5EaUvpe7MIvkK4DWwrk2ffx9GRW+qQzzjn+OLhFJbT/QWi81Q" +
          "2CrX34GmYGrDTC/thqr5WoPELKRg6a0v3mvOCVtfIxJx7NKK4B6PGhuTl83hGzTc+Wwbaxwjqzl/SUwCNd2R8GV8EkhYH8Kay3Ac7Qx3ag" +
          "rJJ6H8j+h+nCKLjIdYImvnznKyR0N2CRc/zQ+g==";

  //TODO maybe it is good idea to put it to separate file
  private static String signature =
      "  <Signature Id=\"S1\" xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
          "    <SignedInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
          "      <CanonicalizationMethod Algorithm=\"http://www.w3" +
          ".org/TR/2001/REC-xml-c14n-20010315\"></CanonicalizationMethod>\n" +
          "      <SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></SignatureMethod>\n" +
          "      <Reference URI=\"#D0\">\n" +
          "        <DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></DigestMethod>\n" +
          "        <DigestValue>PKkcL8LlT9S1BO+HdXjb2djNzrM=</DigestValue>\n" +
          "      </Reference>\n" +
          "      <Reference Type=\"http://uri.etsi.org/01903/v1.1.1#SignedProperties\" " +
          "URI=\"#S1-SignedProperties\">\n" +
          "        <DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></DigestMethod>\n" +
          "        <DigestValue>UOK/V3cCbuo/kRTtgTWuYLm2wJA=</DigestValue>\n" +
          "      </Reference>\n" +
          "    </SignedInfo>\n" +
          "    <SignatureValue Id=\"S1-SIG\">\n" +
          "      RQ13qS4+2eqas4MS9rz4dKXqIEKMoXBWHGU0/TOtBk2sRST+ItOChpRn" +
          "+xRIFg13vSzjKIZHhnTYHuNRSmMLPFWD1UiXU68sqnBDzpI5f2db1FXC7OniGthZDwHWuGg1gBsqtW7tOfSpSQREYzP86amzY" +
          "/lf1CLECPqC+Up886dyNjWccSQf1CaYtSneEUNpJP2XQhlMf22bOvL8wq76dvudl3jx2HJErqyz0TObQXYLsQQGSYGN7JhdDQvYwTEYNIz" +
          "/NCu1K/Wn+HuIUZCZnh1KqQt7KZSX186MilkCXSBEM+PHEd/7qOyJYTnwJFYp6sTDO4ntAUq3ZDMHM4qE2g==\n" +
          "    </SignatureValue>\n" +
          "    <KeyInfo>\n" +
          "      <KeyValue>\n" +
          "        <RSAKeyValue>\n" +
          "          <Modulus>AKGf2pVoD7HdGQMGsPm6PB+et4rn81v42+j1WDk4syfckpShSEVfoD+rmUFTKUr8\n" +
          "            JT+U1FO/+SJyqe7VoLICJbRdViTyOs4jXBFsd7IEyr/mBWb+9ttG0yL9cYuSAxnl\n" +
          "            QPFjRBl+a7zCVEnzXrjNE2hy5QkstdY4Hq91rZFFcLcKLHhMO9DAaY+tBAnx3MMg\n" +
          "            kSQtLs0IsFNyd2btu5B6BFzTjeAoyNSZGEZkTMXiglrikx9ep5Jr49S2s3I001Bo\n" +
          "            7BMfb1HxoV8ZTbGitW/GhSJ42KnJYwTPwFNQbDAhx5qlaLvuWgFX9VB9K1edA27+\n" +
          "            zoELTiyzjc7qwrvQwHWqVFk=\n" +
          "          </Modulus>\n" +
          "          <Exponent>AQAB</Exponent>\n" +
          "        </RSAKeyValue>\n" +
          "      </KeyValue>\n" +
          "      <X509Data>\n" +
          "        <X509Certificate>MIIFEzCCA/ugAwIBAgIQSXxaK/qTYahTT77Z9I56EjANBgkqhkiG9w0BAQUFADBs\n" +
          "          MQswCQYDVQQGEwJFRTEiMCAGA1UECgwZQVMgU2VydGlmaXRzZWVyaW1pc2tlc2t1\n" +
          "          czEfMB0GA1UEAwwWVEVTVCBvZiBFU1RFSUQtU0sgMjAxMTEYMBYGCSqGSIb3DQEJ\n" +
          "          ARYJcGtpQHNrLmVlMB4XDTE0MDQxNzExNDUyOVoXDTE2MDQxMjIwNTk1OVowgbQx\n" +
          "          CzAJBgNVBAYTAkVFMQ8wDQYDVQQKDAZFU1RFSUQxGjAYBgNVBAsMEWRpZ2l0YWwg\n" +
          "          c2lnbmF0dXJlMTEwLwYDVQQDDCjFvcOVUklOw5xXxaBLWSxNw4RSw5wtTMOWw5Za\n" +
          "          LDExNDA0MTc2ODY1MRcwFQYDVQQEDA7FvcOVUklOw5xXxaBLWTEWMBQGA1UEKgwN\n" +
          "          TcOEUsOcLUzDlsOWWjEUMBIGA1UEBRMLMTE0MDQxNzY4NjUwggEiMA0GCSqGSIb3\n" +
          "          DQEBAQUAA4IBDwAwggEKAoIBAQChn9qVaA+x3RkDBrD5ujwfnreK5/Nb+Nvo9Vg5\n" +
          "          OLMn3JKUoUhFX6A/q5lBUylK/CU/lNRTv/kicqnu1aCyAiW0XVYk8jrOI1wRbHey\n" +
          "          BMq/5gVm/vbbRtMi/XGLkgMZ5UDxY0QZfmu8wlRJ8164zRNocuUJLLXWOB6vda2R\n" +
          "          RXC3Cix4TDvQwGmPrQQJ8dzDIJEkLS7NCLBTcndm7buQegRc043gKMjUmRhGZEzF\n" +
          "          4oJa4pMfXqeSa+PUtrNyNNNQaOwTH29R8aFfGU2xorVvxoUieNipyWMEz8BTUGww\n" +
          "          IceapWi77loBV/VQfStXnQNu/s6BC04ss43O6sK70MB1qlRZAgMBAAGjggFmMIIB\n" +
          "          YjAJBgNVHRMEAjAAMA4GA1UdDwEB/wQEAwIGQDCBmQYDVR0gBIGRMIGOMIGLBgor\n" +
          "          BgEEAc4fAwEBMH0wWAYIKwYBBQUHAgIwTB5KAEEAaQBuAHUAbAB0ACAAdABlAHMA\n" +
          "          dABpAG0AaQBzAGUAawBzAC4AIABPAG4AbAB5ACAAZgBvAHIAIAB0AGUAcwB0AGkA\n" +
          "          bgBnAC4wIQYIKwYBBQUHAgEWFWh0dHA6Ly93d3cuc2suZWUvY3BzLzAdBgNVHQ4E\n" +
          "          FgQUEjVsOkaNOGG0GlcF4icqxL0u4YcwIgYIKwYBBQUHAQMEFjAUMAgGBgQAjkYB\n" +
          "          ATAIBgYEAI5GAQQwHwYDVR0jBBgwFoAUQbb+xbGxtFMTjPr6YtA0bW0iNAowRQYD\n" +
          "          VR0fBD4wPDA6oDigNoY0aHR0cDovL3d3dy5zay5lZS9yZXBvc2l0b3J5L2NybHMv\n" +
          "          dGVzdF9lc3RlaWQyMDExLmNybDANBgkqhkiG9w0BAQUFAAOCAQEAYTJLbScA3+Xh\n" +
          "          /s29Qoc0cLjXW3SVkFP/U71/CCIBQ0ygmCAXiQIp/7X7JonY4aDz5uTmq742zZgq\n" +
          "          5FA3c3b4NtRzoiJXFUWQWZOPE6Ep4Y07Lpbn04sypRKbVEN9TZwDy3elVq84BcX/\n" +
          "          7oQYliTgj5EaUvpe7MIvkK4DWwrk2ffx9GRW+qQzzjn+OLhFJbT/QWi81Q2CrX34\n" +
          "          GmYGrDTC/thqr5WoPELKRg6a0v3mvOCVtfIxJx7NKK4B6PGhuTl83hGzTc+Wwbax\n" +
          "          wjqzl/SUwCNd2R8GV8EkhYH8Kay3Ac7Qx3agrJJ6H8j+h+nCKLjIdYImvnznKyR0\n" +
          "          N2CRc/zQ+g==\n" +
          "        </X509Certificate>\n" +
          "      </X509Data>\n" +
          "    </KeyInfo>\n" +
          "    <Object>\n" +
          "      <QualifyingProperties Target=\"#S1\" xmlns=\"http://uri.etsi.org/01903/v1.1.1#\">\n" +
          "        <SignedProperties Id=\"S1-SignedProperties\">\n" +
          "          <SignedSignatureProperties>\n" +
          "            <SigningTime>2014-06-13T09:50:06Z</SigningTime>\n" +
          "            <SigningCertificate>\n" +
          "              <Cert>\n" +
          "                <CertDigest>\n" +
          "                  <DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></DigestMethod>\n" +
          "                  <DigestValue>N9OdruanX8xd0jmQiqaTjnIb7Mk=</DigestValue>\n" +
          "                </CertDigest>\n" +
          "                <IssuerSerial>\n" +
          "                  <X509IssuerName\n" +
          "                    xmlns=\"http://www.w3.org/2000/09/xmldsig#\">1.2.840.113549.1.9" +
          ".1=#1609706b6940736b2e6565,CN=TEST of ESTEID-SK 2011,O=AS Sertifitseerimiskeskus,C=EE\n" +
          "                  </X509IssuerName>\n" +
          "                  <X509SerialNumber xmlns=\"http://www.w3" +
          ".org/2000/09/xmldsig#\">97679317403981919837045055800589842962</X509SerialNumber>\n" +
          "                </IssuerSerial>\n" +
          "              </Cert>\n" +
          "            </SigningCertificate>\n" +
          "            <SignaturePolicyIdentifier>\n" +
          "              <SignaturePolicyImplied></SignaturePolicyImplied>\n" +
          "            </SignaturePolicyIdentifier>\n" +
          "            <SignatureProductionPlace></SignatureProductionPlace>\n" +
          "          </SignedSignatureProperties>\n" +
          "          <SignedDataObjectProperties></SignedDataObjectProperties>\n" +
          "        </SignedProperties>\n" +
          "        <UnsignedProperties xmlns=\"http://uri.etsi.org/01903/v1.1.1#\">\n" +
          "          <UnsignedSignatureProperties>\n" +
          "            <CompleteCertificateRefs Id=\"S1-CERTREFS\">\n" +
          "              <CertRefs>\n" +
          "                <Cert>\n" +
          "                  <CertDigest>\n" +
          "                    <DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></DigestMethod>\n" +
          "                    <DigestValue>fQUNlFAUBPUsq/i/n/1zf1WVQqw=</DigestValue>\n" +
          "                  </CertDigest>\n" +
          "                  <IssuerSerial>\n" +
          "                    <X509IssuerName\n" +
          "                      xmlns=\"http://www.w3.org/2000/09/xmldsig#\">1.2.840.113549.1.9" +
          ".1=#1609706b6940736b2e6565,CN=TEST of EE Certification Centre Root CA,O=AS Sertifitseerimiskeskus,C=EE\n" +
          "                    </X509IssuerName>\n" +
          "                    <X509SerialNumber xmlns=\"http://www.w3" +
          ".org/2000/09/xmldsig#\">138983222239407220571566848351990841243</X509SerialNumber>\n" +
          "                  </IssuerSerial>\n" +
          "                </Cert>\n" +
          "              </CertRefs>\n" +
          "            </CompleteCertificateRefs>\n" +
          "            <CompleteRevocationRefs Id=\"S1-REVOCREFS\">\n" +
          "              <OCSPRefs>\n" +
          "                <OCSPRef>\n" +
          "                  <OCSPIdentifier URI=\"#N1\">\n" +
          "                    <ResponderID>C=EE,O=AS Sertifitseerimiskeskus,OU=OCSP," +
          "CN=TEST of SK OCSP RESPONDER 2011,E=pki@sk.ee</ResponderID>\n" +
          "                    <ProducedAt>2014-06-13T09:50:06Z</ProducedAt>\n" +
          "                  </OCSPIdentifier>\n" +
          "                  <DigestAlgAndValue>\n" +
          "                    <DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></DigestMethod>\n" +
          "                    <DigestValue>4LqK0iBlmpiwRVkNW0PXMID8zZE=</DigestValue>\n" +
          "                  </DigestAlgAndValue>\n" +
          "                </OCSPRef>\n" +
          "              </OCSPRefs>\n" +
          "            </CompleteRevocationRefs>\n" +
          "            <CertificateValues>\n" +
          "              <EncapsulatedX509Certificate Id=\"S1-RESPONDER_CERT\">\n" +
          CERTIFICATE +
          "              </EncapsulatedX509Certificate>\n" +
          "            </CertificateValues>\n" +
          "            <RevocationValues>\n" +
          "              <OCSPValues>\n" +
          "                <EncapsulatedOCSPValue Id=\"N1\">\n" +
          "                  MIICWwoBAKCCAlQwggJQBgkrBgEFBQcwAQEEggJBMIICPTCCASWhgYYwgYMxCzAJ\n" +
          "                  BgNVBAYTAkVFMSIwIAYDVQQKDBlBUyBTZXJ0aWZpdHNlZXJpbWlza2Vza3VzMQ0w\n" +
          "                  CwYDVQQLDARPQ1NQMScwJQYDVQQDDB5URVNUIG9mIFNLIE9DU1AgUkVTUE9OREVS\n" +
          "                  IDIwMTExGDAWBgkqhkiG9w0BCQEWCXBraUBzay5lZRgPMjAxNDA2MTMwOTUwMDZa\n" +
          "                  MGAwXjBJMAkGBSsOAwIaBQAEFJlSx0SY5H6TNo4LfCcJivmxW5RQBBRBtv7FsbG0\n" +
          "                  UxOM+vpi0DRtbSI0CgIQSXxaK/qTYahTT77Z9I56EoAAGA8yMDE0MDYxMzA5NTAw\n" +
          "                  NlqhJzAlMCMGCSsGAQUFBzABAgQWBBSgQ/a9YzY4QSbzrnXGUG4GxX7SOjANBgkq\n" +
          "                  hkiG9w0BAQUFAAOCAQEAnKwDAMXHB7lRNXRS9QBDdfwuNZkZPGUGImN/ZGXyVWNE\n" +
          "                  2xY+FRg4ADgxzJVHLHp6CdH50pLdeQHpvI5OS7g7P8XhXEkDmt5QwHyi+iDP72Cn\n" +
          "                  cPKTgWI7C23c417v8NcxaHAkiWpe/RwKpEQ5BNqC0sI25N5UuKVFN+Qm7KxTQ/Do\n" +
          "                  +oZC8rEBfuaR9L9DTEQA3rLbNPZIFPc3wpv6aBMBLq/O7p23TCiLXGiz/Yl0yuSx\n" +
          "                  zu/xL4NiBprD23Q3jiDAu9SsskPGrgwh36v+I7rYzSbj8bKEjKdf+dY3QO+9Egoe\n" +
          "                  TaMmSTFSra3zxjAo82vlegR/KFl/Dr/JJiPN6+xbhw==\n" +
          "                </EncapsulatedOCSPValue>\n" +
          "              </OCSPValues>\n" +
          "            </RevocationValues>\n" +
          "          </UnsignedSignatureProperties>\n" +
          "        </UnsignedProperties>\n" +
          "      </QualifyingProperties>\n" +
          "    </Object>\n" +
          "  </Signature>";

  private PKCS12SignatureToken PKCS12_SIGNER;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();
  File tempFile;


  @Before
  public void setUp() throws Exception {
    PKCS12_SIGNER = new PKCS12SignatureToken("testFiles/signout.p12", "test".toCharArray());
    tempFile = testFolder.newFile("tempFile.txt");
  }

  @AfterClass
  public static void deleteTemporaryFiles() {
    try {
      DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get("."));
      for (Path item : directoryStream) {
        String fileName = item.getFileName().toString();
        if ((fileName.endsWith("bdoc") || fileName.endsWith("ddoc")) && fileName.startsWith("test"))
          Files.deleteIfExists(item);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void createBDocContainersByDefault() {
    assertTrue(createContainer() instanceof BDocContainer);
  }

  @Test
  public void createBDocContainer() {
    assertTrue(createBDoc() instanceof BDocContainer);
  }

  private BDocContainer createBDoc() {
    Container container = ContainerBuilder.
        aContainer(BDOC_CONTAINER_TYPE).
        build();
    return (BDocContainer) container;
  }

  @Test
  public void createDDocContainer() {
    assertTrue(createDDoc() instanceof DDocContainer);
  }

  @Test
  public void openBDocContainerWhenTheFileIsAZipAndTheExtensionIsBDoc() {
    assertTrue(ContainerOpener.open("testFiles/zip_file_without_asics_extension.bdoc") instanceof BDocContainer);
  }

  @Test
  public void openDDocContainerForAllOtherFiles() {
    assertTrue(ContainerOpener.open("testFiles/changed_digidoc_test.ddoc") instanceof DDocContainer);
  }

  @Test
  public void testAddOneFileToContainerForBDoc() throws Exception {
    Container container = createContainer();
    container.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    List<DataFile> dataFiles = container.getDataFiles();
    assertEquals(1, dataFiles.size());
    assertEquals("test.txt", dataFiles.get(0).getName());
    assertEquals(TEXT_MIME_TYPE, dataFiles.get(0).getMediaType());
  }

  @Test
  public void testRemovesOneFileFromContainerWhenFileExistsForBDoc() throws Exception {
    Container bDocContainer = createContainer();
    bDocContainer.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    bDocContainer.removeDataFile("testFiles/test.txt");
    assertEquals(0, bDocContainer.getDataFiles().size());
  }

  @Test
  public void testCreateBDocContainerSpecifiedByDocumentTypeForBDoc() throws Exception {
    Container asicContainer = createBDoc();
    asicContainer.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    asicContainer.sign(PKCS12_SIGNER);
    asicContainer.save("test.bdoc");
    assertTrue(Helper.isZipFile(new File("test.bdoc")));
  }

  @Test
  public void testCreateDDocContainer() throws Exception {
    Container dDocContainer = createDDoc();
    dDocContainer.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    dDocContainer.sign(PKCS12_SIGNER);
    dDocContainer.save("testCreateDDocContainer.ddoc");

    assertTrue(Helper.isXMLFile(new File("testCreateDDocContainer.ddoc")));
  }

  @Test
  public void testAddOneFileToContainerForDDoc() throws Exception {
    Container container = createDDoc();
    container.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    List<DataFile> dataFiles = container.getDataFiles();
    assertEquals(1, dataFiles.size());
    assertEquals("test.txt", dataFiles.get(0).getName());
    assertEquals(TEXT_MIME_TYPE, dataFiles.get(0).getMediaType());
  }

  @Test
  public void testRemovesOneFileFromContainerWhenFileExistsForDDoc() throws Exception {
    Container container = createDDoc();
    container.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    container.save("testRemovesOneFileFromContainerWhenFileExistsFor.ddoc");

    Container container1 = ContainerOpener.open("testRemovesOneFileFromContainerWhenFileExistsFor.ddoc");
    container1.removeDataFile("testFiles/test.txt");
    assertEquals(0, container1.getDataFiles().size());
  }

  @Test
  public void testOpenCreatedDDocFile() throws Exception {
    Container container = createDDoc();
    container.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    container.save("testOpenCreatedDDocFile.ddoc");
    Container containerForReading = ContainerOpener.open("testOpenCreatedDDocFile.ddoc");
    assertEquals(DDOC, containerForReading.getDocumentType());

    assertEquals(1, container.getDataFiles().size());
  }

  @Test(expected = DigiDoc4JException.class)
  public void testOpenInvalidFileReturnsError() {
    ContainerOpener.open("testFiles/test.txt");
  }

  @Test
  public void testValidateDDoc() throws Exception {
    Container dDocContainer = ContainerOpener.open("testFiles/ddoc_for_testing.ddoc");
    assertFalse(dDocContainer.validate().hasErrors());
    assertFalse(dDocContainer.validate().hasWarnings());
  }

  @Test(expected = DigiDoc4JException.class)
  public void testOpenNotExistingFileThrowsException() {
    ContainerOpener.open("noFile.ddoc");
  }

  @Test(expected = DigiDoc4JException.class)
  public void testOpenEmptyFileThrowsException() {
    ContainerOpener.open("emptyFile.ddoc");
  }

  @Test(expected = DigiDoc4JException.class)
  public void testFileTooShortToVerifyIfItIsZipFileThrowsException() {
    ContainerOpener.open("testFiles/tooShortToVerifyIfIsZip.ddoc");
  }

  @Test(expected = DigiDoc4JException.class)
  public void testOpenFromStreamTooShortToVerifyIfIsZip() {
    try {
      FileInputStream stream = new FileInputStream(new File("testFiles/tooShortToVerifyIfIsZip.ddoc"));
      ContainerOpener.open(stream, true);
      IOUtils.closeQuietly(stream);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testAddFileFromStreamToDDoc() throws IOException {
    Container container = createDDoc();
    try (ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{0x42})) {
      container.addDataFile(is, "testFromStream.txt", TEXT_MIME_TYPE);
    }
    DataFile dataFile = container.getDataFiles().get(0);

    assertEquals("testFromStream.txt", dataFile.getName());
  }

  @Test
  public void openContainerFromStreamAsBDoc() throws IOException {
    Container container = createContainer();
    container.addDataFile("testFiles/test.txt", "text/plain");
    container.sign(PKCS12_SIGNER);
    container.save("testOpenContainerFromStreamAsBDoc.bdoc");

    FileInputStream stream = new FileInputStream("testOpenContainerFromStreamAsBDoc.bdoc");
    Container containerToTest = ContainerOpener.open(stream, false);
    stream.close();

    assertEquals(1, containerToTest.getSignatures().size());
  }

  @Test
  public void openContainerFromStreamAsDDoc() throws IOException {
    FileInputStream stream = new FileInputStream("testFiles/ddoc_for_testing.ddoc");
    Container container = ContainerOpener.open(stream, false);
    stream.close();

    assertEquals(1, container.getSignatures().size());
  }

  @Test
  public void testGetSignatureFromDDoc() {
    Container container = createDDoc();
    container.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    container.sign(PKCS12_SIGNER);
    List<Signature> signatures = container.getSignatures();

    assertEquals(1, signatures.size());
  }

  @Test(expected = DigiDoc4JException.class)
  public void testAddRawSignatureThrowsException() {
    Container container = createDDoc();
    container.addRawSignature(new byte[]{0x42});
  }

  @Test
  public void testAddRawSignatureAsByteArrayForDDoc() throws CertificateEncodingException, IOException, SAXException {
    Container container = createDDoc();
    container.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    container.sign(PKCS12_SIGNER);
    container.addRawSignature(signature.getBytes());

    assertEquals(2, container.getSignatures().size());
    assertEquals(CERTIFICATE.replaceAll("\\s", ""), encodeBase64String(getSigningCertificateAsBytes(container, 1)));
    assertXMLEqual(signature.trim(), new String(container.getSignatures().get(1).getAdESSignature()));
  }

  @Test
  public void throwsErrorWhenCreatesDDOCContainerWithConfiguration() throws Exception {
    Container container = ContainerBuilder.
        aContainer(DDOC_CONTAINER_TYPE).
        withConfiguration(new DefaultConfiguration()).
        build();

    assertEquals("DDOC", container.getType());
  }

  @Test
  public void testExtendToForBDOC() {
    Container container = createContainer();
    container.addDataFile("testFiles/test.txt", "text/plain");
    container.setSignatureProfile(B_BES);
    container.sign(PKCS12_SIGNER);

    container.extendTo(LT);

    assertNotNull(container.getSignature(0).getOCSPCertificate());
  }

  @Test
  public void testExtendToForDDOC() {
    Container container = createDDoc();
    container.addDataFile("testFiles/test.txt", "text/plain");
    container.setSignatureProfile(B_BES);
    container.sign(PKCS12_SIGNER);

    container.extendTo(SignatureProfile.LT_TM);

    assertNotNull(container.getSignature(0).getOCSPCertificate());
  }

  @Test
  @Ignore("possibility of this must be confirmed with dss authors")
  public void testAddRawSignatureAsByteArrayForBDoc() throws CertificateEncodingException, IOException, SAXException {
    Container container = createBDoc();
    container.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    container.sign(PKCS12_SIGNER);
    container.addRawSignature(Base64.decodeBase64("fo4aA1PVI//1agzBm2Vcxj7sk9pYQJt+9a7xLFSkfF10RocvGjVPBI65RMqyxGIsje" +
        "LoeDERfTcjHdNojoK/gEdKtme4z6kvkZzjMjDuJu7krK/3DHBtW3XZleIaWZSWySahUiPNNIuk5ykACUolh+K/UK2aWL3Nh64EWvC8aznLV0" +
        "M21s7GwTv7+iVXhR/6c3O22saWKWsteGT0/AqfcBRoj13H/NyuZOULqU0PFOhbJtV8RyZgC9n2uYBFsnutt5GPvhP+U93gkmFQ0+iC1a9Ktt" +
        "j4QH5si35YmRIe0fp8tGDo6li63/tybb+kQ96AIaRe1NxpkKVDBGNi+VNVNA=="));

    assertEquals(2, container.getSignatures().size());
  }

  @Test
  public void testAddRawSignatureAsStreamArray() throws CertificateEncodingException, IOException {
    Container container = createDDoc();
    container.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    try (ByteArrayInputStream signatureStream = new ByteArrayInputStream(signature.getBytes())) {
      container.addRawSignature(signatureStream);
    }

    assertEquals(1, container.getSignatures().size());
    assertEquals(CERTIFICATE.replaceAll("\\s", ""), encodeBase64String(getSigningCertificateAsBytes(container, 0)));
  }

  private byte[] getSigningCertificateAsBytes(Container container, int index) throws CertificateEncodingException {
    Signature signature = container.getSignatures().get(index);
    return signature.getSigningCertificate().getX509Certificate().getEncoded();
  }

  @Test
  public void testRemoveSignature() throws IOException {
    Container container = createDDoc();
    container.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    container.sign(PKCS12_SIGNER);
    try (ByteArrayInputStream signatureStream = new ByteArrayInputStream(signature.getBytes())) {
      container.addRawSignature(signatureStream);
    }
    container.save("testRemoveSignature.ddoc");

    Container containerToRemoveSignature = ContainerOpener.open("testRemoveSignature.ddoc");
    containerToRemoveSignature.removeSignature(1);

    assertEquals(1, containerToRemoveSignature.getSignatures().size());
    //todo check is correct signatureXML removed by signing time?
  }

  @Test(expected = DigiDoc4JException.class)
  public void testRemovingNotExistingSignatureThrowsException() {
    Container container = createDDoc();
    container.removeSignature(0);
  }


  @Test
  public void testSigningWithSignerInfo() throws Exception {
    String city = "myCity";
    String stateOrProvince = "myStateOrProvince";
    String postalCode = "myPostalCode";
    String country = "myCountry";
    String signerRoles = "myRole / myResolution";

    SignatureParameters signatureParameters = new SignatureParameters();
    signatureParameters.setProductionPlace(new SignatureProductionPlace(city, stateOrProvince, postalCode, country));
    signatureParameters.setRoles(asList(signerRoles));

    Container container = createContainer();
    container.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    container.setSignatureParameters(signatureParameters);
    Signature signature = container.sign(PKCS12_SIGNER);

    assertEquals("myCity", signature.getCity());
    assertEquals("myStateOrProvince", signature.getStateOrProvince());
    assertEquals("myPostalCode", signature.getPostalCode());
    assertEquals("myCountry", signature.getCountryName());
    assertEquals(1, signature.getSignerRoles().size());
    assertEquals("myRole / myResolution", signature.getSignerRoles().get(0));
  }

  @Test(expected = StringIndexOutOfBoundsException.class)
  public void testSetConfigurationForBDoc() throws Exception {
    DefaultConfiguration conf = new DefaultConfiguration(TEST);
    conf.setTslLocation("pole");
    Container container = ContainerBuilder.
        aContainer(BDOC_CONTAINER_TYPE).
        withConfiguration(conf).
        build();
    container.addDataFile("testFiles/test.txt", TEXT_MIME_TYPE);
    container.sign(PKCS12_SIGNER);
  }

  @Test
  public void mustBePossibleToCreateAndVerifyContainerWhereDigestAlgorithmIsSHA224() throws Exception {
    Container container = createContainer();
    SignatureParameters signatureParameters = new SignatureParameters();
    signatureParameters.setDigestAlgorithm(DigestAlgorithm.SHA224);
    container.setSignatureParameters(signatureParameters);
    container.addDataFile("testFiles/test.txt", "text/plain");
    container.sign(PKCS12_SIGNER);
    container.save("testMustBePossibleToCreateAndVerifyContainerWhereDigestAlgorithmIsSHA224.bdoc");

    container = ContainerOpener.open("testMustBePossibleToCreateAndVerifyContainerWhereDigestAlgorithmIsSHA224.bdoc");

    assertEquals("http://www.w3.org/2001/04/xmldsig-more#sha224", container.getSignature(0).getSignatureMethod());
  }

  @Test
  public void constructorWithConfigurationParameter() throws Exception {
    Container container = ContainerBuilder.
        aContainer().
        withConfiguration(new DefaultConfiguration()).
        build();
    assertEquals("BDOC", container.getType());
  }

  @Test
  @Ignore // Fails on Jenkins - need to verify
  public void createContainerWhenAttachmentNameContainsEstonianCharacters() throws Exception {
    Container container = createContainer();
    String s = "testFiles/test_o\u0303a\u0308o\u0308u\u0308.txt";
    container.addDataFile(s, "text/plain");
    container.sign(PKCS12_SIGNER);
    assertEquals(1, container.getDataFiles().size());
    ValidationResult validate = container.validate();
    assertTrue(validate.isValid());
  }

  @Test
  public void containerTypeStringValueForBDOC() throws Exception {
    assertEquals("application/vnd.etsi.asic-e+zip", createContainer().getDocumentType().toString());
  }

  @Test
  public void containerTypeStringValueForDDOC() throws Exception {
    assertEquals("DDOC", createDDoc().getDocumentType().toString());
  }

  @Test
  public void testSigningMultipleFilesInContainer() throws Exception {
    Container container = createContainer();
    container.setSignatureProfile(SignatureProfile.LT_TM);
    container.addDataFile(new ByteArrayInputStream(new byte[]{1, 2, 3}), "1.txt", "text/plain");
    container.addDataFile(new ByteArrayInputStream(new byte[]{1, 2, 3}), "2.txt", "text/plain");
    container.addDataFile(new ByteArrayInputStream(new byte[]{1, 2, 3}), "3.txt", "text/plain");
    container.sign(PKCS12_SIGNER);
    container.save(tempFile.getPath());
    assertEquals(3, container.getDataFiles().size());
    assertContainsDataFile("1.txt", container);
    assertContainsDataFile("2.txt", container);
    assertContainsDataFile("3.txt", container);
    Container openedContainer = ContainerOpener.open(tempFile.getPath());
    assertEquals(3, openedContainer.getDataFiles().size());
    assertContainsDataFile("1.txt", openedContainer);
    assertContainsDataFile("2.txt", openedContainer);
    assertContainsDataFile("3.txt", openedContainer);
  }

  private void assertContainsDataFile(String fileName, Container container) {
    for (DataFile file : container.getDataFiles()) {
      if (StringUtils.equals(fileName, file.getName())) {
        return;
      }
    }
    assertFalse("Data file '" + fileName + "' was not found in the container", true);
  }

  private Container createContainer() {
    return ContainerBuilder.aContainer().build();
  }

  private DDocContainer createDDoc() {
    Container container = ContainerBuilder.
        aContainer(DDOC_CONTAINER_TYPE).
        build();
    return (DDocContainer) container;
  }
}
