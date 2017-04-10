
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
 * HOOK 先在ActivityManagerNative 
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
		//gDefault 为静态方法不用传值， gDefault变量值。
		Object declaredObject = declaredField.get(null);
		Class<?> forName2 = Class.forName("android.util.Singleton");
		//
		Field declaredField2 = forName2.getDeclaredField("mInstance");
		declaredField2.setAccessible(true);
		//系统的iActivityManager对象，，，拿去gDefault的值
		//如果Field是一个实例成员对象, 那么我们传入一个 对象实例, 拿到对象实例 的实例成员 的值
		Object instanceValue = declaredField2.get(declaredObject);
		
		//钩子，是一个接口，只有是接口才能有所回调，才能拦截
		Class<?> iActivityManager = Class.forName("android.app.IActivityManager");
		
		AmsInvocationHandler handler = new AmsInvocationHandler(instanceValue);
		//Singleton 动态代理 （当前线程的类加载器，动态代理的接口，第三个是）
		/*
		 * 每一个动态代理类都必须要实现InvocationHandler这个接口，并且每个代理类的实例都关联到了一个handler，
		 * 当我们通过代理对象调用一个方法的时候，这个方法的调用就会被转发为由InvocationHandler这个接口的 invoke 
		 * 方法来进行调用。我们来看看InvocationHandler这个接口的唯一一个方法 invoke 方法：
		 */
		Object newProxyInstance = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
				new Class<?>[]{iActivityManager} ,
				handler);
		
		declaredField2.set(declaredObject,newProxyInstance);
	}
	
	//方法拦截的地方 
	class AmsInvocationHandler implements InvocationHandler{
		private Object iActivityManager;
		
		public AmsInvocationHandler(Object iActivityManager){
			this.iActivityManager = iActivityManager;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			Log.i("INFO", "methodName"+method.getName());
			//在invoke中发现当前使用的方法是startActivity时候进行拦截
			if("startActivity".contains(method.getName())){
				Intent intent = null;
				int index = 0;
				for(int i=0;i<args.length;i++){
					//
					if(args[i] instanceof Intent){
						//说明找到StartActivity方法的Intent参数
						intent = (Intent) args[i];//愿意图，过不去
						index =i;
						break;
					}
				}
				//Intent proxyIntent = new Intent(context,proxyActivity.getClass());
				Intent proxyIntent = new Intent();
				ComponentName componentName = new ComponentName(context, proxyActivity);
				proxyIntent.setComponent(componentName);
				//這裏把Intent替換成了proxyIntent，而原來的Intent的則在oldIntent裏面
				proxyIntent.putExtra("oldIntent", intent);
				args[index] = proxyIntent;
				return method.invoke(iActivityManager, args);
			}
			
			return method.invoke(iActivityManager, args);
		}
		
	}
	private Object activityThreadValue;//系统程序入口ActivityThread的对象
	public void hookSystemHandler(){
		try{
			//找ActivityThread的单利模式的对象，找系统的对象“sCurrentActivityThread”
			Class<?> forName = Class.forName("android.app.ActivityThread");
			Field currentActivityThreadField = forName.getDeclaredField("sCurrentActivityThread");
			currentActivityThreadField.setAccessible(true);
			activityThreadValue= currentActivityThreadField.get(null);
			Field handlerField = forName.getDeclaredField("mH");
			handlerField.setAccessible(true);
			//mH的变量
			Handler handlerObject = (Handler) handlerField.get(activityThreadValue);
			Field callbackField = Handler.class.getDeclaredField("mCallback");
			callbackField.setAccessible(true);
			//代理
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
			
			//替换之前的Intent
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
				//继续反射
				Field intentField = obj.getClass().getDeclaredField("intent");
				intentField.setAccessible(true);
				//代理意图 ,獲得Intent
				Intent proxyIntent = (Intent) intentField.get(obj);
				//得到真實的Intent
				Intent realIntent = proxyIntent.getParcelableExtra("oldIntent");
				if(realIntent != null){
					//proxyIntent = realIntent;不行
					//proxyIntent.setComponent(realIntent.getComponent());//可以但是獲取參數的時候需要先獲得getParcelableExtra("oldIntent")，然後才能獲取之前的Intent參數
					///proxyIntent = realIntent;不行
					intentField.set(obj, realIntent);
				}
				
				
			}catch(Exception e){
				
			}
		}
		
	}
	
	
	
}
