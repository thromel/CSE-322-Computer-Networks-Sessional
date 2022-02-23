#include "ns3/core-module.h"
#include "ns3/internet-module.h"
#include "ns3/network-module.h"
#include "ns3/point-to-point-module.h"
#include <fstream>

// added
#include "ns3/address.h"
#include "ns3/applications-module.h"
#include "ns3/core-module.h"
#include "ns3/data-rate.h"
#include "ns3/inet6-socket-address.h"
#include "ns3/internet-module.h"
#include "ns3/lr-wpan-helper.h"
#include "ns3/mobility-helper.h"
#include "ns3/mobility-module.h"
#include "ns3/net-device-container.h"
#include "ns3/network-module.h"
#include "ns3/node-container.h"
#include "ns3/object.h"
#include "ns3/point-to-point-module.h"
#include "ns3/ptr.h"
#include "ns3/sixlowpan-helper.h"
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

NS_LOG_COMPONENT_DEFINE("taskA_2");

#define QUEUE_SIZE 100000
#define NUM_FLOWS 5
#define N_PACKETS_PER_SECOND 10

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

// static void CwndChange(Ptr<OutputStreamWrapper> stream, uint32_t oldCwnd,
//                        uint32_t newCwnd) {
//   NS_LOG_UNCOND(Simulator::Now().GetSeconds() << "\t" << newCwnd);
//   *stream->GetStream() << Simulator::Now().GetSeconds() << "\t" << newCwnd
//                        << std::endl;
// }

