package ch.ethz.bsse.bamextract;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.sf.samtools.AlignmentBlock;
import net.sf.samtools.CigarElement;
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
    private List<Header> headerList = new LinkedList<>();
    private List<Read> readList = new LinkedList<>();
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
                splitRegion();
                extractReads();


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

    private void extractReads() {
        for (Integer[] ii : regionList) {
            List<Read> reads = new LinkedList<>();
            for (Read r : readList) {
                Read readCut = r.cut(ii[0], ii[1]);
                if (readCut != null) {
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
        for (final SAMRecord samRecord : sfr) {
            List<AlignmentBlock> alignmentBlocks = samRecord.getAlignmentBlocks();
            if (alignmentBlocks.isEmpty()) {
                continue;
            }
            int refStart = alignmentBlocks.get(0).getReferenceStart() + alignmentBlocks.get(0).getReadStart() - 1;
            StringBuilder readSB = new StringBuilder();
            int readStart = 0;
            for (CigarElement c : samRecord.getCigar().getCigarElements()) {
                switch (c.getOperator()) {
                    case X:
                    case EQ:
                    case M:
                        for (int i = 0; i < c.getLength(); i++) {
                            readSB.append(samRecord.getReadString().charAt(readStart++));
                        }
                        break;
                    case I:
                        for (int i = 0; i < c.getLength(); i++) {
                            readStart++;
                        }
                        break;
                    case D:
                        for (int i = 0; i < c.getLength(); i++) {
                            readSB.append("-");
                        }
                        break;
                    case S:
                        for (int i = 0; i < c.getLength(); i++) {
                            readStart++;
                        }
                        break;
                    case H:
                        break;
                    case P:
                        System.out.println("P");
                        System.exit(9);
                        break;
                    case N:
                        System.out.println("N");
                        System.exit(9);
                        break;
                    default:
                        break;
                }
            }
            Read r = new Read();
            r.id = samRecord.getReadName();
            r.ref = samRecord.getReferenceName();
            r.setSequence(readSB.toString());
            r.start = refStart;
            r.quality = samRecord.getBaseQualityString();
            readList.add(r);
        }
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
