package org.jgroups.tests;


import org.jgroups.*;
import org.jgroups.protocols.DUPL;
import org.jgroups.protocols.pbcast.NAKACK;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Tuple;
import org.jgroups.util.Util;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tests whether UNICAST or NAKACK prevent delivery of duplicate messages. JGroups guarantees that a message is
 * delivered once and only once. The test inserts DUPL below both UNICAST and NAKACK and makes it duplicate (1)
 * unicast, (2) multicast, (3) regular and (4) OOB messages. The receiver(s) then check for the presence of duplicate
 * messages. 
 * @author Bela Ban
 * @version $Id: DuplicateTest.java,v 1.13 2009/11/13 15:01:09 belaban Exp $
 */
@Test(groups=Global.STACK_DEPENDENT,sequential=true)
public class DuplicateTest extends ChannelTestBase {
    private JChannel c1, c2, c3;
    protected Address a1, a2, a3;
    private MyReceiver r1, r2, r3;

    @BeforeClass
    void classInit() throws Exception {
        createChannels(true, true, (short)2, (short)2);
        c1.setName("C1"); c2.setName("C2"); c3.setName("C3");
        a1=c1.getAddress();
        a2=c2.getAddress();
        a3=c3.getAddress();
    }

    @BeforeMethod
    void init() throws Exception {
        r1=new MyReceiver("C1");
        r2=new MyReceiver("C2");
        r3=new MyReceiver("C3");
        c1.setReceiver(r1);
        c2.setReceiver(r2);
        c3.setReceiver(r3);
    }

    @AfterClass
    void tearDown() throws Exception {
        Util.close(c3, c2, c1);
    }



    public void testRegularUnicastsToSelf() throws Exception {
        send(c1, c1.getAddress(), false, 10);
        sendStableMessages(c1,c2, c3);
        check(r1, 1, false, new Tuple<Address,Integer>(a1, 10));
    }

    public void testOOBUnicastsToSelf() throws Exception {
        send(c1, c1.getAddress(), true, 10);
        sendStableMessages(c1,c2,c3);
        check(r1, 1, true, new Tuple<Address,Integer>(a1, 10));
    }

    public void testRegularUnicastsToOthers() throws Exception {
        send(c1, c2.getAddress(), false, 10);
        send(c1, c3.getAddress(), false, 10);
        sendStableMessages(c1,c2,c3);
        check(r2, 1, false, new Tuple<Address,Integer>(a1, 10));
        check(r3, 1, false, new Tuple<Address,Integer>(a1, 10));
    }

    public void testOOBUnicastsToOthers() throws Exception {
        send(c1, c2.getAddress(), true, 10);
        send(c1, c3.getAddress(), true, 10);
        sendStableMessages(c1,c2,c3);
        check(r2, 1, true, new Tuple<Address,Integer>(a1, 10));
        check(r3, 1, true, new Tuple<Address,Integer>(a1, 10));
    }


    public void testRegularMulticastToAll() throws Exception {
        send(c1, null /** multicast */, false, 10);
        sendStableMessages(c1,c2,c3);
        check(r1, 1, false, new Tuple<Address,Integer>(a1, 10));
        check(r2, 1, false, new Tuple<Address,Integer>(a1, 10));
        check(r3, 1, false, new Tuple<Address,Integer>(a1, 10));
    }


    public void testOOBMulticastToAll() throws Exception {
        send(c1, null /** multicast */, true, 10);
        sendStableMessages(c1,c2,c3);
        check(r1, 1, true, new Tuple<Address,Integer>(a1, 10));
        check(r2, 1, true, new Tuple<Address,Integer>(a1, 10));
        check(r3, 1, true, new Tuple<Address,Integer>(a1, 10));
    }


    public void testRegularMulticastToAll3Senders() throws Exception {
        send(c1, null /** multicast */, false, 10);
        send(c2, null /** multicast */, false, 10);
        send(c3, null /** multicast */, false, 10);
        sendStableMessages(c1,c2,c3);
        check(r1, 3, false, new Tuple<Address,Integer>(a1, 10), new Tuple<Address,Integer>(a2, 10), new Tuple<Address,Integer>(a3, 10));
        check(r2, 3, false, new Tuple<Address,Integer>(a1, 10), new Tuple<Address,Integer>(a2, 10), new Tuple<Address,Integer>(a3, 10));
        check(r3, 3, false, new Tuple<Address,Integer>(a1, 10), new Tuple<Address,Integer>(a2, 10), new Tuple<Address,Integer>(a3, 10));
    }

    public void testOOBMulticastToAll3Senders() throws Exception {
        send(c1, null /** multicast */, true, 10);
        send(c2, null /** multicast */, true, 10);
        send(c3, null /** multicast */, true, 10);
        sendStableMessages(c1,c2,c3);
        check(r1, 3, true, new Tuple<Address,Integer>(a1, 10), new Tuple<Address,Integer>(a2, 10), new Tuple<Address,Integer>(a3, 10));
        check(r2, 3, true, new Tuple<Address,Integer>(a1, 10), new Tuple<Address,Integer>(a2, 10), new Tuple<Address,Integer>(a3, 10));
        check(r3, 3, true, new Tuple<Address,Integer>(a1, 10), new Tuple<Address,Integer>(a2, 10), new Tuple<Address,Integer>(a3, 10));
    }

