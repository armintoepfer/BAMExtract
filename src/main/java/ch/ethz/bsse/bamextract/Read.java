/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.ethz.bsse.bamextract;

/**
 *
 * @author toepfera
 */
public class Read {

    public String cigar;
    private String sequence;
    public String quality;
    public int start;
    public String id;
    public String ref;

    public Read cut(int fromW, int toW) {
        int readLength = sequence.length();
        try {
            if (toW < start || fromW >= start + readLength) {
                return null;
            }

            if (fromW >= start && start + readLength <= toW) {
                Read r = new Read();
                int from = fromW - start;
                r.start = fromW;
                //r.quality = this.quality.substring(from);
                r.sequence = this.sequence.substring(from);
                r.id = id;
                r.ref = ref;
                return r;
            }

            if (fromW >= start && start + readLength > toW) {
                int from = -1;
                int to = -1;
                try {
                    Read r = new Read();
                    r.start = fromW;
                    from = fromW - start;
                    to = from + toW - fromW;
                    //r.quality = this.quality.substring(from, to);
                    r.sequence = this.sequence.substring(from, to);
                    r.id = id;
                    r.ref = ref;
                    return r;
                } catch (StringIndexOutOfBoundsException e) {
                    System.err.println("fromW: " + fromW + "\ttoW: " + toW);
                    System.err.println("from: " + from + "\ttoW: " + to);
                    System.err.println("start: " + start + "\tlength: " + readLength + "\tto:" + (start + readLength));
                    System.exit(0);
                }
            }
            if (fromW < start && start + readLength <= toW) {
                return this;
            }
            if (fromW < start && start + fromW + readLength <= toW) {
                return this;
            }
            if (fromW < start && fromW + readLength <= toW) {
                Read r = new Read();
                r.start = start;
                int from = 0;
                int to = toW - start;
//            int to = readLength - (start - fromW);
                //r.quality = this.quality.substring(from, to);
                r.sequence = this.sequence.substring(from, to);
                r.id = id;
                r.ref = ref;
                return r;
            }
            if (fromW < start && fromW + readLength > toW) {
                Read r = new Read();
                r.start = start;
                int from = 0;
                int to = toW - start;
                //r.quality = this.quality.substring(from, to);
                r.sequence = this.sequence.substring(from, to);
                r.id = id;
                r.ref = ref;
                return r;
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println(e);
        }
        return null;
    }

    public String getCigars() {
        StringBuilder cigarSB = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        boolean onlyDeletion = true;
        for (char c : sequence.toUpperCase().toCharArray()) {
            switch (c) {
                case 'A':
                case 'C':
                case 'G':
                case 'T':
                    onlyDeletion = false;
                    sb.append('M');
                    break;
                case '-':
                    sb.append('D');
                    break;
            }
        }
        if (onlyDeletion) {
            throw new IllegalStateException("Only deletion");
        }
        String cigarUnfold = sb.toString();
        char prev = 'x';
        int sum = 0;
        int count = 0;
        for (char c : cigarUnfold.toCharArray()) {
            if (c != 0) {
                if (prev == 'x') {
                    prev = c;
                    count++;
                    sum++;
                } else {
                    if (c == prev) {
                        count++;
                        sum++;
                    } else {
                        cigarSB.append(count).append(prev);
                        prev = c;
                        count = 1;
                        sum++;
                    }
                }
            }
        }
//        if (sum != this.sequence.length()) {
//            throw new IllegalStateException("SUM: " + sum + " vs. " + this.sequence.length());
//        }
        cigarSB.append(count).append(prev);
        return cigarSB.toString();
    }

    public String sam() {
        String cigarTmp = null;
        try {
            cigarTmp = getCigars();
        } catch (IllegalStateException e) {
            return "";
        }
        StringBuilder samSB = new StringBuilder();
        samSB.append(id).append("\t");
        samSB.append(0).append("\t");
        samSB.append(ref).append("\t");
        samSB.append(start).append("\t");
        samSB.append(255).append("\t");
        samSB.append(cigarTmp).append("\t");
        samSB.append("*").append("\t");
        samSB.append(0).append("\t");
        samSB.append(0).append("\t");
        samSB.append(getSequence()).append("\t");
        samSB.append("*");
        samSB.append("\n");
        return samSB.toString();
    }

    public String getSequence() {
        StringBuilder sb = new StringBuilder();
        for (char c : sequence.toCharArray()) {
            if (c != '-') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
}
