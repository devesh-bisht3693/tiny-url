package com.example.tinyurl.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class ZookeeperConfiguration {

	@Bean(destroyMethod = "close")
	public CuratorFramework curatorFramework(AppProperties appProperties) {
		AppProperties.Zookeeper zk = appProperties.zookeeper();
		CuratorFramework client = CuratorFrameworkFactory.builder()
				.connectString(zk.connectString())
				.sessionTimeoutMs(zk.sessionTimeoutMs())
				.connectionTimeoutMs(zk.connectionTimeoutMs())
				.retryPolicy(new ExponentialBackoffRetry(1000, 3))
				.build();
		client.start();
		return client;
	}
}
