package server;
import client.Request;
import client.Task;
import client.ClientSettings;
import java.util.Hashtable;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.EOFException;
public class ServerCreator{
    Main main;
    Hashtable<Integer,ObjectOutputStream> taskOutputStreams;
    public ServerCreator(Main main){
        this.main=main;
        this.taskOutputStreams=new Hashtable<Integer,ObjectOutputStream>();
        try{
            new Thread("ClientConnectionThread"){
                public ServerSocket server=new ServerSocket(Settings.SERVER_PORT);
                public void run(){
                    while(true){
                        try{
                            Socket client=server.accept();
                            ObjectOutputStream out=new ObjectOutputStream(client.getOutputStream());
                            synchronized(out){
                                out.flush();
                            }
                            ObjectInputStream in=new ObjectInputStream(client.getInputStream());
                            boolean isRequestClient=in.readBoolean(); //sonst: taskClient
                            if (isRequestClient){
                                new Thread("requestClientThread"){
                                    long timeOfLastAction=System.currentTimeMillis(); //Wenn das zu lange her ist, dann wird der Thread geschlossen
                                    Integer playerID=-1; //wird von den Requests genommen
                                    public void run(){
                                        while(true){
                                            try{
                                                Request req=(Request) in.readObject();
                                                playerID=req.playerID;
                                                if ((playerID.equals(-1) && (req.todo.equals("Main.newPlayer") || req.todo.equals("Main.getPlayer")))
                                                        || (main.getPlayer(playerID)!=null && (req.todo.equals("Main.login") || main.getPlayer(playerID).isOnline()))){
                                                    if (req.retClass!=null){
                                                        Object ret=resolveRequest(req);
                                                        synchronized(out){
                                                            //das reset() ist notwendig, da sonst eine Referenz geschrieben wird => �bertragung falscher (zu alter) Attribute
                                                            out.reset();
                                                            out.writeObject(ret);
                                                            out.flush();
                                                        }
                                                    }
                                                    else{
                                                        resolveRequest(req);
                                                    }
                                                    timeOfLastAction=System.currentTimeMillis();
                                                }
                                                else{ //Jemand versucht, zu betr�gen, hier am Server kann n�mlich ein Player nur isOnline()=true zur�ckgeben, 
                                                      //wenn er sich vorher erfolgreich (mit dem richtigen Passwort) eingeloggt hat
                                                      //Ausnahmen sind login und mit playerID -1 Main.newPlayer und Main.getPlayer-Requests (die finden bereits im Login-Men� statt)
                                                    System.out.println("Sehr Witzig du Betr�ger "+req);
                                                    client.close();
                                                    return;
                                                }
                                            }
                                            catch(Exception e){
                                                if (e instanceof EOFException){}
                                                else if (e instanceof InvocationTargetException){
                                                    System.out.println("InvocationTargetException when resolving request: "+e.getCause());
                                                    e.printStackTrace();
                                                }
                                                else{
                                                    System.out.println("Exception when resolving request: "+e);
                                                }
                                            }
                                            if (System.currentTimeMillis()-timeOfLastAction>Settings.REQUEST_THREAD_TIMEOUT){
                                                if (!playerID.equals(-1)){
                                                    main.getPlayer(playerID).logout();
                                                    main.newTask(playerID,"logoutTask");
                                                }
                                                return;
                                            }
                                        }
                                    }
                                }.start();
                            }
                            else{
                                int playerID=in.readInt();
                                taskOutputStreams.put(playerID,out);
                            }
                        }
                        catch(Exception e){
                            System.out.println("Exception when waiting for clients: "+e);
                        }
                    }
                }
            }.start();
        }
        catch(Exception e){
            System.out.println("Exception when creating ServerSocket: "+e);
        }
    }
    
    public Object resolveRequest(Request req) throws NoSuchMethodException,IllegalAccessException,InvocationTargetException,IllegalArgumentException{
        if (ClientSettings.PRINT_COMMUNICATION){
            System.out.println("Resolving Request "+req);
        }
        String className=req.todo.substring(0,req.todo.indexOf("."));
        String methodName=req.todo.substring(req.todo.indexOf(".")+1);
        Object[] params=new Object[req.params.length+1];
        params[0]=req.playerID;
        for (int i=0;i<req.params.length;i++){
            params[i+1]=req.params[i];
        }
        Class[] parameterTypes=new Class[params.length];
        for (int i=0;i<params.length;i++){
            parameterTypes[i]=params[i].getClass();
        }
        if (className.equals("Main")){
            if (methodName.equals("synchronizePlayerVariable") || methodName.equals("synchronizePlayerSVariable") || methodName.equals("synchronizePlayerCVariable")){
                //Diese Methoden nehmen formal Objects als Parameter, das muss auch so in den ParameterTypes stehen
                parameterTypes[3]=Object.class;
            }
            Method method=Main.class.getMethod(methodName,parameterTypes);
            req.ret=method.invoke(main,params);
        }
        else if (className.equals("Space")){
            Method method=Space.class.getMethod(methodName,parameterTypes);
            req.ret=method.invoke(main.getSpace(),params);
        }
        else if (className.equals("Sandbox")){
            int sandboxIndex=(int) params[1];
            Method method=PlanetC.class.getMethod(methodName,parameterTypes);
            req.ret=method.invoke(main.getSandbox(sandboxIndex),params);
        }
        //hier k�nnen auch noch weitere Klassen folgen
        else{
            throw new IllegalArgumentException("className = "+className+", methodName = "+methodName);
        }
        return req.ret;
    }
    
    public void sendTask(int playerID, Task task){
        if (main.getPlayer(playerID).isOnline()){
            try{
                ObjectOutputStream tos=taskOutputStreams.get(playerID);
                synchronized(tos){
                    tos.reset();
                    tos.writeObject(task);
                    tos.flush();
                }
            }
            catch(Exception e){
                System.out.println("Exception when sending Task: "+e);
            }
        }
    }
}