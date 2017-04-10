
package com.example.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

/**
 * HOOK ����ActivityManagerNative 
 * @author dream
 *
 */
public class HookAmsUtil {
	private Class<?> proxyActivity;
	private Context context;
	public HookAmsUtil(Class<?> proxyActivity,Context context){
		this.proxyActivity = proxyActivity;
		this.context = context;
	}
	
	public void hoolAms() throws Exception{
		Log.i("INFO", "start hook");
		Class<?> forName = Class.forName("android.app.ActivityManagerNative");
		Field declaredField = forName.getDeclaredField("gDefault");
		declaredField.setAccessible(true);
		//gDefault Ϊ��̬�������ô�ֵ�� gDefault����ֵ��
		Object declaredObject = declaredField.get(null);
		Class<?> forName2 = Class.forName("android.util.Singleton");
		//
		Field declaredField2 = forName2.getDeclaredField("mInstance");
		declaredField2.setAccessible(true);
		//ϵͳ��iActivityManager���󣬣�����ȥgDefault��ֵ
		//���Field��һ��ʵ����Ա����, ��ô���Ǵ���һ�� ����ʵ��, �õ�����ʵ�� ��ʵ����Ա ��ֵ
		Object instanceValue = declaredField2.get(declaredObject);
		
		//���ӣ���һ���ӿڣ�ֻ���ǽӿڲ��������ص�����������
		Class<?> iActivityManager = Class.forName("android.app.IActivityManager");
		
		AmsInvocationHandler handler = new AmsInvocationHandler(instanceValue);
		//Singleton ��̬���� ����ǰ�̵߳������������̬����Ľӿڣ��������ǣ�
		/*
		 * ÿһ����̬�����඼����Ҫʵ��InvocationHandler����ӿڣ�����ÿ���������ʵ������������һ��handler��
		 * ������ͨ������������һ��������ʱ����������ĵ��þͻᱻת��Ϊ��InvocationHandler����ӿڵ� invoke 
		 * ���������е��á�����������InvocationHandler����ӿڵ�Ψһһ������ invoke ������
		 */
		Object newProxyInstance = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
				new Class<?>[]{iActivityManager} ,
				handler);
		
		declaredField2.set(declaredObject,newProxyInstance);
	}
	
	//�������صĵط� 
	class AmsInvocationHandler implements InvocationHandler{
		private Object iActivityManager;
		
		public AmsInvocationHandler(Object iActivityManager){
			this.iActivityManager = iActivityManager;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			Log.i("INFO", "methodName"+method.getName());
			//��invoke�з��ֵ�ǰʹ�õķ�����startActivityʱ���������
			if("startActivity".contains(method.getName())){
				Intent intent = null;
				int index = 0;
				for(int i=0;i<args.length;i++){
					//
					if(args[i] instanceof Intent){
						//˵���ҵ�StartActivity������Intent����
						intent = (Intent) args[i];//Ը��ͼ������ȥ
						index =i;
						break;
					}
				}
				//Intent proxyIntent = new Intent(context,proxyActivity.getClass());
				Intent proxyIntent = new Intent();
				ComponentName componentName = new ComponentName(context, proxyActivity);
				proxyIntent.setComponent(componentName);
				//�@�Y��Intent��Q����proxyIntent����ԭ���Intent�Ąt��oldIntent�Y��
				proxyIntent.putExtra("oldIntent", intent);
				args[index] = proxyIntent;
				return method.invoke(iActivityManager, args);
			}
			
			return method.invoke(iActivityManager, args);
		}
		
	}
	private Object activityThreadValue;//ϵͳ�������ActivityThread�Ķ���
	public void hookSystemHandler(){
		try{
			//��ActivityThread�ĵ���ģʽ�Ķ�����ϵͳ�Ķ���sCurrentActivityThread��
			Class<?> forName = Class.forName("android.app.ActivityThread");
			Field currentActivityThreadField = forName.getDeclaredField("sCurrentActivityThread");
			currentActivityThreadField.setAccessible(true);
			activityThreadValue= currentActivityThreadField.get(null);
			Field handlerField = forName.getDeclaredField("mH");
			handlerField.setAccessible(true);
			//mH�ı���
			Handler handlerObject = (Handler) handlerField.get(activityThreadValue);
			Field callbackField = Handler.class.getDeclaredField("mCallback");
			callbackField.setAccessible(true);
			//����
			callbackField.set(handlerObject, new ActivityThreadHandlerCallback(handlerObject));
			
		}catch(Exception e){
			
		}
	}
	
	//ActivityClientRecord r
	class ActivityThreadHandlerCallback implements Handler.Callback{
		
		Handler handler;
		public ActivityThreadHandlerCallback(Handler handler) {
			super();
			this.handler = handler;
		}


		@Override
		public boolean handleMessage(Message msg) {
			Log.i("INFO", "message callback");
			
			//�滻֮ǰ��Intent
			if(msg.what == 100){
				Log.i("INFO", "launchActivity");
				handlerLunchActivity(msg);
			}
			handler.handleMessage(msg);
			return true;
		}


		private void handlerLunchActivity(Message msg) {
			Object obj = msg.obj;//ActivityClientRecord
			try{
				//��������
				Field intentField = obj.getClass().getDeclaredField("intent");
				intentField.setAccessible(true);
				//������ͼ ,�@��Intent
				Intent proxyIntent = (Intent) intentField.get(obj);
				//�õ��挍��Intent
				Intent realIntent = proxyIntent.getParcelableExtra("oldIntent");
				if(realIntent != null){
					//proxyIntent = realIntent;����
					//proxyIntent.setComponent(realIntent.getComponent());//���Ե��ǫ@ȡ�����ĕr����Ҫ�ȫ@��getParcelableExtra("oldIntent")��Ȼ����ܫ@ȡ֮ǰ��Intent����
					///proxyIntent = realIntent;����
					intentField.set(obj, realIntent);
				}
				
				
			}catch(Exception e){
				
			}
		}
		
	}
	
	
	
}
