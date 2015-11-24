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

import eu.europa.ec.markt.dss.signature.token.AbstractSignatureTokenConnection;
import eu.europa.ec.markt.dss.signature.token.DSSPrivateKeyEntry;
import eu.europa.ec.markt.dss.signature.token.Pkcs11SignatureToken;
import org.digidoc4j.Configuration;
import org.digidoc4j.DefaultConfiguration;
import org.digidoc4j.DigestAlgorithm;
import org.digidoc4j.SignatureToken;

import java.security.cert.X509Certificate;

/**
 * This signer implementation is for testing purposes
 */
@Deprecated
public class PKCS11Signer implements SignatureToken {
  protected AbstractSignatureTokenConnection signatureTokenConnection = null;
  protected DSSPrivateKeyEntry keyEntry = null;


  /**
   * Constructor
   *
   * @param password password
   */
  public PKCS11Signer(char[] password) {
    Configuration configuration = new DefaultConfiguration();
    signatureTokenConnection = new Pkcs11SignatureToken(configuration.getPKCS11ModulePath(), password, 2);
    keyEntry = signatureTokenConnection.getKeys().get(0);
  }

  @Override
  public X509Certificate getCertificate() {
    return keyEntry.getCertificate().getCertificate();
  }

  @Override
  public byte[] sign(DigestAlgorithm digestAlgorithm, byte[] dataToSign) {
    return new byte[0];
  }
}
