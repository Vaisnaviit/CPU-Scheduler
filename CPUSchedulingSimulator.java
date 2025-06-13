import java.util.*;

public class CPUSchedulingSimulator {

    // Global Constants for Operation Types (not strictly used as strings in Java version, but good to keep if intended)
    // NetBeans might suggest these could be unused if not explicitly referenced for control flow.
    // Making them final is a good practice for constants.
    private static final String TRACE = "trace";
    private static final String SHOW_STATISTICS = "stats";

    // Global variables for process scheduling data
    private static int last_instant = 0;
    private static int process_count = 0;

    // Custom class to represent a process, similar to std::tuple in C++
    // NetBeans will suggest making the fields 'final' as they are set in the constructor and not modified.
    static class Process {
        final String name;
        final int arrivalTime;
        final int serviceTime;

        public Process(String name, int arrivalTime, int serviceTime) {
            this.name = name;
            this.arrivalTime = arrivalTime;
            this.serviceTime = serviceTime;
        }

        public String getName() {
            return name;
        }

        public int getArrivalTime() {
            return arrivalTime;
        }

        public int getServiceTime() {
            return serviceTime;
        }
    }

    private static List<Process> processes; // Stores process name, arrival time, service time
    private static List<List<Character>> timeline; // Represents the timeline of process execution
    private static Map<String, Integer> processToIndex; // Maps process names to their index in the processes vector

    // Results vectors
    private static List<Integer> finishTime;     // Stores the finish time for each process
    private static List<Integer> turnAroundTime; // Stores the turnaround time for each process
    private static List<Float> normTurn;     // Stores the normalized turnaround time for each process

    // Array of algorithm names for printing statistics
    private static final String[] ALGORITHMS = {"", "FCFS", "RR-", "SPN", "SRT", "HRRN", "FB-1", "FB-2i", "AGING"};

    // Function to clear the timeline for a new simulation
    private static void clear_timeline() {
        for (int i = 0; i < last_instant; i++) {
            for (int j = 0; j < process_count; j++) {
                timeline.get(i).set(j, ' ');
            }
        }
    }

    // Helper functions to get process attributes (now directly from Process object)
    private static int getArrivalTime(Process p) { return p.getArrivalTime(); }
    private static int getServiceTime(Process p) { return p.getServiceTime(); }
    private static String getProcessName(Process p) { return p.getName(); }

    // Function to calculate the response ratio for HRRN
    private static double calculate_response_ratio(int wait_time, int service_time) {
        return (wait_time + service_time) * 1.0 / service_time;
    }

    // Fills in '.' for wait times in the timeline after a process finishes
    private static void fillInWaitTime() {
        for (int i = 0; i < process_count; i++) {
            int arrivalTime = getArrivalTime(processes.get(i));
            for (int k = arrivalTime; k < finishTime.get(i); k++) {
                if (timeline.get(k).get(i) != '*') {
                    timeline.get(k).set(i, '.'); // Mark as waiting if not running
                }
            }
        }
    }

    // First Come First Serve (FCFS) scheduling algorithm
    private static void firstComeFirstServe() {
        // Start time is the arrival time of the first process
        int time = getArrivalTime(processes.get(0));
        for (int i = 0; i < process_count; i++) {
            int arrival = getArrivalTime(processes.get(i));
            int service = getServiceTime(processes.get(i));

            // Calculate finish time, turnaround time, and normalized turnaround time
            finishTime.set(i, time + service);
            turnAroundTime.set(i, finishTime.get(i) - arrival);
            normTurn.set(i, (float) (turnAroundTime.get(i) * 1.0 / service));

            // Fill the timeline: '*' for running, '.' for waiting
            for (int j = time; j < finishTime.get(i); j++) {
                timeline.get(j).set(i, '*');
            }
            for (int j = arrival; j < time; j++) {
                timeline.get(j).set(i, '.');
            }
            time = finishTime.get(i); // Update current time
        }
    }

