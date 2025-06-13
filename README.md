# CPU-Scheduler
This is a command-line CPU scheduling simulator implemented in Java. It supports various common scheduling algorithms to demonstrate how processes are managed and executed by an operating system.

# Supported Algorithms

* First Come First Serve (FCFS)
* Round Robin (RR)
* Shortest Process Next (SPN)
* Shortest Remaining Time (SRT)
* Highest Response Ratio Next (HRRN)
* Aging

# Features
**Process Input**: Define processes with names, arrival times, and service times.
**Algorithm Selection**: Choose from a list of implemented scheduling algorithms.
 **Output Formats**:
    **Trace**: Visual timeline showing process execution (`*`) and waiting (`.`) at each time instant.
    **Statistics**: Detailed metrics including finish time, turnaround time, and normalized turnaround time for each process, along with mean values.

## How to Run
Prerequisites
* Java Development Kit (JDK) 8 or higher installed.
Steps
1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/YOUR_USERNAME/your-repo-name.git](https://github.com/YOUR_USERNAME/your-repo-name.git)
    ```
    (Replace `YOUR_USERNAME` and `your-repo-name` with your actual GitHub username and repository name.)

2.  **Navigate to the project directory:**
    ```bash
    cd your-repo-name/src
    ```

3.  **Compile the Java code:**
    ```bash
    javac CPUSchedulingSimulator.java
    ```

4.  **Run the compiled program:**
    ```bash
    java CPUSchedulingSimulator
    ```

5.  **Follow the on-screen prompts** to enter process details and select the desired algorithm and output format.
