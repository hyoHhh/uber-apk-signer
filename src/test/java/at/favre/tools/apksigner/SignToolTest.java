package at.favre.tools.apksigner;

import at.favre.tools.apksigner.signing.AndroidApkSignerVerify;
import at.favre.tools.apksigner.ui.CLIParser;
import at.favre.tools.apksigner.ui.CLIParserTest;
import at.favre.tools.apksigner.ui.MultiKeystoreParser;
import at.favre.tools.apksigner.util.FileUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.*;

public class SignToolTest {
    private final static String ksAlias = "app";
    private final static String ksPass = "password";
    private final static String keyPass = "keypass";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File originalFolder, outFolder, testReleaseKs, testDebugKeystore;

    private List<File> unsingedApks;
    private List<File> singedApks;

    @Before
    public void setUp() throws Exception {
        originalFolder = temporaryFolder.newFolder("signer-test", "apks");
        outFolder = temporaryFolder.newFolder("signer-test", "out");
        testReleaseKs = new File(getClass().getClassLoader().getResource("test-release-key.jks").toURI().getPath());
        testDebugKeystore = new File(getClass().getClassLoader().getResource("test-debug.jks").toURI().getPath());

        File signedFolder = new File(getClass().getClassLoader().getResource("test-apks-signed").toURI().getPath());
        File unsignedFolder = new File(getClass().getClassLoader().getResource("test-apks-unsigned").toURI().getPath());

        singedApks = Arrays.asList(signedFolder.listFiles());
        unsingedApks = Arrays.asList(unsignedFolder.listFiles());

        assertFalse(singedApks.isEmpty());
        assertFalse(unsingedApks.isEmpty());
    }

    @Test
    public void testSignMultipleApks() throws Exception {
        List<File> uApks = copyToTestPath(originalFolder, unsingedApks);

        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.getAbsolutePath() + " -" + CLIParser.ARG_APK_OUT + " " + outFolder.getAbsolutePath() + " --" + CLIParser.ARG_SKIP_ZIPALIGN;
        testAndCheck(cmd, outFolder, uApks);
    }

    @Test
    public void testSignSingleApk() throws Exception {
        List<File> uApks = copyToTestPath(originalFolder, unsingedApks.subList(0, 1));
        System.out.println("found " + uApks.size() + " apks in out folder");
        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.listFiles()[0].getAbsolutePath() + " -" + CLIParser.ARG_APK_OUT + " " + outFolder.getAbsolutePath() + " --" + CLIParser.ARG_SKIP_ZIPALIGN;
        testAndCheck(cmd, outFolder, uApks);
    }

    @Test
    public void testSignMultipleApksCustomCert() throws Exception {
        List<File> uApks = copyToTestPath(originalFolder, unsingedApks);

        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.getAbsolutePath() + " -" + CLIParser.ARG_APK_OUT + " " + outFolder.getAbsolutePath() + " --" + CLIParser.ARG_SKIP_ZIPALIGN + " --ks " + testReleaseKs.getAbsolutePath() + " --ksPass " + ksPass + " --ksKeyPass " + keyPass + " --ksAlias " + ksAlias;
        testAndCheck(cmd, outFolder, uApks);
    }

    @Test
    public void testSignMultipleApksMultipleCustomCert() throws Exception {
        List<File> uApks = copyToTestPath(originalFolder, unsingedApks);

        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.getAbsolutePath() + " -" + CLIParser.ARG_APK_OUT
                + " " + outFolder.getAbsolutePath() + " --" + CLIParser.ARG_SKIP_ZIPALIGN
                + " --ks 1" + MultiKeystoreParser.sep + testReleaseKs.getAbsolutePath() + " 2" + MultiKeystoreParser.sep + testDebugKeystore.getAbsolutePath() + " --ksPass 1" + MultiKeystoreParser.sep + ksPass + " 2" + MultiKeystoreParser.sep + "android --ksKeyPass 1" + MultiKeystoreParser.sep + keyPass + " 2" + MultiKeystoreParser.sep + "android --ksAlias 1" + MultiKeystoreParser.sep + ksAlias + " 2" + MultiKeystoreParser.sep + "androiddebugkey";
        testAndCheck(cmd, outFolder, uApks);
    }

    @Test
    public void testSignMultipleApksCustomDebugCert() throws Exception {
        List<File> uApks = copyToTestPath(originalFolder, unsingedApks);

        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.getAbsolutePath() + " -" + CLIParser.ARG_APK_OUT +
                " " + outFolder.getAbsolutePath() + " --" + CLIParser.ARG_SKIP_ZIPALIGN + " --ksDebug " + testDebugKeystore.getAbsolutePath();
        testAndCheck(cmd, outFolder, uApks);
    }

