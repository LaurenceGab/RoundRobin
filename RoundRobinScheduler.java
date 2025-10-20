/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package javaapplication11;
import java.util.*;

/**
 * RoundRobinScheduler.java
 *
 * Menu-driven console program that simulates:
 *  1) Round Robin - Preemptive (standard RR)
 *  2) Round Robin - Non-Preemptive (as described in the lab: runs to completion if remaining <= quantum; otherwise preempt after quantum)
 *
 * - 100% user-input based.
 * - Prints Gantt chart in the boxed/time-marker style (Option 1).
 * - Prints process table (Arrival, Burst, Completion, Turnaround, Waiting).
 * - Prints Average Waiting Time (AWT), Average Turnaround Time (ATT), and CPU Utilization.
 *
 * Author: ChatGPT (GPT-5 Thinking mini) - produce code aligned to user's lab doc
 */

public class RoundRobinScheduler {

    static class Process {
        String pid;
        int arrival;
        int burst;
        int remaining;
        int completion = 0;
        int turnaround = 0;
        int waiting = 0;

        Process(String pid, int arrival, int burst) {
            this.pid = pid;
            this.arrival = arrival;
            this.burst = burst;
            this.remaining = burst;
        }
    }

    // A simple Gantt entry: pid, startTime, endTime
    static class GanttEntry {
        String pid;
        int start;
        int end;

        GanttEntry(String pid, int start, int end) {
            this.pid = pid;
            this.start = start;
            this.end = end;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("ROUND ROBIN CPU SCHEDULER");
        System.out.println("-------------------------");
        System.out.println("Choose algorithm:");
        System.out.println("1 - Round Robin (Preemptive - standard)");
        System.out.println("2 - Round Robin (Non-Preemptive variant)");
        System.out.print("Enter choice (1 or 2): ");
        int choice = readInt(sc, 1, 2);

        System.out.print("Enter number of processes: ");
        int n = readInt(sc, 1, Integer.MAX_VALUE);

        List<Process> processes = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            System.out.print("Process name (or press Enter for default P" + i + "): ");
            String name = sc.nextLine().trim();
            if (name.isEmpty()) name = "P" + i;

            int arrival;
            while (true) {
                System.out.print("Arrival Time of " + name + " (non-negative integer): ");
                try {
                    arrival = Integer.parseInt(sc.nextLine().trim());
                    if (arrival < 0) throw new NumberFormatException();
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid entry. Please enter a non-negative integer.");
                }
            }

            int burst;
            while (true) {
                System.out.print("Burst Time of " + name + " (positive integer): ");
                try {
                    burst = Integer.parseInt(sc.nextLine().trim());
                    if (burst <= 0) throw new NumberFormatException();
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid entry. Please enter a positive integer.");
                }
            }

            processes.add(new Process(name, arrival, burst));
        }

        int quantum;
        while (true) {
            System.out.print("Enter Time Quantum (positive integer): ");
            try {
                quantum = Integer.parseInt(sc.nextLine().trim());
                if (quantum <= 0) throw new NumberFormatException();
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid entry. Please enter a positive integer.");
            }
        }

        // Sort by arrival time (stable)
        processes.sort(Comparator.comparingInt(p -> p.arrival));

        if (choice == 1) {
            simulatePreemptiveRR(processes, quantum);
        } else {
            simulateNonPreemptiveVariantRR(processes, quantum);
        }

        sc.close();
    }

    // Read an integer in range [min, max]
    private static int readInt(Scanner sc, int min, int max) {
        while (true) {
            try {
                int v = Integer.parseInt(sc.nextLine().trim());
                if (v < min || v > max) throw new NumberFormatException();
                return v;
            } catch (NumberFormatException e) {
                System.out.print("Invalid. Enter a value between " + min + " and " + max + ": ");
            }
        }
    }

