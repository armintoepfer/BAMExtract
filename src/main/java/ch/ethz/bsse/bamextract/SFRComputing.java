/**
 * Copyright (c) 2011-2013 Armin Töpfer
 *
 * This file is part of InDelFixer.
 *
 * InDelFixer is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 *
 * InDelFixer is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * InDelFixer. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.ethz.bsse.bamextract;

import java.util.List;
import java.util.concurrent.Callable;
import net.sf.samtools.AlignmentBlock;
import net.sf.samtools.CigarElement;
import net.sf.samtools.SAMRecord;


/**
 * @author Armin Töpfer (armin.toepfer [at] gmail.com)
 */
public class SFRComputing implements Callable<Read> {

    final SAMRecord samRecord;
    public SFRComputing(final SAMRecord samRecord) {
        this.samRecord = samRecord;
    }

    @Override
    public Read call() {
        List<AlignmentBlock> alignmentBlocks = samRecord.getAlignmentBlocks();
            if (alignmentBlocks.isEmpty()) {
                return null;
            }
            int refStart = alignmentBlocks.get(0).getReferenceStart() + alignmentBlocks.get(0).getReadStart() - 1;
            StringBuilder readSB = new StringBuilder();
            StringBuilder qualitySB = new StringBuilder();
            int readStart = 0;
            for (CigarElement c : samRecord.getCigar().getCigarElements()) {
                switch (c.getOperator()) {
                    case X:
                    case EQ:
                    case M:
                        for (int i = 0; i < c.getLength(); i++) {
                            qualitySB.append(samRecord.getBaseQualityString().charAt(readStart));
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
                            qualitySB.append("!");
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
            r.quality = qualitySB.toString();
        return r;
    }
}