    // Round Robin (RR) scheduling algorithm
    private static void roundRobin(int quantum) {
        Queue<Integer> q = new LinkedList<>(); // Queue to hold indices of ready processes
        List<Integer> remaining = new ArrayList<>(process_count); // Remaining service time for each process

        // Initialize remaining service times
        for (int i = 0; i < process_count; ++i) {
            remaining.add(getServiceTime(processes.get(i)));
        }

        int time = 0, j = 0; // Current time and index for processes that have arrived

        // Loop until all processes are completed or time reaches last_instant
        while (time < last_instant || !q.isEmpty()) {
            // Add newly arrived processes to the queue
            while (j < process_count && getArrivalTime(processes.get(j)) <= time) {
                q.add(j++);
            }

            if (!q.isEmpty()) {
                int idx = q.poll(); // Get the process at the front of the queue

                // Determine how long the process will run in this quantum
                int run = Math.min(quantum, remaining.get(idx));

                // Mark the timeline for the running process
                for (int t = 0; t < run; ++t) {
                    if (time + t < last_instant) // Ensure we don't go out of bounds
                        timeline.get(time + t).set(idx, '*');
                }

                time += run; // Advance time
                remaining.set(idx, remaining.get(idx) - run); // Decrease remaining service time

                // Add any processes that arrived during this quantum to the queue
                while (j < process_count && getArrivalTime(processes.get(j)) <= time) {
                    q.add(j++);
                }

                // If the process is not yet finished, add it back to the queue
                if (remaining.get(idx) > 0) {
                    q.add(idx);
                } else {
                    // If the process is finished, calculate its metrics
                    finishTime.set(idx, time);
                    turnAroundTime.set(idx, time - getArrivalTime(processes.get(idx)));
                    normTurn.set(idx, (float) (turnAroundTime.get(idx) * 1.0 / getServiceTime(processes.get(idx))));
                }
            } else {
                // If the queue is empty, increment time to find the next arriving process
                time++;
            }
        }
        fillInWaitTime(); // Mark wait times in the timeline
    }

    // Shortest Process Next (SPN) scheduling algorithm
    private static void shortestProcessNext() {
        List<Boolean> done = new ArrayList<>(Collections.nCopies(process_count, false)); // Tracks if a process is completed
        int time = 0; // Current time

        // Loop until all processes are completed
        while (done.contains(false)) { // Equivalent to C++'s count(done.begin(), done.end(), true) < process_count
            int idx = -1; // Index of the selected process
            int minST = Integer.MAX_VALUE; // Minimum service time found

            // Find the process with the shortest service time among arrived and not done processes
            for (int i = 0; i < process_count; ++i) {
                if (!done.get(i) && getArrivalTime(processes.get(i)) <= time && getServiceTime(processes.get(i)) < minST) {
                    idx = i;
                    minST = getServiceTime(processes.get(i));
                }
            }

            if (idx == -1) {
                time++; // No process is ready, advance time
                continue;
            }

            // Execute the selected process
            int st = getServiceTime(processes.get(idx));
            for (int t = time; t < time + st; ++t) {
                if (t < last_instant) // Ensure we don't go out of bounds
                    timeline.get(t).set(idx, '*');
            }
            time += st; // Advance time by service time

            // Calculate metrics for the finished process
            finishTime.set(idx, time);
            turnAroundTime.set(idx, time - getArrivalTime(processes.get(idx)));
            normTurn.set(idx, (float) (turnAroundTime.get(idx) * 1.0 / minST));
            done.set(idx, true); // Mark process as done
        }
        fillInWaitTime(); // Mark wait times in the timeline
    }