    // Preemptive Round Robin standard simulation
    private static void simulatePreemptiveRR(List<Process> originalProcesses, int quantum) {
        // Deep copy processes so originalProcesses remains untouched for other run
        List<Process> procs = copyProcesses(originalProcesses);

        int currentTime = 0;
        Queue<Integer> q = new LinkedList<>();
        List<GanttEntry> gantt = new ArrayList<>();
        int n = procs.size();
        boolean[] inQueue = new boolean[n];

        int completed = 0;
        int idx = 0; // index to add arriving processes

        // If there are gaps before first arrival, advance time to first arrival
        if (n > 0) currentTime = procs.get(0).arrival;

        while (completed < n) {
            // enqueue all processes that have arrived by currentTime
            for (int i = 0; i < n; i++) {
                if (!inQueue[i] && procs.get(i).arrival <= currentTime && procs.get(i).remaining > 0) {
                    q.add(i);
                    inQueue[i] = true;
                }
            }

            if (q.isEmpty()) {
                // No process ready -> jump to next arrival
                int nextArrival = Integer.MAX_VALUE;
                for (Process p : procs) if (p.remaining > 0) nextArrival = Math.min(nextArrival, p.arrival);
                if (nextArrival != Integer.MAX_VALUE) {
                    currentTime = Math.max(currentTime, nextArrival);
                    for (int i = 0; i < n; i++) {
                        if (!inQueue[i] && procs.get(i).arrival <= currentTime && procs.get(i).remaining > 0) {
                            q.add(i);
                            inQueue[i] = true;
                        }
                    }
                }
                continue;
            }

            int i = q.poll(); // process index
            Process p = procs.get(i);

            int execTime = Math.min(quantum, p.remaining);
            int start = currentTime;
            int end = start + execTime;

            // Record Gantt
            gantt.add(new GanttEntry(p.pid, start, end));

            p.remaining -= execTime;
            currentTime = end;

            // Enqueue newly arrived processes during this time slice
            for (int j = 0; j < n; j++) {
                if (!inQueue[j] && procs.get(j).arrival <= currentTime && procs.get(j).remaining > 0) {
                    q.add(j);
                    inQueue[j] = true;
                }
            }

            if (p.remaining > 0) {
                q.add(i); // still has remaining -> requeue
            } else {
                // process finished
                p.completion = currentTime;
                p.turnaround = p.completion - p.arrival;
                p.waiting = p.turnaround - p.burst;
                completed++;
            }
        }

        printResults("Round Robin (Preemptive)", procs, gantt);
    }

    // Non-preemptive variant as described in the lab doc:
    // - If remaining <= quantum => run it to completion
    // - Else => run for quantum and preempt (requeue)
    private static void simulateNonPreemptiveVariantRR(List<Process> originalProcesses, int quantum) {
        List<Process> procs = copyProcesses(originalProcesses);

        int currentTime = 0;
        Queue<Integer> q = new LinkedList<>();
        List<GanttEntry> gantt = new ArrayList<>();
        int n = procs.size();
        boolean[] inQueue = new boolean[n];

        if (n > 0) currentTime = procs.get(0).arrival;

        int completed = 0;

        while (completed < n) {
            // enqueue arrived processes
            for (int i = 0; i < n; i++) {
                if (!inQueue[i] && procs.get(i).arrival <= currentTime && procs.get(i).remaining > 0) {
                    q.add(i);
                    inQueue[i] = true;
                }
            }

            if (q.isEmpty()) {
                // jump to next arrival
                int nextArrival = Integer.MAX_VALUE;
                for (Process p : procs) if (p.remaining > 0) nextArrival = Math.min(nextArrival, p.arrival);
                if (nextArrival != Integer.MAX_VALUE) {
                    currentTime = Math.max(currentTime, nextArrival);
                    for (int i = 0; i < n; i++) {
                        if (!inQueue[i] && procs.get(i).arrival <= currentTime && procs.get(i).remaining > 0) {
                            q.add(i);
                            inQueue[i] = true;
                        }
                    }
                }
                continue;
            }

            int i = q.poll();
            Process p = procs.get(i);

            // Here is the key difference: if remaining <= quantum -> run to completion (non-preemptive),
            // otherwise run for quantum and preempt (requeue).
            int execTime;
            if (p.remaining <= quantum) execTime = p.remaining; // finish it
            else execTime = quantum; // preempt after quantum

            int start = currentTime;
            int end = start + execTime;

            gantt.add(new GanttEntry(p.pid, start, end));

            p.remaining -= execTime;
            currentTime = end;

            // enqueue any new arrivals
            for (int j = 0; j < n; j++) {
                if (!inQueue[j] && procs.get(j).arrival <= currentTime && procs.get(j).remaining > 0) {
                    q.add(j);
                    inQueue[j] = true;
                }
            }

            if (p.remaining > 0) {
                q.add(i); // requeue
            } else {
                p.completion = currentTime;
                p.turnaround = p.completion - p.arrival;
                p.waiting = p.turnaround - p.burst;
                completed++;
            }
        }

        printResults("Round Robin (Non-Preemptive Variant)", procs, gantt);
    }

