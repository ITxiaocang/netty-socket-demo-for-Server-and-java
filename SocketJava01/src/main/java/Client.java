import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 客户端
 */
public class Client {
    public static String userId=null;                    //用户手机号
        public static void main(String [] args){
            //获取当前时间
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
            //输入内容
            Scanner in = new Scanner(System.in);
//            String url ="localhost:8000";

            String staffId="a10001";
//            String userId=null;
            String url ="http://192.168.3.73:9000";
            Boolean isconnected=false;
            try{
                IO.Options options = new IO.Options();
                options.transports = new String[]{"websocket"};
                options.reconnectionAttempts = 2;
                options.reconnection=true; //是否重新连接
                options.reconnectionDelay = 1000;//失败重连的时间间隔
                options.timeout = 500;//连接超时时间(ms)
                final Socket socket = IO.socket(url,options);


                socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    public void call(Object... args) {
                        System.out.println("发送 send 消息");
                        //0代表是客服 下面表示告诉服务器，我已经准备好与客户对话了；
                        socket.send("0:"+staffId);
                    }
                });

                //网络错误监听
                socket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        System.out.println("error:" + objects[0].toString());
                    }
                });

                //网络错误监听
                socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        System.out.println("connect error:" + objects[0].toString()+"--------若有错误提示出错，请检查ip、端口是否打开");
                    }
                });


                //自定义接收事件   广播接收事件
                socket.on("borcast", new Emitter.Listener() {
                    public void call(Object... objects) {
                        System.out.println("receive borcast data:" + objects[0].toString());
                    }
                });

                //自定义连接事件    如果连接成功，服务器会发来连接成功提醒
                socket.on("connected", new Emitter.Listener() {
                    public void call(Object... objects) {
                        //连接服务器成功提示，若没有此提示，则检查网络是否通畅
                        System.out.println("event：“connected”  receive data from Server:" + objects[0].toString());
                    }
                });

                //自定义 客户端成功断开连接后，服务器发来断开连接通知事件
                socket.on("disconnect_info", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        System.out.println("event：“disconnect_info”  receive data from Server:" + objects[0].toString());
                    }
                });
                socket.connect();

                //获取用户Id;                   下面的staffId是标识符，为了堆内客服准确接收当前发来的客户信息；
                socket.on("getUserId"+staffId, new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        String newUserId=objects[0].toString();
                        System.out.println("事件：“getUserId"+staffId+"”    开始获取用户Id --------收到的信息:"+newUserId);
                        if(newUserId.equals(userId)){
                            return;
                        }else {
                            userId=objects[0].toString();
                            //获取到用户Id;
                            System.out.println("获取到用户ID:"+userId);
                            //监听收到的事件
                            socket.on("borcast:"+userId,new Emitter.Listener() {
                                @Override
                                public void call(Object... objects) {
                                    System.out.println("event：“borcast:"+userId+" ”  收到客户发来的消息:" + objects[0].toString());
                                }
                            });
                        }
                    }
                });

                do{
                    String s = in.nextLine();
                     socket.emit("submitMessageToUser",staffId+":"+s);
                    System.out.println("Client-----您发送给用户的信息为:"+staffId +":"+ s);
                }while (!isconnected);

            }catch (Exception ex){
                ex.printStackTrace();
            }

        }

        private Emitter.Listener getUserMessage=new Emitter.Listener() {
            @Override
            public void call(Object... objects) {

            }
        };

}