/*
 * Copyright 2016 the original author or authors.
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
package com.eriwen.gradle;

import org.apache.commons.codec.digest.DigestUtils;
import org.gradle.api.Action;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.gradle.api.tasks.util.PatternSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Digest extends SourceTask {
    @Input
    private String algorithm = DigestAlgorithm.MD5.toString();

    @OutputDirectory
    private File dest;

    // Allow flexible user input
    public void setDest(final String input) {
        this.dest = getProject().file(input);
    }

    public void dest(final String input) {
        setDest(input);
    }

    public void dest(final File input) {
        this.dest = input;
    }

    private File checksumFile(final File destFile, final DigestAlgorithm algorithm) {
        return getProject().file(destFile.getAbsolutePath() + "." + algorithm.suffix());
    }

    private File digestedFile(final File file, final String digest) {
        return getProject().file(file.getParent() + File.separator + digest + "-" + file.getName());
    }

    private byte[] digest(final File file, final DigestAlgorithm algorithm) throws IOException {
        final byte[] contentBytes = Files.readAllBytes(file.toPath());
        final byte[] checksum;
        switch (algorithm) {
            case MD5: checksum = DigestUtils.md5Hex(contentBytes).getBytes(); break;
            case SHA1: checksum = DigestUtils.sha1(contentBytes); break;
            case SHA256: checksum = DigestUtils.sha256(contentBytes); break;
            case SHA512: checksum = DigestUtils.sha512(contentBytes); break;
            default: throw new IllegalArgumentException("Cannot use unknown digest algorithm " + algorithm.toString());
        }
        return checksum;
    }

    @TaskAction
    void processChanges() {
        getSource().visit(new FileVisitor() {
            @Override
            public void visitDir(FileVisitDetails visitDetails) {
                visitDetails.getRelativePath().getFile(dest).mkdir();
            }

            @Override
            public void visitFile(FileVisitDetails visitDetails) {
                try {
                    byte[] checksum = digest(visitDetails.getFile(), DigestAlgorithm.valueOf(algorithm));
                    final File destFile = visitDetails.getRelativePath().getFile(dest);
                    visitDetails.copyTo(digestedFile(destFile, new String(checksum)));
                    Files.write(checksumFile(destFile, DigestAlgorithm.valueOf(algorithm)).toPath(), checksum);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void deleteOutputsFor(final File file) {
        file.delete();
        new File(file.getAbsolutePath() + "." + DigestAlgorithm.valueOf(algorithm).suffix()).delete();
    }

    void executeIncremental(IncrementalTaskInputs inputs) {
        if (!inputs.isIncremental()) {
            getProject().delete((Object) dest.listFiles());
        }

        final PatternSet patternSet = new PatternSet();

        inputs.outOfDate(new Action<InputFileDetails>() {
            @Override
            public void execute(final InputFileDetails inputFileDetails) {
                if (inputFileDetails.isModified() || inputFileDetails.isRemoved()) {
                    deleteOutputsFor(inputFileDetails.getFile());
                }
                if (inputFileDetails.isAdded() || inputFileDetails.isModified()) {
                    // TODO(ew): patternSet.include()
                    Paths.get(inputFileDetails.getFile().toURI());
                }
            }
        });

        inputs.removed(new Action<InputFileDetails>() {
            public void execute(InputFileDetails inputFileDetails) {
                deleteOutputsFor(inputFileDetails.getFile());
            }
        });

        // TODO(ew): processChanges(getSource().matching(patternSet));
    }
}
