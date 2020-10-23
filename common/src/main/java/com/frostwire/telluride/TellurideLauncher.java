/*
 * Created by Angel Leon (@gubatron)
 * Copyright (c) 2007-2020, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.telluride;

import org.limewire.concurrent.ThreadExecutor;

import java.io.*;

/**
 * A Java process builder and parser for the telluride cloud downloader.
 * Launches the process passing the appropiate options, parses its output to dispatch events to a TellurideListener
 */
public final class TellurideLauncher {

    /**
     * @param executable
     * @param downloadUrl
     * @param saveDirectory
     * @param audioOnly
     * @param metaOnly        Get only metadata about the downloadUrl. If set to true, it ignores audioOnly
     * @param processListener
     */
    public static void launch(final File executable,
                              final String downloadUrl,
                              final File saveDirectory,
                              final boolean audioOnly,
                              final boolean metaOnly,
                              TellurideListener processListener) {
        if (executable == null) {
            throw new IllegalArgumentException("executable path is null, no telluride to launch");
        }
        if (!executable.isFile()) {
            throw new IllegalArgumentException(executable + " is not a file");
        }
        if (!executable.canExecute()) {
            throw new IllegalArgumentException(executable + " is not executable");
        }
        ThreadExecutor.startThread(launchRunnable(executable, downloadUrl, saveDirectory, audioOnly, metaOnly, processListener),
                "telluride-process-adapter:" + downloadUrl);
    }

    private static Runnable launchRunnable(final File executable,
                                           final String downloadUrl,
                                           final File saveDirectory,
                                           final boolean audioOnly,
                                           final boolean metaOnly,
                                           TellurideListener processListener) {
        return () -> {
            ProcessBuilder processBuilder = new ProcessBuilder(executable.getAbsolutePath(), downloadUrl);
            if (audioOnly) {
                processBuilder = new ProcessBuilder(
                        executable.getAbsolutePath(),
                        "-a",
                        downloadUrl);
            }
            if (metaOnly) {
                processBuilder = new ProcessBuilder(
                        executable.getAbsolutePath(),
                        "-m",
                        downloadUrl);
            }
            if (saveDirectory != null && saveDirectory.isDirectory() && saveDirectory.canWrite()) {
                processBuilder.directory(saveDirectory);
            }
            try {
                final Process process = processBuilder.start();
                final InputStream stdOut = process.getInputStream();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(stdOut));
                TellurideParser parser = null;
                if (processListener != null) {
                    parser = new TellurideParser(processListener, metaOnly);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[" + executable.getName() + "] " + line);
                    if (processListener != null && processListener.aborted()) {
                        process.destroyForcibly();
                        break;
                    }
                    if (parser != null) {
                        parser.parse(line);
                    }
                }
                process.waitFor();
                System.out.println("Exit-Code: " + process.exitValue());
                if (parser != null) {
                    parser.done();
                }
                if (processListener != null) {
                    processListener.onFinished(process.exitValue());
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        };
    }

    private final static class TellurideParser {
        final TellurideListener processListener;
        final boolean metaOnly;
        boolean pageUrlRead;
        final StringBuilder sb;

        TellurideParser(TellurideListener listener, boolean pMetaOnly) {
            processListener = listener;
            metaOnly = pMetaOnly;
            sb = new StringBuilder();
        }

        public void parse(String line) {
            if (!pageUrlRead && line.startsWith("Page_URL:")) {
                pageUrlRead = true;
                return;
            }
            if (!pageUrlRead) {
                return;
            }
            if (metaOnly) {
                sb.append(line);
            } else {
                // reports on the file name we're about to download - onDestination
                // [download]  30.5% of 277.93MiB at 507.50KiB/s ETA 06:29

                // reports on progress
                // reports on errors
            }
        }

        public void done() {
            if (metaOnly) {
                String JSON = sb.toString();
                if (JSON != null && JSON.length() > 0) {
                    processListener.onMeta(JSON);
                } else {
                    processListener.onError("No metadata returned by telluride");
                }
            }
        }
    }
}
