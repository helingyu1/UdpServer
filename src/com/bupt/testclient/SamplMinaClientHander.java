package com.bupt.testclient;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;  
import org.apache.mina.core.session.IdleStatus;  
import org.apache.mina.core.session.IoSession;  
  
/** 
 * 消息处理类 
 * @author 何明 
 * 
 */  
public class SamplMinaClientHander extends IoHandlerAdapter {  
  
    @Override  
    public void exceptionCaught(IoSession arg0, Throwable arg1)  
            throws Exception {  
        // TODO Auto-generated method stub  
  
    }  
  
    /** 
     * 当客户端接受到消息时 
     */  
    @Override  
    public void messageReceived(IoSession session, Object message) throws Exception {  
  
        //我们已设定了服务器的消息规则是一行一行读取，这里就可以转为String:  
//        String s = (String)message;  
    	IoBuffer buffer = (IoBuffer)message;
    	System.out.println(buffer.toString());
          
        //Writer the received data back to remote peer  
//        System.out.println("服务器发来的收到消息: " + s);  
          
        //测试将消息回送给客户端  
//        session.write(s);  
  
    }  
  
    @Override  
    public void messageSent(IoSession arg0, Object arg1) throws Exception {  
        // TODO Auto-generated method stub  
  
    }  
  
    /** 
     * 当一个客户端被关闭时 
     */  
    @Override  
    public void sessionClosed(IoSession session) throws Exception {  
        System.out.println("one client Disconnect");  
  
    }  
  
    @Override  
    public void sessionCreated(IoSession arg0) throws Exception {  
        // TODO Auto-generated method stub  
    	System.out.println(1111);
  
    }  
  
    @Override  
    public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {  
        // TODO Auto-generated method stub  
  
    }  
  
    /** 
     * 当一个客户端连接进入时 
     */  
    @Override  
    public void sessionOpened(IoSession session) throws Exception {  
  
        System.out.println("incomming client:" + session.getRemoteAddress()); 
        byte[] aa = {0x64,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};

        byte[] bb = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1A, (byte)0xFE, 0x34, (byte)0xF7, 0x6A, 0x14};
        byte[] cc = {0x02,0x01,(byte)0xC8,(byte)0xB4,0x00,0x00,0x00,0x00,0x00, 0x00, 0x00, 0x00, 0x00, 0x00 ,0x1A ,(byte)0xFE ,0x34 ,(byte)0xF7 ,0x6A ,0x14 ,(byte)0xAE ,(byte)0xE5 ,0x38 ,(byte)0xE3 ,(byte)0xBC ,0x7F ,0x3A ,0x2D ,(byte)0x85 ,0x18 ,0x03 ,0x36 ,(byte)0xD2 ,(byte)0xCB ,(byte)0xF6 ,(byte)0xE1};
        IoBuffer buffer = IoBuffer.wrap(cc);
        session.write(buffer);  
  
    }  
  
}  