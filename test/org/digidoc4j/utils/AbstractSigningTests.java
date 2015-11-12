package org.digidoc4j.utils;

import org.digidoc4j.Configuration;
import org.digidoc4j.DefaultConfiguration;

public abstract class AbstractSigningTests {

    protected Configuration createDigiDoc4JConfiguration() {
        DefaultConfiguration result = new ConfigurationWithIpBasedAccess();
        result.setOcspSource(Configuration.TEST_OCSP_URL);
        result.setTSL(new CertificatesForTests().getTslCertificateSource());
        return result;
    }

    private static class ConfigurationWithIpBasedAccess extends DefaultConfiguration {
        public ConfigurationWithIpBasedAccess() {
            super(Mode.PROD);

            getJDigiDocConfiguration().put("SIGN_OCSP_REQUESTS", Boolean.toString(false));
        }

        @Override
        public boolean hasToBeOCSPRequestSigned() {
            return false;
        }
    }
}
