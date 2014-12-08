/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.builder.internal.incremental;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Holds dependency information, including the main compiled file, secondary input files
 * (usually headers), and output files.
 */
public class DependencyData {

    @NonNull
    private String mMainFile;
    @NonNull
    private List<String> mSecondaryFiles = Lists.newArrayList();
    @NonNull
    private List<String> mOutputFiles = Lists.newArrayList();

    DependencyData() {
    }

    @NonNull
    public String getMainFile() {
        return mMainFile;
    }

    void setMainFile(String path) {
        mMainFile = path;
    }

    @NonNull
    public List<String> getSecondaryFiles() {
        return mSecondaryFiles;
    }

    void addSecondaryFile(String path) {
        mSecondaryFiles.add(path);
    }

    @NonNull
    public List<String> getOutputFiles() {
        return mOutputFiles;
    }

    void addOutputFile(String path) {
        mOutputFiles.add(path);
    }

    /**
     * Parses the given dependency file and returns the parsed data
     *
     * @param dependencyFile the dependency file
     */
    public static DependencyData parseDependencyFile(@NonNull File dependencyFile)
            throws IOException {
        // first check if the dependency file is here.
        if (!dependencyFile.isFile()) {
            return null;
        }

        // Read in our dependency file
        List<String> content = Files.readLines(dependencyFile, Charsets.UTF_8);
        return processDependencyData(content);
    }

    private static enum ParseMode {
        OUTPUT, MAIN, SECONDARY
    }

    @VisibleForTesting
    static DependencyData processDependencyData(@NonNull List<String> content) {
        // The format is technically:
        // output1 output2 [...]: dep1 dep2 [...]
        // However, the current tools generating those files guarantee that each file path
        // is on its own line, making it simpler to handle windows paths as well as path
        // with spaces in them.

        DependencyData data = new DependencyData();

        ParseMode parseMode = ParseMode.OUTPUT;

        for (String line : content) {
            line = line.trim();

            // check for separator at the beginning
            if (line.startsWith(":")) {
                parseMode = ParseMode.MAIN;
                line = line.substring(1).trim();
            }

            ParseMode nextMode = parseMode;

            // remove the \ at the end.
            if (line.endsWith("\\")) {
                line = line.substring(0, line.length() - 1).trim();
            }

            // detect : at the end indicating a parse mode change *after* we process this line.
            if (line.endsWith(":")) {
                nextMode = ParseMode.MAIN;
                line = line.substring(0, line.length() - 1).trim();
            }

            if (!line.isEmpty()) {
                switch (parseMode) {
                    case OUTPUT:
                        data.addOutputFile(line);
                        break;
                    case MAIN:
                        data.setMainFile(line);
                        nextMode = ParseMode.SECONDARY;
                        break;
                    case SECONDARY:
                        data.addSecondaryFile(line);
                        break;
                }
            }

            parseMode = nextMode;
        }

        return data;
    }

    @Override
    public String toString() {
        return "DependencyData{" +
                "mMainFile='" + mMainFile + '\'' +
                ", mSecondaryFiles=" + mSecondaryFiles +
                ", mOutputFiles=" + mOutputFiles +
                '}';
    }
}