    // Shortest Remaining Time (SRT) scheduling algorithm
    private static void shortestRemainingTime() {
        List<Integer> remaining = new ArrayList<>(process_count); // Remaining service time for each process
        for (int i = 0; i < process_count; i++) {
            remaining.add(getServiceTime(processes.get(i)));
        }

        List<Boolean> done = new ArrayList<>(Collections.nCopies(process_count, false)); // Tracks if a process is completed
        int time = 0, completed = 0; // Current time and number of completed processes

        // Loop until all processes are completed
        while (completed < process_count) {
            int idx = -1; // Index of the selected process
            int minRem = Integer.MAX_VALUE; // Minimum remaining time found

            // Find the process with the shortest remaining time among arrived and not done processes
            for (int i = 0; i < process_count; ++i) {
                if (!done.get(i) && getArrivalTime(processes.get(i)) <= time && remaining.get(i) < minRem && remaining.get(i) > 0) {
                    idx = i;
                    minRem = remaining.get(i);
                }
            }

            if (idx == -1) {
                time++; // No process is ready, advance time
                continue;
            }

            // Execute the selected process for one unit of time
            if (time < last_instant) // Ensure we don't go out of bounds
                timeline.get(time).set(idx, '*');
            remaining.set(idx, remaining.get(idx) - 1);
            time++;

            // If the process is finished
            if (remaining.get(idx) == 0) {
                finishTime.set(idx, time);
                int arrival = getArrivalTime(processes.get(idx));
                int service = getServiceTime(processes.get(idx));
                turnAroundTime.set(idx, time - arrival);
                normTurn.set(idx, (float) (turnAroundTime.get(idx) * 1.0 / service));
                done.set(idx, true);
                completed++;
            }
        }
        fillInWaitTime(); // Mark wait times in the timeline
    }

    // Highest Response Ratio Next (HRRN) scheduling algorithm
    private static void highestResponseRatioNext() {
        List<Boolean> done = new ArrayList<>(Collections.nCopies(process_count, false)); // Tracks if a process is completed
        int time = 0; // Current time

        // Loop until all processes are completed
        while (done.contains(false)) {
            double maxRatio = -1.0; // Maximum response ratio found
            int idx = -1; // Index of the selected process

            // Find the process with the highest response ratio among arrived and not done processes
            for (int i = 0; i < process_count; ++i) {
                if (!done.get(i) && getArrivalTime(processes.get(i)) <= time) {
                    int wait = time - getArrivalTime(processes.get(i));
                    int service = getServiceTime(processes.get(i));
                    double ratio = calculate_response_ratio(wait, service);
                    if (ratio > maxRatio) {
                        maxRatio = ratio;
                        idx = i;
                    }
                }
            }

            if (idx == -1) {
                time++; // No process is ready, advance time
                continue;
            }

            // Execute the selected process
            int st = getServiceTime(processes.get(idx));
            for (int t = time; t < time + st; ++t) {
                if (t < last_instant) // Ensure we don't go out of bounds
                    timeline.get(t).set(idx, '*');
            }
            time += st; // Advance time by service time

            // Calculate metrics for the finished process
            finishTime.set(idx, time);
            turnAroundTime.set(idx, time - getArrivalTime(processes.get(idx)));
            normTurn.set(idx, (float) (turnAroundTime.get(idx) * 1.0 / st));
            done.set(idx, true); // Mark process as done
        }
        fillInWaitTime(); // Mark wait times in the timeline
    }

