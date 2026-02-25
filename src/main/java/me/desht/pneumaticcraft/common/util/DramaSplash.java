package me.desht.pneumaticcraft.common.util;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DramaSplash {
    private static final int MIN_SIZE = 5;
    private static final int MAX_SIZE = 10;
    private static final long SLEEP_TIME = 5000;  // sleep time between getting another drama text

    private static DramaSplash INSTANCE;
    private URL splashSite;
    private final ConcurrentLinkedQueue<String> dramaFifo;
    private Thread grabberThread = null;

    private DramaSplash() {
        try {
            this.splashSite = new URL("http://mc-drama.herokuapp.com/raw");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            this.splashSite = null;
        }
        this.dramaFifo = new ConcurrentLinkedQueue<>();
        this.fetchMoreSplash();
    }

    public static DramaSplash getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DramaSplash();
        }
        return INSTANCE;
    }

    public String getSplash() {
        String res = this.dramaFifo.poll();
        if (this.dramaFifo.size() < MIN_SIZE) {
            this.fetchMoreSplash();
        }
        return res == null ? "" : res;
    }

    private void fetchMoreSplash() {
        if (this.grabberThread == null && this.splashSite != null) {
            this.grabberThread = new Thread(new SplashGrabber());
            this.grabberThread.start();
            PneumaticCraftRepressurized.logger.info("Started splash fetcher: thread " + this.grabberThread.getName());
        }
    }

    private class SplashGrabber implements Runnable {
        @Override
        public void run() {
            try {
                while (DramaSplash.this.dramaFifo.size() < MAX_SIZE) {
                    String s = IOUtils.toString(DramaSplash.this.splashSite, StandardCharsets.UTF_8);
                    DramaSplash.this.dramaFifo.offer(s);
                    if (DramaSplash.this.dramaFifo.size() >= MIN_SIZE) {
                        try {
                            Thread.sleep(SLEEP_TIME);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            DramaSplash.this.grabberThread = null;
            PneumaticCraftRepressurized.logger.info("Finished fetching splash: " + DramaSplash.this.dramaFifo.size() + " texts in queue");
        }
    }
}
