/*
 * This file is part of VIUtils.
 *
 * Copyright © 2012-2014 Visual Illusions Entertainment
 *
 * VIUtils is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/lgpl.html.
 */
package net.visualillusionsent.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import static net.visualillusionsent.utils.Verify.notEmpty;
import static net.visualillusionsent.utils.Verify.notNull;

/**
 * Version Checker
 * <p/>
 * Used to check if software is the latest version<br>
 * There is an included versionchecker.php in the /resources/extras/ folder inside the jar.<br>
 *
 * @author Jason (darkdiplomat)
 * @version 1.2
 * @since 1.0.0
 * @deprecated Being replaced by {@link net.visualillusionsent.utils.ProgramChecker}
 */
@Deprecated
public final class VersionChecker {

    private static float classVersion = 1.2F;
    private final String programName, checkURL, user_agent, formatted_Post;
    private final boolean checkUnstable;
    private final ProgramStatus status;
    private String currver, error;
    private long lastCheck = 0L;
    private boolean isLatest = true, canCheck = true;
    private float version;
    private long build;

    /**
     * Creates a new {@code VersionChecker}<br>
     * In your access log you will see something like: Java/{version} ({OS.Version}; {JarName}/{Version}; VersionChecker/{Version}) VIUtils/{Version}<br>
     *
     * @param programName
     *         the name of the Program being version checked
     * @param version
     *         A {@link String} representation of the software version
     * @param build
     *         A {@link String} representation of the build number
     * @param status
     *         The {@link ProgramStatus} of the given program.
     * @param checkURL
     *         A {@link String} representation of the url to verify version though (ie: http://visualillusionsent.net/testing/versionchecker.php)
     *
     * @throws UtilityException
     *         if an argument is null or a string argument is empty
     */
    public VersionChecker(String programName, String version, String build, String checkURL, ProgramStatus status, boolean checkUnstable) {
        notNull(programName, "String programName");
        notNull(version, "String version");
        notNull(build, "String build");
        notNull(checkURL, "String checkURL");
        notNull(status, "ProgramStatus status");
        notEmpty(programName, "String programName");
        notEmpty(version, "String version");
        notEmpty(build, "String build");
        notEmpty(checkURL, "String checkURL");

        this.programName = programName;
        this.currver = version;
        this.checkURL = checkURL;
        this.user_agent = "Java/" + SystemUtils.JAVA_VERSION + " (" + SystemUtils.SYSTEM_OS + "; " + programName + "/" + version + "; VersionChecker/" + classVersion + ") VIUtils/" + VIUtils.VIUTILS_VERSION;
        this.formatted_Post = "program=".concat(programName);
        this.checkUnstable = checkUnstable;
        this.status = status;
        try {
            this.version = Float.parseFloat(version);
            this.build = Long.parseLong(build);
        }
        catch (NumberFormatException nfe) {
            canCheck = false;
        }
        if (status == ProgramStatus.UNKNOWN) {
            canCheck = false;
        }
    }

    /**
     * Checks if version is latest<br>
     * NOTE: Site queries are limited to once every 5 minutes.
     *
     * @return {@code true} if latest; {@code false} if not; {@code null} on error
     */
    public final Boolean isLatest() {
        long currentTime = System.currentTimeMillis();
        if ((lastCheck + 600000) > currentTime || !canCheck) { //If recently checked, reuse value rather than spam the site
            return isLatest;
        }
        String inputLine = getInput(currentTime);
        if (inputLine == null || inputLine.startsWith("ERROR") || inputLine.startsWith("Fatal")) {
            parseError(inputLine);
            return null; //ERROR
        }
        else {
            parseError("GOOD");
            parseInput(inputLine);
        }
        return isLatest;
    }

