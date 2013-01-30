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

    public Read() {
    }

    public Read(String sequence, String quality, int start, String id, String ref) {
        this.sequence = sequence;
        this.quality = quality;
        this.start = start;
        this.id = id;
        this.ref = ref;
    }

    public Read cut(int fromW, int toW, int alignmentStart, int alignmentEnd) {
        char[] x = new char[alignmentEnd - alignmentStart];
        char[] q = new char[alignmentEnd - alignmentStart];

        int begin = start - alignmentStart;
        for (int i = 0; i < sequence.length(); i++) {
            char c = sequence.charAt(i);
            if (c == 'N') {
                return null;
            }
            x[begin + i] = c;
            q[begin + i] = quality.charAt(i);
        }

        StringBuilder x_sb = new StringBuilder();
        StringBuilder q_sb = new StringBuilder();
        boolean pre_offset = true;

        int start_new = fromW;
        for (int i = fromW; i < toW; i++) {
            char c_x = x[i - alignmentStart];
            char c_q = q[i - alignmentStart];
            if (c_x != 0) {
                pre_offset = false;
                x_sb.append(c_x);
                q_sb.append(c_q);
            } else {
                if (pre_offset) {
                    start_new++;
                }
            }
        }

        if (x_sb.toString().length() > 0) {
            return new Read(x_sb.toString(), q_sb.toString(), start_new, id, ref);
        } else {
            return null;
        }
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
        if (quality != null && !quality.isEmpty()) {
            samSB.append(getQuality());
        } else {
            samSB.append("*");
        }
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

    public String getQuality() {
        StringBuilder sb = new StringBuilder();
        char[] seq = sequence.toCharArray();
        char[] qual = quality.toCharArray();
        for (int i = 0; i < seq.length; i++) {
            if (seq[i] != '-') {
                sb.append(qual[i]);
            }
        }
        return sb.toString();
    }

    public int getLength() {
        return sequence.length();
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
}