int main(int argc, char *argv[]) {
  srand(0);
  uint32_t nWsnN1 = NUM_FLOWS;
  uint32_t nWsnN2 = NUM_FLOWS;
  uint32_t payloadSize = 1024;     /* Transport layer payload size in bytes. */
  std::string dataRate = "50Mbps"; /* Application layer datarate. */
  std::string phyRate = "HtMcs7";  /* Physical layer bitrate. */
  double simulationTime = 10210;   /* Simulation time in seconds. */
  bool pcapTracing = true;         /* PCAP Tracing is enabled or not. */
  std::string prefix_file_name = "TaskA_2";
  Packet::EnablePrinting();

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

  uint32_t rate = (8 * N_PACKETS_PER_SECOND * payloadSize) / 1048576;
  dataRate = std::to_string(rate) + "Mbps";

  NodeContainer wNodes1;
  wNodes1.Create(nWsnN1);

  MobilityHelper mobility1;
  mobility1.SetPositionAllocator(
      "ns3::GridPositionAllocator", "MinX", DoubleValue(0.0), "MinY",
      DoubleValue(0.0), "DeltaX", DoubleValue(5.0), "DeltaY", DoubleValue(10.0),
      "GridWidth", UintegerValue(3), "LayoutType", StringValue("RowFirst"));
  mobility1.SetMobilityModel(
      "ns3::RandomWalk2dMobilityModel", "Speed",
      StringValue("ns3::ConstantRandomVariable[Constant=20.0]"), "Bounds",
      RectangleValue(Rectangle(-100, 100, -100, 100)));
  mobility1.Install(wNodes1);

  LrWpanHelper lrWpanHelper1;
  NetDeviceContainer lrWpanDevices1 = lrWpanHelper1.Install(wNodes1);

  // Fake PAN association and short address assignment.
  // This is needed because the lr-wpan module does not provide (yet)
  // a full PAN association procedure.
  lrWpanHelper1.AssociateToPan(lrWpanDevices1, 0);

  NodeContainer wNodes2;
  wNodes2.Add(wNodes1.Get(0));
  wNodes2.Create(nWsnN2);

  MobilityHelper mobility2;
  mobility2.SetPositionAllocator(
      "ns3::GridPositionAllocator", "MinX", DoubleValue(0.0), "MinY",
      DoubleValue(0.0), "DeltaX", DoubleValue(5.0), "DeltaY", DoubleValue(10.0),
      "GridWidth", UintegerValue(3), "LayoutType", StringValue("RowFirst"));
  mobility2.SetMobilityModel(
      "ns3::RandomWalk2dMobilityModel", "Speed",
      StringValue("ns3::ConstantRandomVariable[Constant=20.0]"), "Bounds",
      RectangleValue(Rectangle(-100, 100, -100, 100)));
  mobility2.Install(wNodes2);

  LrWpanHelper lrWpanHelper2;
  NetDeviceContainer lrWpanDevices2 = lrWpanHelper2.Install(wNodes2);

  // Fake PAN association and short address assignment.
  // This is needed because the lr-wpan module does not provide (yet)
  // a full PAN association procedure.
  lrWpanHelper2.AssociateToPan(lrWpanDevices2, 0);

  InternetStackHelper internetv6;
  internetv6.InstallAll();

  SixLowPanHelper sixLowPanHelper1;
  NetDeviceContainer sixLowPanDevices1 =
      sixLowPanHelper1.Install(lrWpanDevices1);

  SixLowPanHelper sixLowPanHelper2;
  NetDeviceContainer sixLowPanDevices2 =
      sixLowPanHelper2.Install(lrWpanDevices2);

  Ipv6AddressHelper ipv6;
  ipv6.SetBase(Ipv6Address("2001:cafe::"), Ipv6Prefix(64));
  Ipv6InterfaceContainer wsnDeviceInterfaces1;
  wsnDeviceInterfaces1 = ipv6.Assign(sixLowPanDevices1);
  wsnDeviceInterfaces1.SetForwarding(0, true);
  wsnDeviceInterfaces1.SetDefaultRouteInAllNodes(0);

  ipv6.SetBase(Ipv6Address("2001:f00d::"), Ipv6Prefix(64));
  Ipv6InterfaceContainer wsnDeviceInterfaces2;
  wsnDeviceInterfaces2 = ipv6.Assign(sixLowPanDevices2);
  wsnDeviceInterfaces2.SetForwarding(0, true);
  wsnDeviceInterfaces2.SetDefaultRouteInAllNodes(0);

  for (uint32_t i = 0; i < sixLowPanDevices1.GetN(); i++) {
    Ptr<NetDevice> dev = sixLowPanDevices1.Get(i);
    dev->SetAttribute("UseMeshUnder", BooleanValue(true));
    dev->SetAttribute("MeshUnderRadius", UintegerValue(10));
  }

  for (uint32_t i = 0; i < sixLowPanDevices2.GetN(); i++) {
    Ptr<NetDevice> dev = sixLowPanDevices2.Get(i);
    dev->SetAttribute("UseMeshUnder", BooleanValue(true));
    dev->SetAttribute("MeshUnderRadius", UintegerValue(10));
  }

  for (int i = 0; i < NUM_FLOWS / 2; i++) {
    /* Install TCP Receiver on the access point */
    uint16_t sinkPort = 8080;
    PacketSinkHelper sinkHelper(
        "ns3::TcpSocketFactory",
        Inet6SocketAddress(Ipv6Address::GetAny(), sinkPort));
    ApplicationContainer sinkApp = sinkHelper.Install(wNodes2.Get(nWsnN2 - i));

    sinkApp.Start(Seconds(0.));
    sinkApp.Stop(Seconds(simulationTime));

    Ptr<Socket> tcpSocket = Socket::CreateSocket(wNodes1.Get(nWsnN1 - i - 1),
                                                 TcpSocketFactory::GetTypeId());

    Address sinkAddress = Inet6SocketAddress(
        wsnDeviceInterfaces2.GetAddress(nWsnN2 - i - 1, 1), sinkPort);
    Ptr<MyApp> app = CreateObject<MyApp>();

    app->Setup(tcpSocket, sinkAddress, payloadSize, QUEUE_SIZE,
               DataRate(dataRate));

    wNodes1.Get(nWsnN1 - i - 1)->AddApplication(app);
    app->SetStartTime(Seconds(0));
    app->SetStopTime(Seconds(simulationTime - 1));

    std::stringstream nodeId;
    nodeId << wNodes1.Get(nWsnN1 - i - 1)->GetId();

    std::cout << nWsnN1 - i - 1 << std::endl;

    std::string tcp_variant1 = "ns3::TcpVegas";
    std::string tcp_variant2 = "ns3::TcpNewReno";
    uint32_t choice = rand() % 2;

    TypeId tid =
        TypeId::LookupByName(choice == 0 ? tcp_variant1 : tcp_variant2);

    std::string specificNode =
        "/NodeList/" + nodeId.str() + "/$ns3::TcpL4Protocol/SocketType";
    Config::Set(specificNode, TypeIdValue(tid));

    std::cout << "--------------------4------------------" << std::endl;

    // // DEBUG -- Start
    // std::cout << nodeId.str() << " " << choice << " "
    //           << wsnDeviceInterfaces1.GetAddress(nWsnN1 - i, 1) << " "
    //           << tcpSocket->GetSocketType() << std::endl;
    // // DEBUG -- Start
  }

  FlowMonitorHelper flowmon;
  Ptr<FlowMonitor> monitor = flowmon.InstallAll();
  //   Simulator::Schedule(Seconds(0.01), &TraceThroughput, monitor);

  /* Start Simulation */
  Simulator::Stop(Seconds(simulationTime + 1));

  std::cout << "--------------------3------------------" << std::endl;

  // Create a new directory to store the output of the program
  std::string dir = "Task-A_2-results/";
  std::string dirToSave = "mkdir -p " + dir;
  if (system(dirToSave.c_str()) == -1) {
    exit(1);
  }

  Simulator::Run();

  flowmon.SerializeToXmlFile(dir + "wlowpan-tcp.flowmonitor", false, false);

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

  Ptr<Ipv6FlowClassifier> classifier =
      DynamicCast<Ipv6FlowClassifier>(flowmon.GetClassifier6());
  FlowMonitor::FlowStatsContainer stats = monitor->GetFlowStats();

  std::cout << stats.size() << std::endl;

  for (auto iter = stats.begin(); iter != stats.end(); ++iter) {

    Ipv6FlowClassifier::FiveTuple t = classifier->FindFlow(iter->first);

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