# CSE-322 Computer Networks Sessional

This repository contains the assignments, projects, and practical work related to the Computer Networks sessional course (CSE-322) at BUET.

## Repository Structure

The repository is organized into three main directories:

1. **Offline 1** - File Server Implementation
2. **NS3** - Network Simulation Projects
3. **Packet Tracer** - Network Configuration Exercises

## Offline 1: File Server

This directory contains a client-server file sharing application implemented in Java.

### Contents:
- **Assignment 1.pdf**: The assignment specification document
- **File Server/**: Source code directory containing:
  - `src/Client/`: Client-side implementation
  - `src/Server/`: Server-side implementation
  - `src/Utils/`: Utility classes

### Implementation Details:

#### Server Features:
- Multi-threaded server handling multiple client connections
- User authentication and session management
- File upload/download with buffer management (100MB max buffer size)
- Random chunk size generation (10-100 units) for efficient file transfers
- Request management system with broadcast notifications
- Separate socket handlers for file transfer and messaging

#### Client Features:
- Login/authentication to server
- File uploading with chunking mechanism
- Viewing public and private file lists
- File download requests and transfers
- Messaging system for notifications

#### Utility Classes:
- `MessageType`: Enum defining different message types for client-server communication
- `Message`: Serializable message objects for communication
- `File`: File metadata and attributes handling
- `Chunk`: File chunk management for efficient transfers
- `Request`: File request handling

### Protocol:
The application implements a custom application-layer protocol with the following message types:
- LOGIN_REQUEST, LOGIN_OK, LOGIN_FAIL: Authentication messages
- LOOKUP_STD, LOOKUP_FILES_OTHER, LOOKUP_FILES_SELF: Directory listing
- FILE_REQUEST, FILE_ACCEPT, FILE_REJECT: File transfer negotiation
- FILE_UL_COMPLETE, BUFFER_FULL, CHUNK_RECEIVED: Transfer status messages
- BROADCAST, ANNOUNCE: Notification messages

## NS3: Network Simulation Projects

This directory contains network simulations using the NS3 (Network Simulator 3) platform.

### Contents:
- **1705069/**: Student implementation directory containing:
  - `TaskA/`: TCP connection performance analysis
  - `TaskB/`: TCP Reno vs Vegas implementation and comparison
  - `NS3_Report.pdf`: Documentation of simulation results and analysis
  - Various Excel files for data analysis
- **Relevant Files/**: Supporting materials
- **Documentation PDFs**:
  - `ns3_project_guideline.pdf`: Project guidelines
  - `ns3_cheatbook.pdf`: Quick reference guide for NS3
  - `ns-3-tutorial.pdf`: Tutorial for NS3
  - `ns-3-model-library.pdf`: Documentation of NS3 models
  - `ns-3-manual.pdf`: Comprehensive NS3 manual
  - `NS3 Group Presentation Outline V2.pdf`: Presentation outline

### Implementation Details:

#### Task A: Network Performance Analysis
- Implementation of network topologies with WiFi nodes and point-to-point links
- Data rate configuration and packet transmission setup
- Flow monitoring and performance metrics collection
- Analysis of congestion window evolution with TCP variants
- Packet drop monitoring and analysis

#### Task B: TCP Variants Implementation
- Implementation and modification of TCP Vegas congestion control algorithm
- Comparison with TCP Reno for fairness analysis
- Modification of socket state handling and congestion control mechanisms
- Performance analysis under different network conditions
- Implementation of socket base modifications for TCP protocol enhancements

### TCP Vegas+ Modification

#### Problem Statement
TCP Vegas and TCP Reno exhibit unfairness when competing for bandwidth, with Reno's aggressive congestion window increase dominating over Vegas' more moderate approach. This unfairness limits the deployment of TCP Vegas in real-world networks despite its stability advantages.

#### Algorithm Modification
The implementation introduces TCP Vegas+, which modifies the original TCP Vegas to better compete with TCP Reno while maintaining its core benefits:

1. **Dual-Mode Operation**:
   - **Moderate Mode**: Identical to original TCP Vegas behavior with controlled window size adjustments based on RTT estimates
   - **Aggressive Mode**: Behaves like TCP Reno with more aggressive window size increases to compete fairly

2. **Mode Switching Logic**:
   - Switches to aggressive mode when RTT increases while window size remains unchanged, indicating competition from aggressive flows
   - Reverts to moderate mode when packet loss is detected, indicating network congestion

3. **Implementation Details**:
   - Added a counter to track consecutive RTT increases to determine when to switch modes
   - Implemented threshold-based switching (countmax = 5 in experiments)
   - Modified congestion avoidance algorithms to support both operational modes

#### Modified Files
The following files were modified to implement TCP Vegas+:
1. `tcp-vegas.cc` - Core Vegas algorithm modifications
2. `tcp-socket-base.cc` - Socket handling modifications
3. `tcp-socket-base.h` - Interface declarations
4. `tcp-socket-state.h` - State tracking additions

#### Testing Methodology

1. **Network Topology**:
   - Custom topology with variable number of nodes
   - Point-to-point links with controlled bandwidth and delay

2. **Varied Parameters**:
   - Number of flows
   - Mix of Vegas+ and Reno flows

3. **Evaluation Metrics**:
   - Network throughput
   - Fairness index
   - Packet drop ratio

#### Experiment Results

1. **Throughput Analysis**:
   - Overall network throughput remained largely unchanged
   - Individual Vegas+ flows achieved improved throughput when competing with Reno

2. **Fairness Improvements**:
   - Modification caused a measurable increase in fairness index
   - Vegas+ flows obtained a more equitable share of bandwidth when competing with Reno

3. **Trade-offs**:
   - Slight increase in packet drop ratio due to more aggressive behavior
   - Despite increased drops, more packets were successfully delivered overall

4. **Key Findings**:
   - Aggressive behavior largely depends on the countmax threshold setting
   - Performance improvement would be more significant in wired medium (vs. wireless)
   - The modification successfully addresses the fairness issue while maintaining Vegas' core benefits

### Key Parameters:
- Configurable packet size (default: 1024 bytes)
- Adjustable data rates (application and physical layers)
- Simulation duration settings
- Queue size management (100000 packet buffer)
- Multiple flow configurations (default: 5 flows)
- TCP socket parameters and congestion window monitoring
- countmax threshold for Vegas+ mode switching (default: 5)

## Packet Tracer: Network Configuration

This directory contains Cisco Packet Tracer simulation files for network design and configuration exercises.

### Contents:
- **Packet Tracer [Online]/**: Online lab exercises
  - `A1.pka`, `A2.pka`, `B1.pka`, `B2.pka`: Lab activity files
  - `PT-Practice.pdf`: Practice guide
  - `Router-Config.pdf`: Router configuration guide
- **1705069.pkt**: Student implementation file

### Features:
- Network topology design
- Router and switch configuration
- Subnetting and IP addressing
- Routing protocols implementation

## Getting Started

### Prerequisites
- Java Development Kit (JDK) for File Server
- NS3 simulator for network simulations
- Cisco Packet Tracer for network configuration tasks

### Running the File Server
1. Navigate to the File Server directory
2. Compile the Java files
3. Run the Server application first: `java Server.Server`
4. Run the Client application to connect to the server: `java Client.Main`

### Running NS3 Simulations
1. Install NS3 following the instructions in the provided documentation
2. Navigate to the respective task directory
3. Run the simulation scripts:
   ```
   ./waf --run "scratch/taskA_1"
   ./waf --run "scratch/taskA_2"
   ./waf --run "scratch/taskB"
   ```
4. Analyze the generated output files and graphs

### Opening Packet Tracer Files
1. Install Cisco Packet Tracer
2. Open the .pkt or .pka files using the application

## Documentation

Detailed documentation can be found in the respective assignment PDFs and report files within each directory.

The `NS3_Report.pdf` contains comprehensive analysis of the network simulations conducted using NS3, including performance comparisons between TCP Reno and TCP Vegas implementations. 