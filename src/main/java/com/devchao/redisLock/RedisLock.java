package com.devchao.redisLock;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisLock {

	private StringRedisTemplate redisTemplate;
	
	public StringRedisTemplate  getRedisTemplate() {
		return redisTemplate;
	}

	public void setRedisTemplate(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public boolean trylock(final String target, final Long timeout) {
		
		long time = System.currentTimeMillis(), nowTime = 0;
		while(!this.redisSetNX(target, timeout)) {
			nowTime = System.currentTimeMillis();

			//等待超时
			if(nowTime - time > timeout) {
				Long value = this.redisGet(target);
				if(value == null) continue;
				if(nowTime >= value) {//比对时间戳，发现锁超时
					Long lastVal = this.redisGetSet(target, timeout);
					
					//取得锁
					if(lastVal.longValue() == value || lastVal == null) {
						break;
					}
				}
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	public void unlock(final String target) {
		redisTemplate.delete(target);
	}

	private boolean redisSetNX(final String target, final Long timeout) {
		return redisTemplate.execute(new RedisCallback<Boolean>() {
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				long redisTime = connection.time();
                byte[] key  = target.getBytes();    
                byte[] value = (redisTime + timeout + "").getBytes();
                boolean result = connection.setNX(key, value);
                connection.close();
				return result;
			}
		});
	}
	
	private Long redisGet(final String target) {
		return redisTemplate.execute(new RedisCallback<Long>() {
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				Long val = null;
				byte[] bytes = connection.get(target.getBytes());
				if(bytes != null) {
					String value = new String(bytes);
					val = Long.valueOf(value);
				}
				connection.close();
				return val;
			}
		});
	}
	
	private Long redisGetSet(final String target, final Long timeout) {
		return redisTemplate.execute(new RedisCallback<Long>() {
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				Long val = null;
				long redisTime = connection.time();
                byte[] key  = target.getBytes();    
                byte[] value = (redisTime + timeout + "").getBytes();    
				byte[] bytes = connection.getSet(key, value);
				if(bytes != null) {
					val = Long.valueOf(new String(bytes));
				}
				connection.close();
				return val;
			}
		});
	}
}
