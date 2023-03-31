package net.creeperhost.blockshot.lib;

import com.google.common.util.concurrent.AtomicDouble;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.Args;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by brandon3055 on 31/03/2023
 */
public class TrackableByteArrayEntity extends ByteArrayEntity {
    private final byte[] b;
    private final int off;
    private final int len;
    private final AtomicDouble progress;

    public TrackableByteArrayEntity(byte[] b, ContentType contentType, @Nullable AtomicDouble progress) {
        super(b, contentType);
        this.b = b;
        this.off = 0;
        this.len = this.b.length;
        this.progress = progress;
    }

    public TrackableByteArrayEntity(byte[] b, int off, int len, ContentType contentType, @Nullable AtomicDouble progress) {
        super(b, off, len, contentType);
        this.b = b;
        this.off = off;
        this.len = len;
        this.progress = progress;
    }

    public TrackableByteArrayEntity(byte[] b, @Nullable AtomicDouble progress) {
        this(b, null, progress);
    }

    public TrackableByteArrayEntity(byte[] b, int off, int len, @Nullable AtomicDouble progress) {
        this(b, off, len, null, progress);
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {
        Args.notNull(outStream, "Output stream");
        for (int i = 0 ; i < len ; i++) {
            if (progress != null) {
                progress.set(i / (double)len);
            }
            outStream.write(b[off + i]);
        }
//        outStream.write(this.b, this.off, this.len);
        outStream.flush();
    }
}
