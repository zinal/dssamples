package com.ibm.iosample;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Sample code demonstrating the ability to rename write-open files
 * under Windows operating system.
 * openChannel() is taken from Kafka's FileRecords.
 * atomicMoveWithFallback() is taken from Kafka's Utils.
 * @author zinal
 */
public class IoSample implements Runnable {
    
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private FileChannel fc = null;
    private long lastTv = 0L, curTv = 0L;
    private File f = null;
    
    public static void main(String[] args) {
        try {
            new IoSample() . run();
        } catch(Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                step();
                try { Thread.sleep(100L); } catch(InterruptedException x) {}
            }
        } catch(Exception ex) {
            throw new RuntimeException("FAILURE!", ex);
        }
    }
    
    private void step() throws Exception {
        if (fc==null) {
            f = new File("zztop.orig");
            if (f.exists()) {
                System.out.println("Deleting pre-existing file " + f);
                f.delete();
            }
            fc = openChannel(f, true, false, 1024, true);
        }
        fc.position(0L).write(ByteBuffer.wrap(dateData()));
        renameSometimes();
    }
    
    private byte[] dateData() {
        final long l = System.currentTimeMillis();
        final Date d = new Date(l);
        curTv = l;
        return (Long.toHexString(l) + " *** " + sdf.format(d)).
                getBytes(StandardCharsets.UTF_8);
    }
    
    private void renameSometimes() throws Exception {
        if (lastTv == 0L || lastTv > curTv) {
            lastTv = curTv;
            return;
        }
        if ( (curTv - lastTv) >= 5000L) {
            File f2 = new File("zztop." + Long.toHexString(curTv));
            atomicMoveWithFallback(f.toPath(), f2.toPath());
            f = f2;
            lastTv = curTv;
        }
    }

    /**
     * Open a channel for the given file
     * For windows NTFS and some old LINUX file system, set preallocate to true and initFileSize
     * with one value (for example 512 * 1025 *1024 ) can improve the kafka produce performance.
     * @param file File path
     * @param mutable mutable
     * @param fileAlreadyExists File already exists or not
     * @param initFileSize The size used for pre allocate file, for example 512 * 1025 *1024
     * @param preallocate Pre-allocate file or not, gotten from configuration.
     */
    private static FileChannel openChannel(File file,
                                           boolean mutable,
                                           boolean fileAlreadyExists,
                                           int initFileSize,
                                           boolean preallocate) throws IOException {
        if (mutable) {
            if (preallocate && !fileAlreadyExists) {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
                    randomAccessFile.setLength(initFileSize);
                }
            }
            return FileChannel.open(file.toPath(), StandardOpenOption.CREATE, 
                    StandardOpenOption.READ, StandardOpenOption.WRITE);
        } else {
            return FileChannel.open(file.toPath());
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