    // Utility to deep-copy processes list
    private static List<Process> copyProcesses(List<Process> src) {
        List<Process> copy = new ArrayList<>();
        for (Process p : src) {
            copy.add(new Process(p.pid, p.arrival, p.burst));
        }
        // Keep them sorted by arrival (to match typical input)
        copy.sort(Comparator.comparingInt(p -> p.arrival));
        return copy;
    }

    // Printing results: Gantt chart (Option 1 style), process table, AWT, ATT, CPU Utilization
    private static void printResults(String title, List<Process> procs, List<GanttEntry> gantt) {
        System.out.println();
        System.out.println("========================================");
        System.out.println(title);
        System.out.println("========================================");

        // Print Gantt Chart in requested style
        // We'll print times row and process row. Each segment will use width 5 for small neat alignment.
        final int WIDTH = 5;
        StringBuilder times = new StringBuilder();
        StringBuilder procsRow = new StringBuilder();

        times.append("Time: ");
        procsRow.append("Proc: ");

        // For option 1 look: we will print the start times for each segment followed by the final end time
        // and print process labels under each segment.
        for (int i = 0; i < gantt.size(); i++) {
            GanttEntry e = gantt.get(i);
            // append start time aligned to WIDTH
            times.append(String.format("%" + WIDTH + "d", e.start));
        }
        // append final end time (last entry's end)
        if (!gantt.isEmpty()) {
            times.append(String.format("%" + WIDTH + "d", gantt.get(gantt.size() - 1).end));
        } else {
            times.append(String.format("%" + WIDTH + "d", 0));
        }

        // processes row: print each pid centered under the segment width
        for (GanttEntry e : gantt) {
            String label = e.pid;
            int pad = WIDTH - label.length();
            int left = pad / 2;
            int right = pad - left;
            String seg = " ".repeat(Math.max(0, left)) + label + " ".repeat(Math.max(0, right));
            procsRow.append(seg);
        }

        System.out.println();
        System.out.println("Gantt Chart:");
        System.out.println(times.toString());
        System.out.println(procsRow.toString());
        System.out.println();

        // Print Process table header
        System.out.println("Output");
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-8s | %-12s | %-10s | %-15s | %-12s | %-12s\n",
                "Process", "Arrival Time", "Burst Time", "Completion Time", "Turnaround", "Waiting Time");
        System.out.println("--------------------------------------------------------------------------");

        double totalWaiting = 0;
        double totalTurnaround = 0;
        int lastCompletion = 0;
        int totalBurst = 0;
        for (Process p : procs) {
            totalWaiting += p.waiting;
            totalTurnaround += p.turnaround;
            lastCompletion = Math.max(lastCompletion, p.completion);
            totalBurst += p.burst;
            System.out.printf("%-8s | %-12d | %-10d | %-15d | %-12d | %-12d\n",
                    p.pid, p.arrival, p.burst, p.completion, p.turnaround, p.waiting);
        }
        System.out.println("--------------------------------------------------------------------------");

        double awt = totalWaiting / procs.size();
        double att = totalTurnaround / procs.size();

        // CPU Utilization: we use (total burst time / last completion time) * 100
        double cpuUtil = (lastCompletion == 0) ? 0.0 : ((double) totalBurst / (double) lastCompletion) * 100.0;

        // Print Performance metrics with formatting similar to lab sample
        System.out.println();
        System.out.println("Performance Metrics");
        System.out.printf("CPU Utilization: %.1f%%\n", cpuUtil);
        System.out.printf("Average Waiting Time (AWT): %.1f\n", awt);
        System.out.printf("Average Turnaround Time (ATT): %.1f\n", att);
        System.out.println();
        // End
    }
}