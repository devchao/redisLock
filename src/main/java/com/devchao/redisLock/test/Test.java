package com.devchao.redisLock.test;

import java.util.Random;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.devchao.redisLock.RedisLock;

public class Test implements Runnable {
	
	private int n;
	private static RedisLock redisLock;
	private final static ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
	
	static {
		StringRedisTemplate redisTemplate = (StringRedisTemplate) context.getBean("redisTemplate");
		redisLock = new RedisLock();
		redisLock.setRedisTemplate(redisTemplate);
	}
	
	public Test(int n) {
		this.n = n;
	}
	
	public static void main(String[] args) throws InterruptedException {
		for(int i = 0; i < 10000; i++) {
			new Thread(new Test(i)).start();
			Thread.sleep(new Random().nextInt(10));
		}
	}
	 

	@Override
	public void run() {
		long t1 = System.currentTimeMillis();
		System.out.println("thread" + n + " wait lock");
		boolean b = false;
		try {
			b = redisLock.trylock("foo.lock", 1000L);
			System.out.println("thread" + n + " get lock and begin");
			for(int i = 0; i < 10; i++) {
				System.out.println("thread" + n + " write " + i);
			}
			Thread.sleep(new Random().nextInt(10));
			System.out.println("thread" + n + " end, cost " + (System.currentTimeMillis() - t1) + "ms");
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(b) {
				redisLock.unlock("foo.lock");
			}
		}
	}
	
	
	
	
}
