#include "ns3/core-module.h"
#include "ns3/internet-module.h"
#include "ns3/network-module.h"
#include "ns3/point-to-point-module.h"
#include <fstream>

// added
#include "ns3/applications-module.h"
#include "ns3/core-module.h"
#include "ns3/data-rate.h"
#include "ns3/internet-module.h"
#include "ns3/mobility-module.h"
#include "ns3/network-module.h"
#include "ns3/object.h"
#include "ns3/point-to-point-module.h"
#include "ns3/ptr.h"
#include "ns3/socket.h"
#include "ns3/ssid.h"
#include "ns3/stats-module.h"

// for flow monitor
#include "ns3/flow-monitor-helper.h"
#include "ns3/flow-monitor-module.h"
#include "ns3/flow-monitor.h"
#include "ns3/tcp-socket-factory.h"
#include "ns3/wifi-helper.h"
#include "ns3/wifi-mac-helper.h"
#include "ns3/yans-wifi-helper.h"
#include <cstdlib>
#include <ostream>
#include <string>

using namespace ns3;

NS_LOG_COMPONENT_DEFINE("taskA");

#define QUEUE_SIZE 100000
#define NUM_FLOWS 5
#define N_PACKETS_PER_SECOND 400

class MyApp : public Application {
public:
  MyApp();
  virtual ~MyApp();

  /**
   * Register this type.
   * \return The TypeId.
   */
  static TypeId GetTypeId(void);
  void Setup(Ptr<Socket> socket, Address address, uint32_t packetSize,
             uint32_t nPackets, DataRate dataRate);

private:
  virtual void StartApplication(void);
  virtual void StopApplication(void);

  void ScheduleTx(void);
  void SendPacket(void);

  Ptr<Socket> m_socket;
  Address m_peer;
  uint32_t m_packetSize;
  uint32_t m_nPackets;
  DataRate m_dataRate;
  EventId m_sendEvent;
  bool m_running;
  uint32_t m_packetsSent;
};

MyApp::MyApp()
    : m_socket(0), m_peer(), m_packetSize(0), m_nPackets(0), m_dataRate(0),
      m_sendEvent(), m_running(false), m_packetsSent(0) {}

MyApp::~MyApp() { m_socket = 0; }

/* static */
TypeId MyApp::GetTypeId(void) {
  static TypeId tid = TypeId("MyApp")
                          .SetParent<Application>()
                          .SetGroupName("Tutorial")
                          .AddConstructor<MyApp>();
  return tid;
}

void MyApp::Setup(Ptr<Socket> socket, Address address, uint32_t packetSize,
                  uint32_t nPackets, DataRate dataRate) {
  m_socket = socket;
  m_peer = address;
  m_packetSize = packetSize;
  m_nPackets = nPackets;
  m_dataRate = dataRate;
}

void MyApp::StartApplication(void) {
  m_running = true;
  m_packetsSent = 0;
  m_socket->Bind();
  m_socket->Connect(m_peer);
  SendPacket();
}

void MyApp::StopApplication(void) {
  m_running = false;

  if (m_sendEvent.IsRunning()) {
    Simulator::Cancel(m_sendEvent);
  }

  if (m_socket) {
    m_socket->Close();
  }
}

void MyApp::SendPacket(void) {
  Ptr<Packet> packet = Create<Packet>(m_packetSize);
  m_socket->Send(packet);

  if (++m_packetsSent < m_nPackets) {
    ScheduleTx();
  }
}

void MyApp::ScheduleTx(void) {
  if (m_running) {
    Time tNext(Seconds(m_packetSize * 8 /
                       static_cast<double>(m_dataRate.GetBitRate())));
    m_sendEvent = Simulator::Schedule(tNext, &MyApp::SendPacket, this);
  }
}

