BAMExtract
==========

Extracts regions from a BAM file and exports in SAM format

USAGE:
```
java -jar bamextract.jar options...

=== GENERAL options ===
-i PATH    : Path to the input file (BAM format) [REQUIRED]
-r STRING  : Regions in format start-end,start2-end2 [REQUIRED]

=== EXAMPLE ===
java -jar bamextract.jar -i reads.bam -r 320-600,1220-4000 
```
