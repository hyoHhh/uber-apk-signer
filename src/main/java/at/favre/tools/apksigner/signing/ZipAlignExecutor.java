package at.favre.tools.apksigner.signing;

import at.favre.tools.apksigner.ui.Arg;
import at.favre.tools.apksigner.util.CmdUtil;
import at.favre.tools.apksigner.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

public class ZipAlignExecutor {
    private enum Location {CUSTOM, PATH, BUILT_IN}

    public static final String ZIPALIGN_NAME = "zipalign";

    public String[] zipAlignExecutable;
    private Location location;
    private File tmpFolder;

    public ZipAlignExecutor(Arg arg) {
        findLocation(arg);
    }

    private void findLocation(Arg arg) {
        try {
            if (arg.zipAlignPath != null && new File(arg.zipAlignPath).exists()) {
                File passedPath = new File(arg.zipAlignPath);
                if (passedPath.exists() && passedPath.isFile()) {
                    zipAlignExecutable = new String[]{new File(arg.zipAlignPath).getAbsolutePath()};
                    location = Location.CUSTOM;
                }
            } else {
                File pathFile = CmdUtil.checkAndGetFromPATHEnvVar(ZIPALIGN_NAME);

                if (pathFile != null) {
                    zipAlignExecutable = new String[]{pathFile.getAbsolutePath()};
                    location = Location.PATH;
                    return;
                }

                if (zipAlignExecutable == null) {
                    CmdUtil.OS osType = CmdUtil.getOsType();


                    String fileName;
                    if (osType == CmdUtil.OS.WIN) {
                        fileName = "win-zipalign-24_0_3.exe";
                    } else if (osType == CmdUtil.OS.MAC) {
                        fileName = "mac-zipalign-24_0_3";
                    } else {
                        fileName = "linux-zipalign-24_0_3";
                    }

                    tmpFolder = Files.createTempDirectory("uapksigner-tmp").toFile();
                    File tmpZipAlign = File.createTempFile(fileName, null , tmpFolder);
                    Files.copy(getClass().getClassLoader().getResourceAsStream(fileName), tmpZipAlign.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    if (osType != CmdUtil.OS.WIN) {
                        Set<PosixFilePermission> perms = new HashSet<>();
                        perms.add(PosixFilePermission.OWNER_EXECUTE);

                        Files.setPosixFilePermissions(tmpZipAlign.toPath(), perms);
                        File lib64File = new File(new File(tmpFolder,"lib64"),"libc++.so");
                        lib64File.mkdirs();
                        Files.setPosixFilePermissions(lib64File.toPath(), perms);
                        Files.copy(getClass().getClassLoader().getResourceAsStream("linux64-libc++.so"), lib64File.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    zipAlignExecutable = new String[]{tmpZipAlign.getAbsolutePath()};
                    location = Location.BUILT_IN;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not find location for zipalign. Try to set it in PATH or use the --zipAlignPath argument. Optionally you could skip zipalign with --skipZipAlign. " + e.getMessage(), e);
        }
    }

    public boolean isExecutableFound() {
        return zipAlignExecutable != null;
    }

    public void cleanUp() {
        if (tmpFolder != null) {
            FileUtil.removeRecursive(tmpFolder.toPath());
            tmpFolder = null;
        }
    }

    @Override
    public String toString() {
        return "Using zipalign location " + location + ".";
    }
}