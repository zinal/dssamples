package com.ibm.iosample;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Sample code demonstrating the ability to rename write-mapped files
 * under Windows operating system.
 * openChannel() is taken from Kafka's FileRecords.
 * atomicMoveWithFallback() is taken from Kafka's Utils.
 * @author zinal
 */
public class MmapSample {
    
    public static void main(String[] args) {
        try {
            File f1 = new File("zztop.orig");
            if (f1.exists()) {
                System.out.println("Deleting pre-existing file " + f1);
                f1.delete();
            }
            File f2 = new File("zztop.dest");
            if (f2.exists()) {
                System.out.println("Deleting pre-existing file " + f2);
                f2.delete();
            }
            // pre-allocation
            final int length = 500;
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(f1, "rw")) {
                randomAccessFile.setLength(length);
            }
            System.out.println("Pre-allocated file " + f1);
            // opening the channel
            MappedByteBuffer mbb = null;
            try (FileChannel fc = FileChannel.open(f1.toPath(), 
                    StandardOpenOption.CREATE, StandardOpenOption.READ,
                    StandardOpenOption.WRITE)) {
                mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0L, length);
            }
            // Initial data
            mbb.position(0);
            mbb.put("abcdefg".getBytes());
            mbb.force();
            System.out.println("Wrote some data");
            // renaming
            atomicMoveWithFallback(f1.toPath(), f2.toPath());
            System.out.println("Renamed to file " + f2);
            // New data
            mbb.position(8);
            mbb.put("xyzdqb0".getBytes());
            mbb.force();
            System.out.println("Wrote more data");
        } catch(Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Attempts to move source to target atomically and falls back to a non-atomic move if it fails.
     * This function allows callers to decide whether to flush the parent directory. This is needed
     * when a sequence of atomicMoveWithFallback is called for the same directory and we don't want
     * to repeatedly flush the same parent directory.
     *
     * @throws IOException if both atomic and non-atomic moves fail,
     * or parent dir flush fails if needFlushParentDir is true.
     */
    private static void atomicMoveWithFallback(Path source, Path target) throws IOException {
        System.out.println("moving " + source + " to " + target);
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException outer) {
            try {
                System.out.println("non-atomic mover! " + outer);
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException inner) {
                inner.addSuppressed(outer);
                throw inner;
            }
        }
    }

}
