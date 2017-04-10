package com.example.com_dream_hook;

import com.example.util.HookAmsUtil;

import android.app.Application;

public class MyApplication extends Application{
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		HookAmsUtil amsUtil = new HookAmsUtil(ProxyActivity.class, this);
		try {
			amsUtil.hoolAms();
			amsUtil.hookSystemHandler();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