    /**
     * Parse the input from the PHP Script
     *
     * @param currentTime
     *
     * @return inputLine
     * PHP Script output line of versions/builds
     */
    private final String getInput(long currentTime) {
        BufferedReader in = null;
        OutputStreamWriter out = null;
        String inputLine = "";
        try {
            URL url = new URL(checkURL);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            HttpURLConnection.setFollowRedirects(true);
            huc.setConnectTimeout(2000);
            huc.setReadTimeout(2000);
            huc.setRequestMethod("POST");
            huc.setRequestProperty("User-Agent", user_agent);
            huc.setDoOutput(true);
            huc.setDoInput(true);
            huc.connect();
            out = new OutputStreamWriter(huc.getOutputStream());
            out.write(formatted_Post);
            out.flush();
            in = new BufferedReader(new InputStreamReader(huc.getInputStream()));
            String temp;
            while ((temp = in.readLine()) != null) {
                inputLine += temp;
            }
            lastCheck = currentTime;
        }
        catch (Exception ex) {
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            }
            catch (IOException ioe) {
            }
        }
        if (inputLine.isEmpty()) {
            inputLine = null;
        }
        return inputLine;
    }

    /**
     * Parses the input from the php script
     *
     * @param input
     *         the input line from the php script
     */
    private final void parseInput(String input) {
        String[] statuses = input.split(",");
        float sv = 0.0F; //Stable Version
        float rcv = 0.0F; //ReleaseCandidate Version
        float bv = 0.0F; // BETA version
        float av = 0.0F; //ALPHA version
        long sb = 0; //Stable Build
        long rcb = 0; //ReleaseCandidate Build
        long bb = 0; //BETA Build
        long ab = 0; //ALPHA Build
        for (String status : statuses) {
            if (status.startsWith("STABLE")) {
                String[] verbuild = status.split(":");
                try {
                    sv = Float.parseFloat(verbuild[1].split("=")[1]);
                    sb = Long.parseLong(verbuild[2].split("=")[1]);
                }
                catch (NumberFormatException nfe) {
                }
            }
            else if (status.startsWith("RELEASE_CANIDATE")) {
                String[] verbuild = status.split(":");
                try {
                    rcv = Float.parseFloat(verbuild[1].split("=")[1]);
                    rcb = Long.parseLong(verbuild[2].split("=")[1]);
                }
                catch (NumberFormatException nfe) {
                }
            }
            else if (status.startsWith("BETA")) {
                String[] verbuild = status.split(":");
                try {
                    bv = Float.parseFloat(verbuild[1].split("=")[1]);
                    bb = Long.parseLong(verbuild[2].split("=")[1]);
                }
                catch (NumberFormatException nfe) {
                }
            }
            else if (status.startsWith("ALPHA")) {
                String[] verbuild = status.split(":");
                try {
                    av = Float.parseFloat(verbuild[1].split("=")[1]);
                    ab = Long.parseLong(verbuild[2].split("=")[1]);
                }
                catch (NumberFormatException nfe) {
                }
            }
        }
        float currentVersion;
        long currentBuild;
        Object[] currentVB = compareVersionBuild(version, sv, build, sb, status, ProgramStatus.STABLE);
        if (checkUnstable) {
            currentVB = compareVersionBuild((Float) currentVB[0], av, (Long) currentVB[1], ab, (ProgramStatus) currentVB[2], ProgramStatus.ALPHA);
            currentVB = compareVersionBuild((Float) currentVB[0], bv, (Long) currentVB[1], bb, (ProgramStatus) currentVB[2], ProgramStatus.BETA);
            currentVB = compareVersionBuild((Float) currentVB[0], rcv, (Long) currentVB[1], rcb, (ProgramStatus) currentVB[2], ProgramStatus.RELEASE_CANDIDATE);
            currentVersion = (Float) currentVB[0];
            currentBuild = (Long) currentVB[1];
        }
        else {
            currentVersion = (Float) currentVB[0];
            currentBuild = (Long) currentVB[1];
        }
        isLatest = currentVersion == version && currentBuild == build && status == currentVB[2];
        if (!isLatest) {
            currver = currentVersion + "." + currentBuild + "" + (currentVB[2] != ProgramStatus.STABLE ? " " + currentVB[2] : "");
        }
        else {
            currver = version + "." + build + (status != ProgramStatus.STABLE ? " " + status.toString() : "");
        }
    }

    /**
     * Compares the Version and Build
     *
     * @param vA
     *         float value vA
     * @param vB
     *         float value vB
     *
     * @return the larger of the 2 values
     */
    private final Object[] compareVersionBuild(float vA, float vB, long bA, long bB, ProgramStatus sA, ProgramStatus sB) {
        if (vA == vB && bA < bB) {
            return new Object[]{ vB, bB, sB };
        }
        else if (vA < vB) {
            return new Object[]{ vB, bB, sB };
        }
        return new Object[]{ vA, bA, sA };
    }

    /**
     * Parses the error code from the php script
     *
     * @param input
     *         the input line from the php script
     */
    private final void parseError(String input) {
        if (input == null) {
            error = "External Script could not be reached!";
        }
        else if (input.equals("ERROR: 404")) {
            error = "Program not found.";
        }
        else if (input.equals("ERROR: 400")) {
            error = "External Script Error";
        }
        else if (input.equals("GOOD")) {
            error = "No Errors Present";
        }
        else {
            error = "Unknown error: Input- " + input;
        }
    }

    /**
     * Gets the current version
     *
     * @return current version
     */
    public final String getCurrentVersion() {
        return currver;
    }

    /**
     * Gets a pre-generated update available message
     *
     * @return update: An update is available for: 'ProgramName' - v'Version'<br>
     * latest: Current Version of: 'ProgramName' is installed
     */
    public final String getUpdateAvailableMessage() {
        if (!isLatest()) {
            return "An update is available for: '".concat(programName).concat("' - v").concat(currver);
        }
        else {
            return "Current Version of: '".concat(programName).concat("' is installed");
        }
    }

    /**
     * Gets the Error Message if one is present
     *
     * @return the error message
     */
    public final String getErrorMessage() {
        return error;
    }

    /**
     * Gets this class's version number
     *
     * @return the class version
     */
    public final static float getClassVersion() {
        return classVersion;
    }
}
