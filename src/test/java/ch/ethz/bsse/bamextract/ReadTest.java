/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.ethz.bsse.bamextract;

import org.junit.Test;

/**
 *
 * @author toepfera
 */
public class ReadTest {

    Read r;

    public ReadTest() {
        r = new Read();
        r.id = "ID";
        r.cigar = "MMMMMMMMMM";
        r.quality = "AAAAAAAAAA";
        r.ref = "ref";
        r.setSequence("ABCDEFGHIJ");
        r.start = 4000;
    }

    @Test
    public void overlapMAX() {
        System.out.println("0-8000");
        int from = 0;
        int to = 8000;
        Read expResult = r.cut(from, to);
        System.out.print(r.sam());
        if (expResult == null) {
            System.out.println("null");
        } else {
            System.out.print(expResult.sam());
        }
    }
    @Test
    public void overlapMAX2() {
        System.out.println("0-4001");
        int from = 0;
        int to = 4001;
        Read expResult = r.cut(from, to);
        System.out.print(r.sam());
        if (expResult == null) {
            System.out.println("null");
        } else {
            System.out.print(expResult.sam());
        }
    }
    @Test
    public void overlapMAX4() {
        System.out.println("4001-4002");
        int from = 4001;
        int to = 4002;
        Read expResult = r.cut(from, to);
        System.out.print(r.sam());
        if (expResult == null) {
            System.out.println("null");
        } else {
            System.out.print(expResult.sam());
        }
    }
    @Test
    public void overlapMAX3() {
        System.out.println("4005-8000");
        int from = 4005;
        int to = 8000;
        Read expResult = r.cut(from, to);
        System.out.print(r.sam());
        if (expResult == null) {
            System.out.println("null");
        } else {
            System.out.print(expResult.sam());
        }
    }
    @Test
    public void overlap0() {
        System.out.println("5-10");
        int from = 5;
        int to = 10;
        Read expResult = r.cut(from, to);
        System.out.print(r.sam());
        if (expResult == null) {
            System.out.println("null");
        } else {
            System.out.print(expResult.sam());
        }
    }

    @Test
    public void overlap1() {
        System.out.println("0-10");
        int from = 0;
        int to = 10;
        Read expResult = r.cut(from, to);
        System.out.print(r.sam());
        if (expResult == null) {
            System.out.println("null");
        } else {
            System.out.print(expResult.sam());
        }
    }

    @Test
    public void overlap2() {
        System.out.println("6-10");
        int from = 6;
        int to = 10;
        Read expResult = r.cut(from, to);
        System.out.print(r.sam());
        if (expResult == null) {
            System.out.println("null");
        } else {
            System.out.print(expResult.sam());
        }
    }

    @Test
    public void overlap3() {
        System.out.println("5-15");
        int from = 5;
        int to = 15;
        Read expResult = r.cut(from, to);
        System.out.print(r.sam());
        if (expResult == null) {
            System.out.println("null");
        } else {
            System.out.print(expResult.sam());
        }
    }

    @Test
    public void overlap4() {
        System.out.println("10-15");
        int from = 10;
        int to = 15;
        Read expResult = r.cut(from, to);
        System.out.print(r.sam());
        if (expResult == null) {
            System.out.println("null");
        } else {
            System.out.print(expResult.sam());
        }
    }

    @Test
    public void overlap5() {
        System.out.println("10-25");
        int from = 10;
        int to = 25;
        Read expResult = r.cut(from, to);
        System.out.print(r.sam());
        if (expResult == null) {
            System.out.println("null");
        } else {
            System.out.print(expResult.sam());
        }
    }

    @Test
    public void overlap6() {
        System.out.println("0-25");
        int from = 0;
        int to = 25;
        Read expResult = r.cut(from, to);
        System.out.print(r.sam());
        if (expResult == null) {
            System.out.println("null");
        } else {
            System.out.print(expResult.sam());
        }
    }
}
