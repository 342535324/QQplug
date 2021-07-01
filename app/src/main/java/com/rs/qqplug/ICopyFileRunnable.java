package com.rs.qqplug;

import java.nio.channels.FileChannel;

public interface ICopyFileRunnable {
    public abstract void run(FileChannel srcChannel, FileChannel dstChannel) ;
}