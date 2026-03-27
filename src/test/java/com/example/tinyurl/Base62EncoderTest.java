package com.example.tinyurl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyurl.service.id.Base62Encoder;
import org.junit.jupiter.api.Test;

class Base62EncoderTest {

	@Test
	void encodesZero() {
		assertThat(Base62Encoder.encode(0)).isEqualTo("0");
	}

	@Test
	void encodesSixtyTwo() {
		assertThat(Base62Encoder.encode(62)).isEqualTo("10");
	}

	@Test
	void encodesThreeThousandEightHundredFortyFour() {
		assertThat(Base62Encoder.encode(3844)).isEqualTo("100");
	}

	@Test
	void rejectsNegative() {
		assertThatThrownBy(() -> Base62Encoder.encode(-1)).isInstanceOf(IllegalArgumentException.class);
	}
}
