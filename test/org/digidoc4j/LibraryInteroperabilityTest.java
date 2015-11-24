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

import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.utils.ConfigManager;
import org.digidoc4j.impl.DigiDoc4JTestHelper;
import org.digidoc4j.signers.PKCS12SignatureToken;
import org.digidoc4j.testutils.TestDataBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.digidoc4j.ContainerBuilder.BDOC_CONTAINER_TYPE;
import static org.junit.Assert.assertTrue;

public class LibraryInteroperabilityTest extends DigiDoc4JTestHelper {

    private final static Logger logger = LoggerFactory.getLogger(LibraryInteroperabilityTest.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private File tempFile;
    private PKCS12SignatureToken signatureToken;

    @Before
    public void setUp() throws Exception {
        signatureToken = new PKCS12SignatureToken("testFiles/signout.p12", "test".toCharArray());
        tempFile = testFolder.newFile("test.bdoc");
    }

    @Test
    public void verifyWithJDigidoc() throws Exception {
        String containerFilePath = tempFile.getPath();
        createSignedContainerWithDigiDoc4j(containerFilePath);
        validateContainerWithJDigiDoc(containerFilePath);
    }

    @Test
    public void verifyLibdigidocTS_SignatureWithDigiDoc4j() {
        Container container = ContainerBuilder.
            aContainer(BDOC_CONTAINER_TYPE).
            fromExistingFile("testFiles/Libdigidoc_created_tsa_signature_TS.bdoc").
            withConfiguration(new DefaultConfiguration(DefaultConfiguration.Mode.PROD)).
            build();
        ValidationResult result = container.validate();
        assertTrue(result.isValid());
    }

    @Test
    public void verifyAddingSignatureToJDigiDocContainer() {
        Container container = ContainerBuilder.
            aContainer(BDOC_CONTAINER_TYPE).
            fromExistingFile("testFiles/DigiDocService_spec_est.pdf-TM-j.bdoc").
            withConfiguration(new DefaultConfiguration(DefaultConfiguration.Mode.PROD)).
            build();
        TestDataBuilder.signContainer(container);
    }

    private void createSignedContainerWithDigiDoc4j(String containerFilePath) {
        Configuration configuration = new DefaultConfiguration(DefaultConfiguration.Mode.TEST);
        Container container = ContainerBuilder.
            aContainer(BDOC_CONTAINER_TYPE).
            withConfiguration(configuration).
            withDataFile("testFiles/test.txt", "text/plain").
            build();
        signContainer(container, signatureToken);
        signContainer(container, signatureToken);
        logger.debug("Saving test file temporarily to " + containerFilePath);
        container.saveAsFile(containerFilePath);
    }

    private void signContainer(Container container, PKCS12SignatureToken signatureToken) {
        Signature signature = SignatureBuilder.
            aSignature(container).
            withSignatureProfile(SignatureProfile.LT_TM).
            withSignatureToken(signatureToken).
            invokeSigning();
        container.addSignature(signature);
    }

    private void validateContainerWithJDigiDoc(String containerFilePath) throws DigiDocException {
        String cfgFile = "testFiles/jdigidoc.cfg";
        ConfigManager.init(cfgFile);
        boolean isBdoc = true;
        List errors = new ArrayList();
        DigiDocFactory digFac = ConfigManager.instance().getDigiDocFactory();
        digFac.readSignedDocOfType(containerFilePath, isBdoc, errors);
        assertTrue(getJDigiDocErrorMessage(errors), errors.isEmpty());
    }

    private String getJDigiDocErrorMessage(List errors) {
        String msg = "";
        for(Object error : errors) {
            msg += error.toString() + "; ";
        }
        return msg;
    }
}
