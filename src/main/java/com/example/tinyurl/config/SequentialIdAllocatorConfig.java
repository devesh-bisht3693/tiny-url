package com.example.tinyurl.config;

import com.example.tinyurl.service.id.IdAllocator;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class SequentialIdAllocatorConfig {

	@Bean
	@Primary
	public IdAllocator sequentialIdAllocator() {
		AtomicLong seq = new AtomicLong(0L);
		return seq::getAndIncrement;
	}
}
