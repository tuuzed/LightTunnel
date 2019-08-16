package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Beta
 */
class DiskLogAdapter implements LogAdapter {
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

    private static final long KB = 1024;
    private static final long MB = 1024 * 1024;
    private static final long GB = 1024 * 1024 * 1024;

    private final ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();
    private int level = Logger.ALL;
    // 10k
    private long maxFileSize = KB * 10;
    private int maxFileIndex = 10;
    private String folder = "D:\\";
    private String filename = "log.txt";

    @Override
    public boolean isLoggable(int level) {
        return this.level <= level;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public void log(int level, @NotNull final String msg, @Nullable final Throwable cause) {
        singleThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try (
                    PrintStream out = new PrintStream(
                        new FileOutputStream(getLogFilePath(), true)
                    )
                ) {
                    out.write(msg.getBytes(StandardCharsets.UTF_8));
                    out.write(CRLF);
                    if (cause != null) {
                        cause.printStackTrace(out);
                    }
                    out.write(CRLF);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @NotNull
    private synchronized File getLogFilePath() {
        File dir = new File(folder);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        // 删除旧的文件
        final Pattern pattern = Pattern.compile(filename + "\\.\\d*");
        final File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).find();
            }
        });
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    // 时间早的靠前
                    return Long.compare(o2.lastModified(), o1.lastModified());
                }
            });
            for (int i = files.length; i >= maxFileIndex; i--) {
                //noinspection ResultOfMethodCallIgnored
                files[i - 1].delete();
            }
        }
        final File original = new File(dir, filename);
        int index = 0;
        File file = new File(original.getAbsolutePath() + "." + index);
        while (file.exists()) {
            file = new File(original.getAbsolutePath() + "." + index);
            if (!file.exists() || maxFileSize > file.length()) {
                break;
            }
            index++;
        }
        return file;
    }
}
