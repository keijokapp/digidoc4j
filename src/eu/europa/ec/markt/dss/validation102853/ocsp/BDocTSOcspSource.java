package eu.europa.ec.markt.dss.validation102853.ocsp;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.digidoc4j.AbstractConfiguration;


public class BDocTSOcspSource extends SKOnlineOCSPSource{
  public BDocTSOcspSource(AbstractConfiguration configuration) {
    super(configuration);
  }

  @Override
  Extension createNonce() {
    final long currentTimeNonce = System.currentTimeMillis();

    DEROctetString nonce = new DEROctetString(String.valueOf(currentTimeNonce).getBytes());
    boolean critical = false;
    return new Extension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, critical, nonce);
  }
}