    // Aging scheduling algorithm (simple priority aging)
    private static void agingAlgorithm() {
        List<Integer> remaining = new ArrayList<>(process_count); // Remaining service time for each process
        for (int i = 0; i < process_count; i++) {
            remaining.add(getServiceTime(processes.get(i)));
        }

        List<Integer> priority = new ArrayList<>(Collections.nCopies(process_count, 0)); // Priority for each process (increases with age)
        List<Boolean> done = new ArrayList<>(Collections.nCopies(process_count, false)); // Tracks if a process is completed
        int time = 0; // Current time

        // Loop until all processes are completed
        while (done.contains(false)) {
            int idx = -1; // Index of the selected process

            // Find the process with the highest priority among arrived and not done processes
            for (int i = 0; i < process_count; ++i) {
                if (!done.get(i) && getArrivalTime(processes.get(i)) <= time) {
                    priority.set(i, priority.get(i) + 1); // Increase priority for waiting processes
                    if (idx == -1 || priority.get(i) > priority.get(idx)) {
                        idx = i;
                    }
                }
            }

            if (idx == -1) {
                time++; // No process is ready, advance time
                continue;
            }

            // Execute the selected process for one unit of time
            if (time < last_instant) // Ensure we don't go out of bounds
                timeline.get(time).set(idx, '*');
            remaining.set(idx, remaining.get(idx) - 1);
            time++;

            // If the process is finished
            if (remaining.get(idx) == 0) {
                finishTime.set(idx, time);
                turnAroundTime.set(idx, time - getArrivalTime(processes.get(idx)));
                normTurn.set(idx, (float) (turnAroundTime.get(idx) * 1.0 / getServiceTime(processes.get(idx))));
                done.set(idx, true);
            }
        }
        fillInWaitTime(); // Mark wait times in the timeline
    }

    // Tracing Function: printTimeline
    // This function prints the timeline of process execution, showing which process is running ('*') or waiting ('.') at each time instant.
    private static void printTimelineOutput() {
        // Prints the time instants (0-9 repeating)
        for (int i = 0; i <= last_instant; i++)
            System.out.print(i % 10 + " ");
        System.out.println("\n------------------------------------------------");

        // Iterates through each process
        for (int i = 0; i < process_count; i++) {
            // Prints the process name
            System.out.print(getProcessName(processes.get(i)) + "     |");
            // Prints the state of the process at each time instant
            for (int j = 0; j < last_instant; j++) {
                System.out.print(timeline.get(j).get(i) + "|");
            }
            System.out.println(" ");
        }
        System.out.println("------------------------------------------------");
    }

    // Statistics Functions

    // printAlgorithm: Prints the name of the current scheduling algorithm.
    private static void printAlgorithm(int algorithm_id_val, int quantum_val) {
        if (algorithm_id_val == 2) // For Round Robin (RR-) which includes quantum
            System.out.println(ALGORITHMS[algorithm_id_val] + quantum_val);
        else
            System.out.println(ALGORITHMS[algorithm_id_val]);
    }

    // printProcesses: Prints the names of all processes.
    private static void printProcesses() {
        System.out.print("Process    ");
        for (int i = 0; i < process_count; i++)
            System.out.print("|  " + getProcessName(processes.get(i)) + "  ");
        System.out.println("|");
    }

    // printArrivalTime: Prints the arrival time for each process.
    private static void printArrivalTime() {
        System.out.print("Arrival    ");
        for (int i = 0; i < process_count; i++)
            System.out.printf("|%3d  ", getArrivalTime(processes.get(i)));
        System.out.println("|");
    }

    // printServiceTime: Prints the service time for each process and calculates the mean service time.
    private static void printServiceTime() {
        System.out.print("Service    |");
        for (int i = 0; i < process_count; i++)
            System.out.printf("%3d  |", getServiceTime(processes.get(i)));
        System.out.println(" Mean|");
    }

    // printFinishTime: Prints the finish time for each process.
    private static void printFinishTime() {
        System.out.print("Finish     ");
        for (int i = 0; i < process_count; i++)
            System.out.printf("|%3d  ", finishTime.get(i));
        System.out.println("|-----|");
    }

    // printTurnAroundTime: Prints the turnaround time for each process and calculates the mean turnaround time.
    private static void printTurnAroundTime() {
        System.out.print("Turnaround |");
        int sum = 0;
        for (int i = 0; i < process_count; i++) {
            System.out.printf("%3d  |", turnAroundTime.get(i));
            sum += turnAroundTime.get(i);
        }
        if ((1.0 * sum / turnAroundTime.size()) >= 10)
            System.out.printf("%2.2f|\n", (1.0 * sum / turnAroundTime.size()));
        else
            System.out.printf(" %2.2f|\n", (1.0 * sum / turnAroundTime.size()));
    }

