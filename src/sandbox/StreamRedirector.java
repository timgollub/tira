package sandbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO Describe StreamRedirector
 *
 * @author johannes.kiesel@uni-weimar.de
 * @version $Id: StreamRedirector.java,v 1.1 2012/07/11 19:31:45 dogu3912 Exp $
 */
public final class StreamRedirector
extends Thread {

    private final InputStream source;

    private final OutputStream[] drains;

    private StreamRedirector(
        final InputStream sourceStream,
        final OutputStream... drainStreams)
    {
        this.source = sourceStream;
        this.drains = new OutputStream[drainStreams.length];
        System.arraycopy(drainStreams, 0, this.drains, 0, drainStreams.length);
    }

    @Override
    public void run() {
        try {
            final byte[] buffer = new byte[1024];
            int read = this.source.read(buffer);
            while (read != -1) {
                for (OutputStream drain : this.drains) {
                    drain.write(buffer, 0, read);
                    drain.flush();
                }
                read = this.source.read(buffer);
            }
            this.source.close();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            // TODO
        }
    }

    public static void redirect(
        final InputStream sourceStream,
        final OutputStream... drainStreams)
    {
        new StreamRedirector(sourceStream, drainStreams).start();
    }

}