static void CwndChange(Ptr<OutputStreamWrapper> stream, uint32_t oldCwnd,
                       uint32_t newCwnd) {
  // NS_LOG_UNCOND(Simulator::Now().GetSeconds() << "\t" << newCwnd);
  *stream->GetStream() << Simulator::Now().GetSeconds() << "\t" << newCwnd
                       << std::endl;
}

static void RxDrop(Ptr<PcapFileWrapper> file, Ptr<const Packet> p) {
  NS_LOG_UNCOND("RxDrop at " << Simulator::Now().GetSeconds());
  file->Write(Simulator::Now(), p);
}

int main(int argc, char *argv[]) {
  srand(0);

  uint32_t payloadSize = 1024;     /* Transport layer payload size in bytes. */
  std::string dataRate = "50Mbps"; /* Application layer datarate. */
  std::string phyRate = "HtMcs7";  /* Physical layer bitrate. */
  double simulationTime = 5;       /* Simulation time in seconds. */
  bool pcapTracing = true;         /* PCAP Tracing is enabled or not. */
  std::string prefix_file_name = "TaskA_1";

  /* Command line argument parser setup. */
  CommandLine cmd(__FILE__);
  cmd.AddValue("payloadSize", "Payload size in bytes", payloadSize);
  cmd.AddValue("dataRate", "Application data ate", dataRate);
  cmd.AddValue("phyRate", "Physical layer bitrate", phyRate);
  cmd.AddValue("simulationTime", "Simulation time in seconds", simulationTime);
  cmd.AddValue("pcap", "Enable/disable PCAP Tracing", pcapTracing);
  cmd.Parse(argc, argv);

  /* Configure TCP Options */
  Config::SetDefault("ns3::TcpSocket::SegmentSize", UintegerValue(payloadSize));

  NodeContainer routers;
  routers.Create(2);

  uint32_t rate = (8 * N_PACKETS_PER_SECOND * payloadSize) / 1048576;

  dataRate = std::to_string(rate) + "Mbps";

  NodeContainer wifiStaNodes1;
  wifiStaNodes1.Create(NUM_FLOWS);
  NodeContainer wifiApNode1 = routers.Get(0);

  NodeContainer wifiStaNodes2;
  wifiStaNodes2.Create(NUM_FLOWS);
  NodeContainer wifiApNode2 = routers.Get(1);

  // Create the point-to-point link helpers
  PointToPointHelper bottleneckLink;
  bottleneckLink.SetDeviceAttribute("DataRate", StringValue("20Mbps"));
  bottleneckLink.SetChannelAttribute("Delay", StringValue("5ms"));

  // NetDeviceContainer r1r2 = bottleneckLink.Install(routers.Get(0),
  // routers.Get(1));
  NetDeviceContainer r1r2 = bottleneckLink.Install(routers);

  WifiMacHelper wifiMac1, wifiMac2;
  WifiHelper wifiHelper1, wifiHelper2;
  wifiHelper1.SetStandard(WIFI_STANDARD_80211n_5GHZ);
  wifiHelper2.SetStandard(WIFI_STANDARD_80211n_5GHZ);

  /* Set up Legacy Channel */
  YansWifiChannelHelper wifiChannel1, wifiChannel2;
  wifiChannel1.SetPropagationDelay("ns3::ConstantSpeedPropagationDelayModel");
  wifiChannel1.AddPropagationLoss("ns3::FriisPropagationLossModel", "Frequency",
                                  DoubleValue(5e9));

  wifiChannel2.SetPropagationDelay("ns3::ConstantSpeedPropagationDelayModel");
  wifiChannel2.AddPropagationLoss("ns3::FriisPropagationLossModel", "Frequency",
                                  DoubleValue(5e9));

  /* Setup Physical Layer */
  YansWifiPhyHelper wifiPhy1, wifiPhy2;
  wifiPhy1.SetChannel(wifiChannel1.Create());
  wifiPhy1.SetErrorRateModel("ns3::YansErrorRateModel");
  wifiHelper1.SetRemoteStationManager("ns3::ConstantRateWifiManager",
                                      "DataMode", StringValue(phyRate));

  wifiPhy2.SetChannel(wifiChannel2.Create());
  wifiPhy2.SetErrorRateModel("ns3::YansErrorRateModel");
  wifiHelper2.SetRemoteStationManager("ns3::ConstantRateWifiManager",
                                      "DataMode", StringValue(phyRate));

  /* Configure AP */
  Ssid ssid1 = Ssid("network1");
  wifiMac1.SetType("ns3::ApWifiMac", "Ssid", SsidValue(ssid1));
  Ssid ssid2 = Ssid("network2");
  wifiMac2.SetType("ns3::ApWifiMac", "Ssid", SsidValue(ssid2));

  NetDeviceContainer apDevices1, apDevices2;
  apDevices1 = wifiHelper1.Install(wifiPhy1, wifiMac1, wifiApNode1);
  apDevices2 = wifiHelper2.Install(wifiPhy2, wifiMac2, wifiApNode2);

  /* Configure STA */
  wifiMac1.SetType("ns3::StaWifiMac", "Ssid", SsidValue(ssid1));
  wifiMac2.SetType("ns3::StaWifiMac", "Ssid", SsidValue(ssid2));

  NetDeviceContainer staDevices1, staDevices2;
  staDevices1 = wifiHelper1.Install(wifiPhy1, wifiMac1, wifiStaNodes1);
  staDevices2 = wifiHelper2.Install(wifiPhy2, wifiMac2, wifiStaNodes2);

  /* Mobility model */

  // Configure mobility
  MobilityHelper mobility1, mobility2;

  mobility1.SetPositionAllocator(
      "ns3::GridPositionAllocator", "MinX", DoubleValue(0.0), "MinY",
      DoubleValue(0.0), "DeltaX", DoubleValue(5.0), "DeltaY", DoubleValue(10.0),
      "GridWidth", UintegerValue(3), "LayoutType", StringValue("RowFirst"));

  mobility1.SetMobilityModel(
      "ns3::RandomWalk2dMobilityModel", "Speed",
      StringValue("ns3::ConstantRandomVariable[Constant=2.0]"), "Bounds",
      RectangleValue(Rectangle(-50, 50, -50, 50)));
  mobility1.Install(wifiStaNodes1);

  mobility1.SetMobilityModel("ns3::ConstantPositionMobilityModel");
  mobility1.Install(wifiApNode1);

  mobility2.SetPositionAllocator(
      "ns3::GridPositionAllocator", "MinX", DoubleValue(0.0), "MinY",
      DoubleValue(0.0), "DeltaX", DoubleValue(5.0), "DeltaY", DoubleValue(10.0),
      "GridWidth", UintegerValue(3), "LayoutType", StringValue("RowFirst"));

  mobility2.SetMobilityModel(
      "ns3::RandomWalk2dMobilityModel", "Speed",
      StringValue("ns3::ConstantRandomVariable[Constant=2.0]"), "Bounds",
      RectangleValue(Rectangle(-50, 50, -50, 50)));
  mobility2.Install(wifiStaNodes2);

  mobility2.SetMobilityModel("ns3::ConstantPositionMobilityModel");
  mobility2.Install(wifiApNode2);

  /* Internet stack */
  InternetStackHelper stack;
  stack.Install(wifiApNode1);
  stack.Install(wifiStaNodes1);
  stack.Install(wifiApNode2);
  stack.Install(wifiStaNodes2);

  Ipv4AddressHelper address;

  address.SetBase("10.1.1.0", "255.255.255.0");
  Ipv4InterfaceContainer p2pInterfaces;
  p2pInterfaces = address.Assign(r1r2);

  address.SetBase("10.1.2.0", "255.255.255.0");
  Ipv4InterfaceContainer staInterface1 = address.Assign(staDevices1);
  Ipv4InterfaceContainer apInterface1 = address.Assign(apDevices1);

  address.SetBase("10.1.3.0", "255.255.255.0");
  Ipv4InterfaceContainer staInterface2 = address.Assign(staDevices2);
  Ipv4InterfaceContainer apInterface2 = address.Assign(apDevices2);

  /* Populate routing table */
  Ipv4GlobalRoutingHelper::PopulateRoutingTables();

  for (int i = 0; i < NUM_FLOWS; i++) {
    /* Install TCP Receiver on the access point */
    uint16_t sinkPort = 8080;
    PacketSinkHelper sinkHelper(
        "ns3::TcpSocketFactory",
        InetSocketAddress(Ipv4Address::GetAny(), sinkPort));
    ApplicationContainer sinkApp = sinkHelper.Install(wifiStaNodes2.Get(i));

    Address sinkAddress(
        InetSocketAddress(staInterface2.GetAddress(i), sinkPort));

    sinkApp.Start(Seconds(0.));
    sinkApp.Stop(Seconds(simulationTime));

    // Set Congestion Algorithm Randomly
    std::stringstream nodeId;
    nodeId << wifiStaNodes1.Get(i)->GetId();
    std::string tcp_variant1 = "ns3::TcpNewReno";
    std::string tcp_variant2 = "ns3::TcpVegas";
    uint32_t choice = rand() % 2;

    TypeId tid =
        TypeId::LookupByName(choice == 0 ? tcp_variant1 : tcp_variant2);

    std::string specificNode =
        "/NodeList/" + nodeId.str() + "/$ns3::TcpL4Protocol/SocketType";
    Config::Set(specificNode, TypeIdValue(tid));

    Ptr<Socket> tcpSocket = Socket::CreateSocket(wifiStaNodes1.Get(i),
                                                 TcpSocketFactory::GetTypeId());
    Ptr<MyApp> app = CreateObject<MyApp>();
    app->Setup(tcpSocket, sinkAddress, payloadSize, QUEUE_SIZE,
               DataRate(dataRate));

    wifiStaNodes1.Get(i)->AddApplication(app);
    app->SetStartTime(Seconds(0));
    app->SetStopTime(Seconds(simulationTime));

    AsciiTraceHelper asciiTraceHelper;
    Ptr<OutputStreamWrapper> stream =
        asciiTraceHelper.CreateFileStream("taskA_1_" + nodeId.str() + ".cwnd");
    tcpSocket->TraceConnectWithoutContext(
        "CongestionWindow", MakeBoundCallback(&CwndChange, stream));

    PcapHelper pcapHelper;
    Ptr<PcapFileWrapper> file =
        pcapHelper.CreateFile("taskA_1_" + nodeId.str() + ".pcap",
                              std::ios::out, PcapHelper::DLT_PPP);
    staDevices2.Get(1)->TraceConnectWithoutContext(
        "PhyRxDrop", MakeBoundCallback(&RxDrop, file));

    // DEBUG -- Start
    std::cout << nodeId.str() << " " << choice << " "
              << staInterface1.GetAddress(i) << " "
              << tcpSocket->GetSocketType() << std::endl;

    // DEBUG -- Start
  }

  wifiPhy1.SetPcapDataLinkType(WifiPhyHelper::DLT_IEEE802_11_RADIO);
  wifiPhy1.EnablePcap("AccessPoint", apDevices1);
  wifiPhy1.EnablePcap("Station", staDevices1);

  std::ofstream ascii;
  Ptr<OutputStreamWrapper> ascii_wrap;
  ascii.open((prefix_file_name + "-ascii").c_str());
  ascii_wrap = new OutputStreamWrapper((prefix_file_name + "-ascii").c_str(),
                                       std::ios::out);
  stack.EnableAsciiIpv4All(ascii_wrap);

  FlowMonitorHelper flowmon;
  Ptr<FlowMonitor> monitor = flowmon.InstallAll();
  //   Simulator::Schedule(Seconds(0.01), &TraceThroughput, monitor);

  /* Start Simulation */
  Simulator::Stop(Seconds(simulationTime + 1));

  // Create a new directory to store the output of the program
  std::string dir = "Task-A-results/";
  std::string dirToSave = "mkdir -p " + dir;
  if (system(dirToSave.c_str()) == -1) {
    exit(1);
  }

  Simulator::Run();

  flowmon.SerializeToXmlFile(dir + "my-tcp.flowmonitor", false, false);

  // variables for output measurement
  int j = 0;
  float AvgThroughput = 0;
  float Throughput = 0;
  uint32_t SentPackets = 0;
  uint32_t ReceivedPackets = 0;
  uint32_t DropPackets = 0;
  uint32_t Sent = 0;
  uint32_t Received = 0;
  uint32_t Drop = 0;
  Time AvgDelay;

  Ptr<Ipv4FlowClassifier> classifier =
      DynamicCast<Ipv4FlowClassifier>(flowmon.GetClassifier());
  FlowMonitor::FlowStatsContainer stats = monitor->GetFlowStats();

  std::cout << stats.size() << std::endl;

  for (auto iter = stats.begin(); iter != stats.end(); ++iter) {

    Ipv4FlowClassifier::FiveTuple t = classifier->FindFlow(iter->first);

    // classifier returns FiveTuple in correspondance to a flowID

    std::cout << "----Flow ID = " << iter->first << std::endl;
    std::cout << "Src Addr = " << t.sourceAddress
              << " -- Dst Addr = " << t.destinationAddress << std::endl;
    Sent = iter->second.txPackets;
    std::cout << "Sent Packets = " << Sent << std::endl;
    Received = iter->second.rxPackets;
    std::cout << "Received Packets = " << Received << std::endl;
    Drop = iter->second.lostPackets;
    std::cout << "Drop Packets = " << Drop << std::endl;
    std::cout << "Packet delivery ratio = " << Received * 100.0 / Sent << "%"
              << std::endl;
    std::cout << "Packet drop ratio = " << Drop * 100.0 / Sent << "%"
              << std::endl;
    Throughput = iter->second.rxBytes * 8.0 /
                 ((iter->second.timeLastRxPacket.GetSeconds() -
                   iter->second.timeFirstTxPacket.GetSeconds()) *
                  1024);
    Throughput = Throughput == Throughput ? Throughput : 0;
    std::cout << "Throughput = " << Throughput << "Kbps" << std::endl;

    SentPackets = SentPackets + Sent;
    ReceivedPackets = ReceivedPackets + Received;
    DropPackets = DropPackets + Drop;
    AvgThroughput = AvgThroughput + Throughput;
    AvgDelay = AvgDelay + iter->second.delaySum;

    j = j + 1;

    std::cout << std::endl;
  }

  AvgThroughput = AvgThroughput / j;
  AvgDelay = AvgDelay / ReceivedPackets;
  std::cout << "--------Total Results of the simulation----------" << std::endl;
  std::cout << "Total Sent Packets  = " << SentPackets << std::endl;
  std::cout << "Total Received Packets = " << ReceivedPackets << std::endl;
  std::cout << "Total Drop Packets = " << DropPackets << std::endl;
  std::cout << "Packet Drop Ratio = " << ((DropPackets * 100.00) / SentPackets)
            << "%" << std::endl;
  std::cout << "Packet Delivery Ratio = "
            << ((ReceivedPackets * 100.00) / SentPackets) << "%" << std::endl;
  std::cout << "Network Throughput = " << AvgThroughput << " Kbps" << std::endl;
  std::cout << "End To End Delay =" << AvgDelay << std::endl;
  std::cout << "Total Flow id = " << j << std::endl;
  Simulator::Destroy();
}