    // printNormTurn: Prints the normalized turnaround time for each process and calculates the mean normalized turnaround time.
    private static void printNormTurn() {
        System.out.print("NormTurn   |");
        float sum = 0;
        for (int i = 0; i < process_count; i++) {
            if (normTurn.get(i) >= 10)
                System.out.printf("%2.2f|", normTurn.get(i));
            else
                System.out.printf(" %2.2f|", normTurn.get(i));
            sum += normTurn.get(i);
        }

        if ((1.0 * sum / normTurn.size()) >= 10)
            System.out.printf("%2.2f|\n", (1.0 * sum / normTurn.size()));
        else
            System.out.printf(" %2.2f|\n", (1.0 * sum / normTurn.size()));
    }

    // printStats: Orchestrates the printing of all statistical metrics.
    private static void printStatsOutput(int algorithm_id_val, int quantum_val) {
        printAlgorithm(algorithm_id_val, quantum_val);
        printProcesses();
        printArrivalTime();
        printServiceTime();
        printFinishTime();
        printTurnAroundTime();
        printNormTurn();
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=========================================");
        System.out.println(" CPU SCHEDULING SIMULATOR TOOL      ");
        System.out.println(" (Supports FCFS, RR, SPN, SRT, HRRN, AGING)");
        System.out.println("=========================================\n");

        System.out.print("Enter number of processes: ");
        process_count = scanner.nextInt();

        processes = new ArrayList<>(process_count);
        finishTime = new ArrayList<>(Collections.nCopies(process_count, 0));
        turnAroundTime = new ArrayList<>(Collections.nCopies(process_count, 0));
        normTurn = new ArrayList<>(Collections.nCopies(process_count, 0.0f));
        processToIndex = new HashMap<>();

        for (int i = 0; i < process_count; ++i) {
            String name;
            int arrival, service;
            System.out.print("Enter name, arrival time, and service time for process " + (i + 1) + ": ");
            name = scanner.next();
            arrival = scanner.nextInt();
            service = scanner.nextInt();
            processes.add(new Process(name, arrival, service));
            processToIndex.put(name, i);
            last_instant = Math.max(last_instant, arrival + service);
        }
        // Add some buffer to last_instant for timeline display
        last_instant += 10;

        timeline = new ArrayList<>(last_instant);
        for (int i = 0; i < last_instant; i++) {
            timeline.add(new ArrayList<>(Collections.nCopies(process_count, ' ')));
        }


        System.out.println("Choose Algorithm:\n1. FCFS\n2. RR\n3. SPN\n4. SRT\n5. HRRN\n6. AGING\nEnter choice: ");
        int choice = scanner.nextInt();

        int quantum = -1; // Default quantum, only used for RR
        if (choice == 2) {
            System.out.print("Enter quantum for Round Robin: ");
            quantum = scanner.nextInt();
        }

        clear_timeline(); // Clear timeline before running the selected algorithm

        // Execute the chosen algorithm
        switch (choice) {
            case 1: firstComeFirstServe(); break;
            case 2: roundRobin(quantum); break;
            case 3: shortestProcessNext(); break;
            case 4: shortestRemainingTime(); break;
            case 5: highestResponseRatioNext(); break;
            case 6: agingAlgorithm(); break;
            default:
                System.out.println("Invalid algorithm choice.");
                scanner.close();
                return;
        }

        System.out.println("\nChoose Output Format:\n1. Trace\n2. Statistics\nEnter choice: ");
        int outputChoice = scanner.nextInt();

        if (outputChoice == 1) {
            printTimelineOutput(); // Call the new printTimelineOutput function
        } else if (outputChoice == 2) {
            printStatsOutput(choice, quantum); // Call the new printStatsOutput function with algorithm choice and quantum
        } else {
            System.out.println("Invalid output choice.");
        }

        scanner.close();
    }
}