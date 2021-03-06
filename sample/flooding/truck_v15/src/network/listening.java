package network;

import java.io.IOException;
import java.net.*;

import truck.comdata;
import truck.Global;

public class listening {
	public void listeningmsg() throws IOException{
		DatagramSocket UDPserverSocket = null;
		boolean listening = true;
		try {
			UDPserverSocket = new DatagramSocket(10130);
		}catch (IOException e) {
			System.out.println("Could not listen on the port" + e);
			e.printStackTrace();
//			System.exit(-1);
		}
		byte[] inData = new byte[1024];
		
		while (listening) {
			DatagramPacket inPacket = new DatagramPacket(inData, inData.length);
			try{
				UDPserverSocket.receive(inPacket);
			}catch(IOException e){
				System.out.println("receive error" + e);
			}
			new UDPMultipleServerThread(inPacket, UDPserverSocket).start();
		}
		UDPserverSocket.close();
	}
}

class UDPMultipleServerThread extends Thread {
	private DatagramPacket inPacket=null;
	private DatagramSocket UDPserverSocket=null;
	public UDPMultipleServerThread(DatagramPacket inPacket, DatagramSocket UDPserverSocket) {
		this.inPacket = inPacket;
		this.UDPserverSocket = UDPserverSocket;
	}
	public void run(){
		String seqnum = new String(inPacket.getData(), 0, 5).trim();
		String srcadd = new String(inPacket.getData(), 5, 32).trim();
		String prehop = new String(inPacket.getData(), 37, 32).trim();
		String xloc = new String(inPacket.getData(),69, 10).trim();
		String vol = new String(inPacket.getData(), 79, 10).trim();
		String acc = new String(inPacket.getData(), 89, 5).trim();
		String tstate = new String(inPacket.getData(), 94, 5).trim();
		String trank = new String(inPacket.getData(), 99, 5).trim();
		String delaytime= new String(inPacket.getData(), 104, 5).trim();
		//check package to updata file
//		InetAddress clientIPAddress = inPacket.getAddress();
//		String clientIP=clientIPAddress.getHostAddress();
		InetAddress selfhost = null;
		String selfipadd=null;
		try {
			selfhost = InetAddress.getLocalHost();
			selfipadd=selfhost.getHostAddress();
		} catch (UnknownHostException e) {
			System.out.println("getselfIP error" + e);
			e.printStackTrace();
		}
		check ck=new check();
		int ckresultline=ck.checkresultseq(seqnum, srcadd);
		int ckresultdistance=ck.checkresultdistance(prehop,selfipadd);
//System.out.println("TAG:listening received");
//System.out.println(srcadd+xloc+seqnum);
		int ckresultstate=0;
		if(tstate.equals("9")){
			ck.checkresultstate(srcadd,selfipadd);
			ckresultstate=-1;
		}

		if((ckresultline>-1)&&(ckresultdistance>-1)&&(ckresultstate>-1)){
//System.out.println("TAG:file update, new not self-to-self");
			//broadcast
			broadcast brocast=new broadcast(seqnum, srcadd, selfipadd);
			comdata xva=new comdata();
			try{
				xva.x=Double.parseDouble(xloc);
				xva.v=Double.parseDouble(vol);
				xva.a=Long.parseLong(acc);
				xva.truckstate=Long.parseLong(tstate);
				xva.truckrank=Long.parseLong(trank);
				xva.delaytime=delaytime;
//				xva.x=Long.valueOf(xloc).longValue();
//				xva.x=Long.valueOf(vol).longValue();
//				xva.x=Long.valueOf(acc).longValue();
			}catch (NullPointerException e){
				System.out.println("parseDouble is null" + e);
				e.printStackTrace();
			}
			//delaydetect
				int selfline=ck.checkrank(selfipadd);
				if(selfline<0||Global.configurefile[selfline][6]==null){
				
				}
				else if(Global.configurefile[selfline][6].equals("1")){
					if(delaytime.equals("2")||delaytime.equals("3")||delaytime.equals("4")||delaytime.equals("5")){
						long nowtime=System.currentTimeMillis();
						Global.truckdelaytime[Integer.parseInt(delaytime)-1]=nowtime-Global.truckdelaytime[0];
						System.out.println("truck " + delaytime +" delaytime: " + (Global.truckdelaytime[Integer.parseInt(delaytime)-1])/2 +" ms");
					}
				}
				else{
					if(delaytime.equals("1")){
						Global.truckdelayflag=Global.configurefile[selfline][6];
					}
				}
			
			
			try {
				brocast.broadcastmsg(xva);
			} catch (IOException e) {
				System.out.println("broadcast error" + e);
				e.printStackTrace();
			}
			//writefile
			Global.configurefile[ckresultline][0]=srcadd;
			Global.configurefile[ckresultline][1]=xloc;
			Global.configurefile[ckresultline][2]=vol;
			Global.configurefile[ckresultline][3]=acc;
			Global.configurefile[ckresultline][4]=seqnum;
			Global.configurefile[ckresultline][5]=tstate;
			Global.configurefile[ckresultline][6]=trank;
		}		
	}
}
