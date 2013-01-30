package ch.ethz.bsse.bamextract;

import com.google.common.collect.Lists;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Hello world!
 *
 */
public class Start {

    @Option(name = "-i")
    private String input;
    @Option(name = "-o")
    private String output;
    @Option(name = "-r")
    private String region;
    @Option(name = "-l")
    private int minlength = 0;
    private List<Header> headerList = new LinkedList<>();
    private List<Read> readList = null;
    private List<Integer[]> regionList = new LinkedList<>();
    private static final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(Runtime.getRuntime().availableProcessors() - 1);
    private static final RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
    private static ExecutorService executor = refreshExecutor();

    private static ExecutorService refreshExecutor() {
//        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
        return new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() - 1, Runtime.getRuntime().availableProcessors() - 1, 0L, TimeUnit.MILLISECONDS, blockingQueue, rejectedExecutionHandler);
    }

    public static void main(String[] args) throws IOException {
        new Start().doMain(args);
        System.exit(0);
    }

    /**
     * Main.
     *
     * @param args command-line parameters
     */
    public void doMain(String[] args) {
        try {
            CmdLineParser parser = new CmdLineParser(this);

            parser.setUsageWidth(80);
            try {
                parser.parseArgument(args);
                if (this.input == null || this.region == null) {
                    throw new CmdLineException("");
                }

                if (this.output == null) {
                    this.output = System.getProperty("user.dir") + File.separator;
                }
                if (!new File(this.output).exists()) {
                    new File(this.output).mkdirs();
                }
                parseBAMSAM();

                int start = Integer.MAX_VALUE;
                int end = Integer.MIN_VALUE;
                int counter = 0;
                for (Read r : this.readList) {
                    start = Math.min(start, r.start);
                    end = Math.max(end, r.start + r.getLength());

                    StatusUpdate.print("Index find:\t" + counter++);
                }

                StatusUpdate.println("Index find:\t" + counter++);
                System.out.println("START:\t" + start);
                System.out.println("END:\t" + end);

                splitRegion();
                extractReads(start, end, minlength);

            } catch (CmdLineException e) {
                System.err.println(e.getMessage());
                System.err.println("USAGE:");
                System.err.println("java -jar bamextract.jar options...\n");
                System.err.println(" ------------------------");
                System.err.println(" === GENERAL options ===");
                System.err.println("  -i PATH\t\t: Path to the input file (BAM format) [REQUIRED]");
                System.err.println("  -r STRING\t: Regions in format start-end,start2-end2");
                System.err.println(" ------------------------");
                System.err.println(" === EXAMPLES ===");
                System.err.println("  java -jar bamextract.jar -i reads.bam -r 320-600,1220-4000 ");
                System.err.println(" ------------------------");
            }
        } catch (OutOfMemoryError e) {
            System.err.println("Please increase the heap space.");
        }
    }

    private void extractReads(int alignmentStart, int alignmentStop, int minlength) {
        for (Integer[] ii : regionList) {
            List<Read> reads = new LinkedList<>();
            for (Read r : readList) {
                Read readCut = r.cut(ii[0], ii[1], alignmentStart, alignmentStop);
                if (readCut != null && readCut.getSequence().length() >= minlength) {
                    reads.add(readCut);
                }
            }
            saveSAM(reads, ii[0], ii[1]);
        }
    }

    private void splitRegion() {
        String[] csv = region.split(",");
        for (String s : csv) {
            String[] split = s.split("-");
            regionList.add(new Integer[]{Integer.parseInt(split[0]), Integer.parseInt(split[1])});
        }
    }

    public void parseBAMSAM() {
        File bam = new File(this.input);
        SAMFileReader sfr = new SAMFileReader(bam);
        for (SAMSequenceRecord ssr : sfr.getFileHeader().getSequenceDictionary().getSequences()) {
            Header h = new Header();
            h.name = ssr.getSequenceName();
            h.length = ssr.getSequenceLength();
            headerList.add(h);
        }
        int counter = 0;
//        this.readList = Lists.newArrayListWithCapacity(sfr.)
        List<Future<Read>> readFutures = Lists.newArrayListWithExpectedSize(200_000);
        for (final SAMRecord samRecord : sfr) {
            readFutures.add(executor.submit(new SFRComputing(samRecord)));
            StatusUpdate.print("Parsing:\t" + counter++);
        }
        StatusUpdate.println("Parsing:\t" + counter);
        this.readList = Lists.newArrayListWithCapacity(readFutures.size());
        counter = 0;
        for (Future<Read> future : readFutures) {
            try {
                Read r = future.get();
                if (r != null) {
                    this.readList.add(r);
                }
                StatusUpdate.print("Mapped:\t" + counter++);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        StatusUpdate.println("Mapped:\t" + counter++);
    }

    public void saveSAM(List<Read> reads, int from, int to) {
        StringBuilder samSB = new StringBuilder();
        samSB.append("@HD\tVN:1.4\tSO:unsorted\n");
        for (Header h : headerList) {
            samSB.append("@SQ\tSN:").append(h.name).append("\tLN:").append(h.length).append("\n");
        }
        samSB.append("@PG\tID:BAMExtract\tPN:BAMExtract\tVN:0.1\n");
        for (Read r : reads) {
            samSB.append(r.sam());
        }
        saveFile(this.output + "reads_" + from + "-" + to + ".sam", samSB.toString());
    }

    public static void saveFile(String path, String sb) {
        try {
            // Create file 
            FileWriter fstream = new FileWriter(path);
            try (BufferedWriter out = new BufferedWriter(fstream)) {
                out.write(sb);
            }
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error save file: ");
            System.err.println(path);
        }
    }
}
