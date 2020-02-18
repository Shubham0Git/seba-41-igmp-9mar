package org.opencord.igmpproxy;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.Ethernet;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.flow.FlowRuleServiceAdapter;
import org.onosproject.net.flowobjective.FlowObjectiveServiceAdapter;
import org.opencord.igmpproxy.IgmpManagerBase.MockCfgService;

public class IgmpStatisticsTest extends IgmpManagerBase {

    private static final int WAIT_TIMEOUT = 2000;

    private IgmpManager igmpManager;

    private IgmpStatisticsManager igmpStatisticsManager;

    // Set up the IGMP application.
    @Before
    public void setUp() {
        igmpManager = new IgmpManager();
        igmpManager.coreService = new CoreServiceAdapter();
        igmpManager.mastershipService = new MockMastershipService();
        igmpManager.flowObjectiveService = new FlowObjectiveServiceAdapter();
        igmpManager.deviceService = new MockDeviceService();
        igmpManager.packetService = new MockPacketService();
        igmpManager.flowRuleService = new FlowRuleServiceAdapter();
        igmpManager.multicastService = new TestMulticastRouteService();
        igmpManager.sadisService = new MockSadisService();
        igmpStatisticsManager = new IgmpStatisticsManager();
        igmpStatisticsManager.cfgService = new MockCfgService();
        TestUtils.setField(igmpStatisticsManager, "eventDispatcher", new TestEventDispatcher());
        igmpStatisticsManager.activate();
        igmpManager.igmpStatisticsManager = this.igmpStatisticsManager;
        // By default - we send query messages
        SingleStateMachine.sendQuery = true;
    }

    // Tear Down the IGMP application.
    @After
    public void tearDown() {
        igmpManager.deactivate();
        IgmpManager.groupMemberMap.clear();
        StateMachine.clearMap();
    }

    //Test Igmp Statistics.
    @Test
    public void testIgmpStatistics() throws InterruptedException {
        igmpManager.networkConfig = new TestNetworkConfigRegistry(false);
        igmpManager.activate();
        //IGMPv3 Join
        Ethernet igmpv3MembershipReportPkt = IgmpSender.getInstance().buildIgmpV3Join(GROUP_IP, SOURCE_IP_OF_A);
        sendPacket(igmpv3MembershipReportPkt, true);
        synchronized (savedPackets) {
            savedPackets.wait(WAIT_TIMEOUT);
        }
        //Leave
        Ethernet igmpv3LeavePkt = IgmpSender.getInstance().buildIgmpV3Leave(GROUP_IP, SOURCE_IP_OF_A);
        sendPacket(igmpv3LeavePkt, true);
        synchronized (savedPackets) {
            savedPackets.wait(WAIT_TIMEOUT);
        }

        assertEquals((long) 2, igmpStatisticsManager.getIgmpStats().getTotalMsgReceived().longValue());
        assertEquals((long) 1, igmpStatisticsManager.getIgmpStats().getIgmpJoinReq().longValue());
        assertEquals((long) 2, igmpStatisticsManager.getIgmpStats().getIgmpv3MembershipReport().longValue());
        assertEquals((long) 1, igmpStatisticsManager.getIgmpStats().getIgmpSuccessJoinRejoinReq().longValue());

        assertEquals((long) 1, igmpStatisticsManager.getIgmpStats().getIgmpLeaveReq().longValue());
        assertEquals((long) 2, igmpStatisticsManager.getIgmpStats().getIgmpMsgReceived().longValue());

    }

}
