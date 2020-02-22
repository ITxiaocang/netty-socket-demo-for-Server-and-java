import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import java.util.ArrayList;

public class Server {
    public static String userId=null;
    public static void main(String [] args){
        //设置地址和端口
        Configuration config = new Configuration();
        config.setHostname("192.168.3.73");
        config.setPort(9000);
        ArrayList userId=new ArrayList();
        ArrayList staffId=new ArrayList();
        System.out.println("ip:"+config.getHostname()+"  port:"+config.getPort());
        //获取当前时间
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        SocketIOServer server = new SocketIOServer(config);

        //服务监听，监听是否有用户接入
        server.addConnectListener(new ConnectListener() {
            // 添加客户端连接监听器
            public void onConnect(SocketIOClient client) {
                String sa = client.getRemoteAddress().toString();
                String clientIp = sa.substring(1,sa.indexOf(":"));//获取客户端连接的ip
                String clientPort = sa.substring(sa.indexOf(":"));//获取客户端连接的port
                System.out.println("First服务端接收客户端的连接信息为:"+sa+"----ip:"+clientIp+"   port:"+clientPort);
                //不知道如何与客户端对应，好的办法是自己去写对应的函数   如果客户端Client连接成功，发送给Client一个字符串"hello"
                client.sendEvent("connected", "hello 我是Server，您已经成功连接");
            }
        });

        server.start();

        //默认message事件   send发来的信息 都由此监听处理   在此处理用户发来的个人用户信息
        server.addEventListener("message", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient socketIOClient, String s, AckRequest ackRequest) throws Exception {
                //获取类型，如果getType是1，那么他就是用户；如果是0，就是客服
                String getType=s.substring(0,s.indexOf(":"));
                //开始获取发来的Id  Id1是获取当前“:”前面的字符串，主要用来知道这个长度
                String Id1=s.substring(0,s.indexOf(":"));
                //获取到Id1的长度后，根据substring函数，来截取获取到的Id
                String Id =s.substring(Id1.length()+1,s.length());
                System.out.println("获取到type："+getType);
                System.out.println("获取到id："+Id);
                //1是用户   查看Android端代码
                //0是客服   查看Java端代码
                if(Integer.parseInt(getType)==1){
                    System.out.println("用户发来的个人信息"+s);
                    if(staffId.size()>0){
                        //获取堆内客服的Id
                        String getStaffId=staffId.get(0).toString();
                        //发送至客服
                        System.out.println("即将发送给客服  用户的Id为:"+Id);
                        //发送给客服，让客服知道它的用户是谁
                        server.getBroadcastOperations().sendEvent("getUserId"+getStaffId,Id);
                        //发送给用户，让用户知道他的客服是谁          这里出了问题
//                        建议修改为
                    server.getBroadcastOperations().sendEvent("getStaffId"+Id,getStaffId);
//                        server.getBroadcastOperations().sendEvent("getStaffId",getStaffId);

                        //移除最先进入的客服------FIFO算法
                        staffId.remove(0);
//                        staffId.remove(""+Id+"");
                    }else {
                        System.out.println("用户入堆  Id:"+Id);
                        userId.add(Id);
                    }
                }else if(Integer.parseInt(getType)==0){
                    System.out.println("客服发来的个人信息"+s);
                    if(userId.size()>0){
                        //获取堆内 用户的Id
                        String getUserId=userId.get(0).toString();
                        //发送至用户
                        System.out.println("即将发送给用户  客服的Id为:"+Id);
                        //                                          ?????这里出了问题，因为所有用户都可以收到这个getUserId事件   暂时不对下列内容做出修改
                        server.getBroadcastOperations().sendEvent("getStaffId"+getUserId,Id);
//                        server.getBroadcastOperations().sendEvent("getUserId",Id);
//                        server.getBroadcastOperations().sendEvent("getStaffId",Id);
                        String user1=userId.get(0).toString();
                        //这里出了问题，建议修改为
                        server.getBroadcastOperations().sendEvent("getUserId"+Id,user1);
//                        server.getBroadcastOperations().sendEvent("getUserId",user1);
                        userId.remove(0);
//                        userId.remove(""+Id+"");
                    }else {
                        System.out.println("客服入堆  Id:"+Id);
                        staffId.add(Id);
                    }
                }else if(Integer.parseInt(getType)==2){
                        System.out.println("用户已经成功接入");
                }else {
                    System.out.println("未知身份消息"+s+"拒绝处理");
                }
                System.out.println("**********************************");
            }
        });

        //接收客服发送消息监听  然后发送至用户
        server.addEventListener("submitMessageToUser", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient socketIOClient, String s, AckRequest ackRequest) throws ClassNotFoundException {
                String getAllMessage=s;
                String getStaffId=getAllMessage.substring(0,getAllMessage.indexOf(":"));
                String getMessage1=s.substring(0,s.indexOf(":"));
                String getMessage=s.substring(getMessage1.length()+1,s.length());
                System.out.println("收到客服发来的消息"+s+"    收到客服的Id:"+getStaffId+"    收到要发送给用户的信息:"+getMessage);
                server.getBroadcastOperations().sendEvent("borcast:"+getStaffId,getMessage);
            }
        });

        //接收用户发送消息监听  然后发送给客服
        server.addEventListener("submitMessageToStaff", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient socketIOClient, String s, AckRequest ackRequest) throws ClassNotFoundException {
                String getAllMessage=s;
                String getUserId=s.substring(0,s.indexOf(":"));
                String getMessage1=s.substring(0,s.indexOf(":"));
                String getMessage=getAllMessage.substring(getMessage1.length()+1,s.length());
                System.out.println("收到用户发来的消息"+s+"    收到用户的Id:"+getUserId+"    收到要发送给客服的信息:"+getMessage);
                server.getBroadcastOperations().sendEvent("borcast:"+getUserId,getMessage);
            }
        });

        //添加客户端断开连接事件
        server.addDisconnectListener(new DisconnectListener(){
            public void onDisconnect(SocketIOClient client) {
                String sa = client.getRemoteAddress().toString();
                String clientIp = sa.substring(1,sa.indexOf(":"));//获取设备ip
                System.out.println(clientIp+"-------------------------"+"客户端已断开连接");
                //给客户端发送消息  tip:这里已经断开，发不出去的
                client.sendEvent("disconnect_info",clientIp+"客户端你好，我是服务端，期待下次和你见面");
            }
        });

    }
}
