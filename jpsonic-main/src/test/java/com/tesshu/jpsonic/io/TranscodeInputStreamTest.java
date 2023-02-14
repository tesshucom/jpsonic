/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tesshu.jpsonic.io.TranscodeInputStream.DeleteTmpFileTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TranscodeInputStreamTest {

    @Nested
    class DeleteTmpFileTaskTest {

        private Path tempDir;
        private Path tempFile;

        @BeforeEach
        public void setup(@TempDir Path tempDir) throws ExecutionException, IOException, URISyntaxException {
            this.tempDir = tempDir;
            tempFile = Path.of(tempDir.toString(), "jpsonic.log");
            if (!Files.exists(tempFile)) {
                tempFile = Files.createFile(tempFile);
                Path dummySource = Path.of(TranscodeInputStreamTest.class.getResource("/banner.txt").toURI());
                Files.copy(dummySource, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        @SuppressWarnings({ "PMD.DoNotUseThreads", "PMD.CloseResource" })
        // [CloseResource] False positives occur only on JDK19
        @Test
        void testRun() throws IOException, InterruptedException, ExecutionException {
            assertEquals(1, Files.list(tempDir).count());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            DeleteTmpFileTask task = new DeleteTmpFileTask(Path.of(tempFile.toString() + ".dummy"));
            executor.submit(task).get();
            assertEquals(1, Files.list(tempDir).count());

            executor.submit(new DeleteTmpFileTask(tempFile)).get();
            assertEquals(0, Files.list(tempDir).count());
            executor.shutdown();
        }
    }
}