    public void testMixedMulticastsToAll3Members() throws Exception {
        send(c1, null /** multicast */, false, true, 10);
        send(c2, null /** multicast */, false, true, 10);
        send(c3, null /** multicast */, false, true, 10);
        sendStableMessages(c1,c2,c3);
        check(r1, 3, true, new Tuple<Address,Integer>(a1, 10), new Tuple<Address,Integer>(a2, 10), new Tuple<Address,Integer>(a3, 10));
        check(r2, 3, true, new Tuple<Address,Integer>(a1, 10), new Tuple<Address,Integer>(a2, 10), new Tuple<Address,Integer>(a3, 10));
        check(r3, 3, true, new Tuple<Address,Integer>(a1, 10), new Tuple<Address,Integer>(a2, 10), new Tuple<Address,Integer>(a3, 10));
    }


     private static void send(Channel sender_channel, Address dest, boolean oob, int num_msgs) throws Exception {
         send(sender_channel, dest, oob, false, num_msgs);
     }

     private static void send(Channel sender_channel, Address dest, boolean oob, boolean mixed, int num_msgs) throws Exception {
         long seqno=1;
         for(int i=0; i < num_msgs; i++) {
             Message msg=new Message(dest, null, seqno++);
             if(mixed) {
                 if(i % 2 == 0)
                     msg.setFlag(Message.OOB);
             }
             else if(oob) {
                 msg.setFlag(Message.OOB);
             }

             sender_channel.send(msg);
         }
     }


    private static void sendStableMessages(JChannel ... channels) {
        for(JChannel ch: channels) {
            STABLE stable=(STABLE)ch.getProtocolStack().findProtocol(STABLE.class);
            if(stable != null)
                stable.runMessageGarbageCollection();
        }
    }


    private void createChannels(boolean copy_multicasts, boolean copy_unicasts, int num_outgoing_copies, int num_incoming_copies) throws Exception {
        c1=createChannel(true, 3);
        DUPL dupl=new DUPL(copy_multicasts, copy_unicasts, num_incoming_copies, num_outgoing_copies);
        ProtocolStack stack=c1.getProtocolStack();
        stack.insertProtocol(dupl, ProtocolStack.BELOW, NAKACK.class);

        c2=createChannel(c1);
        c3=createChannel(c1);

        c1.connect("DuplicateTest");
        c2.connect("DuplicateTest");
        c3.connect("DuplicateTest");

        assert c3.getView().size() == 3 : "view was " + c1.getView() + " but should have been 3";
    }


    private static void check(MyReceiver receiver, int expected_size, boolean oob, Tuple<Address,Integer>... vals) {
        Map<Address, List<Long>> msgs=receiver.getMsgs();

        for(int i=0; i < 10; i++) {
            if(msgs.size() == expected_size)
                break;
            Util.sleep(500);
        }
        assert msgs.size() == expected_size : "expected size=" + expected_size + ", msgs: " + msgs.keySet();


        for(Tuple<Address,Integer> tuple: vals) {
            Address addr=tuple.getVal1();
            List<Long> list=msgs.get(addr);
            assert list != null : "no list available for " + addr;

            int expected_values=tuple.getVal2();
            for(int i=0; i < 10; i++) {
                if(list.size() == expected_values)
                    break;
                Util.sleep(500);
            }

            System.out.println("[" + receiver.getName() + "]: " + addr + ": " + list);
            assert list.size() == expected_values : addr + "'s list's size is not " + tuple.getVal2() +", list: " + list;
            if(!oob) // if OOB messages, ordering is not guaranteed
                check(addr, list);
            else
                checkPresence(list);
        }
    }


    private static void check(Address addr, List<Long> list) {
        long id=list.get(0);
        for(long val: list) {
            assert val == id : "[" + addr + "]: val=" + val + " (expected " + id + "): list is " + list;
            id++;
        }
    }

    private static void checkPresence(List<Long> list) {
        for(long l=1; l <= 10; l++) {
            assert list.contains(l) : l + " is not in the list " + list;
        }
    }




    private static class MyReceiver extends ReceiverAdapter {
        final String name;
        private final Map<Address, List<Long>> msgs=new ConcurrentHashMap<Address,List<Long>>();

        public MyReceiver(String name) {
            this.name=name;
        }

        public String getName() {
            return name;
        }

        public Map<Address, List<Long>> getMsgs() {
            return msgs;
        }

        public void receive(Message msg) {
            Address addr=msg.getSrc();
            Long val=(Long)msg.getObject();

            synchronized(msgs) {
                List<Long> list=msgs.get(addr);
                if(list == null) {
                    list=new CopyOnWriteArrayList<Long>();
                    msgs.put(addr, list);
                }
                list.add(val);
            }
        }

        public void clear() {
            synchronized(msgs) {
                msgs.clear();
            }
        }


        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append("receiver " + name).append(":\n");
            for(Map.Entry<Address,List<Long>> entry: msgs.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            return sb.toString();
        }

    }

}