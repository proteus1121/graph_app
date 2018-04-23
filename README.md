Graph app, which random links between points until not found the shortest way from first row to last.

The goal of this work is to develop a program that implements discrete modeling of an object in two-dimensional space.
To achieve this goal, several tasks are being accomplished:
1. Formation of the working area (grid).
2. Eject elements randomly to obtain an infinite cluster.
3. The first stage of parsing the grid (the removal of dangling bonds of the first kind, the folding of parallel and serial connections where possible).
4. Finding the shortest path.
5. The second stage of parsing the grid (dumping hanging networks).
6. Calculation of network resistance. Finding the total resistance of the cluster obtained.

![Alt text](demo.jpg?raw=true "Demo")

Technology, which used in this work: Java 8, JGraphX, Swing