    @Test
    public void testNoApksGiven() throws Exception {
        copyToTestPath(originalFolder, Collections.singletonList(testReleaseKs));

        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.getAbsolutePath() + " -" + CLIParser.ARG_APK_OUT + " " + outFolder.getAbsolutePath() + " --" + CLIParser.ARG_SKIP_ZIPALIGN;
        testAndCheck(cmd, outFolder, Collections.emptyList());
    }

    @Test
    public void testSignMultiApkWithZipalign() throws Exception {
        List<File> uApks = copyToTestPath(originalFolder, unsingedApks);

        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.getAbsolutePath() + " -" + CLIParser.ARG_APK_OUT + " " + outFolder.getAbsolutePath()+" --debug";
        testAndCheck(cmd, outFolder, uApks);
    }

    @Test
    public void testVerify() throws Exception {
        copyToTestPath(originalFolder, singedApks);
        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.getAbsolutePath() + " --" + CLIParser.ARG_VERIFY + " --" + CLIParser.ARG_SKIP_ZIPALIGN;
        System.out.println(cmd);
        SignTool.Result result = SignTool.mainExecute(CLIParserTest.asArgArray(cmd));

        assertNotNull(result);
        assertEquals(0, result.unsuccessful);
        assertEquals(singedApks.size(), result.success);
    }

    @Test
    public void testVerifySingleApk() throws Exception {
        copyToTestPath(originalFolder, singedApks.subList(0, 1));
        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.listFiles()[0].getAbsolutePath() + " --" + CLIParser.ARG_VERIFY + " --" + CLIParser.ARG_SKIP_ZIPALIGN;
        System.out.println(cmd);

        SignTool.Result result = SignTool.mainExecute(CLIParserTest.asArgArray(cmd));
        assertNotNull(result);
        assertEquals(0, result.unsuccessful);
        assertEquals(1, result.success);
    }

    @Test
    public void testVerifyShouldNotBe() throws Exception {
        copyToTestPath(originalFolder, unsingedApks);
        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.getAbsolutePath() + " --" + CLIParser.ARG_VERIFY + " --" + CLIParser.ARG_SKIP_ZIPALIGN;
        System.out.println(cmd);

        SignTool.Result result = SignTool.mainExecute(CLIParserTest.asArgArray(cmd));
        assertNotNull(result);
        assertEquals(unsingedApks.size(), result.unsuccessful);
        assertEquals(0, result.success);
    }

    @Test
    public void testSignMultipleApksOverwrite() throws Exception {
        List<File> uApks = copyToTestPath(originalFolder, unsingedApks);

        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.getAbsolutePath() + " --overwrite --" + CLIParser.ARG_SKIP_ZIPALIGN;
        testAndCheck(cmd, originalFolder, uApks);
    }

    private static void testAndCheck(String cmd, File outFolder, List<File> copyApks) throws Exception {
        System.out.println(cmd);
        SignTool.Result result = SignTool.mainExecute(CLIParserTest.asArgArray(cmd));
        assertNotNull(result);
        assertEquals(0, result.unsuccessful);
        assertEquals(copyApks.size(), result.success);
        assertSigned(outFolder, copyApks);
    }

    private static void assertSigned(File outFolder, List<File> uApks) throws Exception {
        assertNotNull(outFolder);
        File[] outFiles = outFolder.listFiles(pathname -> FileUtil.getFileExtension(pathname).toLowerCase().equals("apk"));
        System.out.println("Found " + outFiles.length + " apks in out dir " + outFolder);
        assertNotNull(outFiles);
        assertEquals("should be same count of apks in out folder", uApks.size(), outFiles.length);

        for (File outFile : outFiles) {
            AndroidApkSignerVerify.Result verifyResult = new AndroidApkSignerVerify().verify(outFile, null, null, false);
            assertTrue(verifyResult.verified);
            assertEquals(0, verifyResult.warnings.size());
            assertEquals(0, verifyResult.errors.size());
            assertFalse(verifyResult.certInfoList.isEmpty());
        }
    }


    private static List<File> copyToTestPath(File target, List<File> source) throws Exception {
        List<File> copiedFiles = new ArrayList<>();
        for (File file : source) {
            File dstFile = new File(target, file.getName());
            Files.copy(file.toPath(), dstFile.toPath());
            copiedFiles.add(dstFile);
        }
        return copiedFiles;
    }

}